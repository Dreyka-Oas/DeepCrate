package oas.dreyka.deepcrate.gametest.config;

import oas.dreyka.deepcrate.config.ConfigBounds;
import oas.dreyka.deepcrate.config.ConfigOption;
import oas.dreyka.deepcrate.config.ConfigRange;
import oas.dreyka.deepcrate.config.ConfigRuntime;
import oas.dreyka.deepcrate.config.schema.ConfigSchema;
import oas.dreyka.deepcrate.net.ConfigSetPayload;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.server.players.PlayerList;

/**
 * The gate on the receiving side, which is the only one a client cannot walk around.
 *
 * Gating {@code /deepcrateconfig} alone would read as correct and hand the settings file to anybody,
 * since a client sends {@link ConfigSetPayload} without ever running the command. The check lives in
 * the lambda {@code ConfigSetPayload.register()} gives to Fabric, so the packet is built here as a
 * forged one is and dropped into the player's own listener, the one seam reaching that lambda without
 * opening production code up. Options are global static state, so every method puts back what it
 * moved, failing path included.
 */
public class ConfigGateGameTest {
    /**
     * One option per method, and never the same one twice.
     *
     * The methods of a batch run at the same time in one world while these options are static fields,
     * so two methods sharing an option read each other's writes. That is what a first run of this
     * class showed: the forged packet saw the clamped value the permitted one had just written, and
     * the permitted one saw the restore of the forged one, each failing with the other's number.
     */
    private static final String REFUSED = "baseCapacity";
    private static final String CLAMPED = "maxRowsPerPage";

    @GameTest
    public void aForgedPacketFromAnOrdinaryPlayerChangesNothing(GameTestHelper gameTestHelper) {
        String before = valueOf(gameTestHelper, REFUSED);
        PlayerList playerList = gameTestHelper.getLevel().getServer().getPlayerList();
        boolean commandsForAll = playerList.isAllowCommandsForAllPlayers();
        // Cheats hand every player a command level, and a refusal would then prove nothing.
        playerList.setAllowCommandsForAllPlayers(false);
        ServerPlayer serverPlayer = gameTestHelper.makeMockServerPlayerInLevel();
        if (serverPlayer.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            playerList.setAllowCommandsForAllPlayers(commandsForAll);
            gameTestHelper.fail("the mock player is a gamemaster, so nothing here tests the gate");
        }

        send(serverPlayer, REFUSED, "1");
        gameTestHelper.runAfterDelay(1, () -> {
            try {
                assertValue(gameTestHelper, REFUSED, before, "after a forged packet");
            } finally {
                playerList.setAllowCommandsForAllPlayers(commandsForAll);
                ConfigRuntime.set(REFUSED, before);
            }
            gameTestHelper.succeed();
        });
    }

    @GameTest
    public void agamemasterChangesTheOptionAndGetsTheClampedValue(GameTestHelper gameTestHelper) {
        String before = valueOf(gameTestHelper, CLAMPED);
        ConfigRange configRange = ConfigBounds.rangeOf(CLAMPED);
        if (configRange == null) {
            gameTestHelper.fail(CLAMPED + " lost its range, so nothing here says what should land");
            return;
        }

        PlayerList playerList = gameTestHelper.getLevel().getServer().getPlayerList();
        // Pinned here too, and not only in the refusal above: the flag is one field on the server and
        // the sibling method moves it, so a method reading it rather than setting it reads whichever
        // way the other left it.
        boolean commandsForAll = playerList.isAllowCommandsForAllPlayers();
        playerList.setAllowCommandsForAllPlayers(false);
        ServerPlayer serverPlayer = gameTestHelper.makeMockServerPlayerInLevel();
        playerList.op(serverPlayer.nameAndId(), Optional.of(LevelBasedPermissionSet.GAMEMASTER), Optional.empty());

        // Far above the upper end, so what lands separates a clamp that ran from a plain assignment.
        send(serverPlayer, CLAMPED, "999999");
        gameTestHelper.runAfterDelay(1, () -> {
            try {
                assertValue(gameTestHelper, CLAMPED, Long.toString((long) configRange.max()), "after a permitted packet");
            } finally {
                playerList.deop(serverPlayer.nameAndId());
                playerList.setAllowCommandsForAllPlayers(commandsForAll);
                ConfigRuntime.set(CLAMPED, before);
            }
            gameTestHelper.succeed();
        });
    }

    /** Straight into the player's own listener, which is where a packet off the wire arrives. */
    private static void send(ServerPlayer serverPlayer, String name, String value) {
        serverPlayer.connection.handleCustomPayload(new ServerboundCustomPayloadPacket(new ConfigSetPayload(name, value)));
    }

    private static void assertValue(GameTestHelper gameTestHelper, String name, String expected, String what) {
        String actual = valueOf(gameTestHelper, name);
        if (!expected.equals(actual)) {
            gameTestHelper.fail(name + " " + what + ": expected " + expected + ", got " + actual);
        }
    }

    private static String valueOf(GameTestHelper gameTestHelper, String name) {
        for (ConfigOption configOption : ConfigRuntime.snapshot()) {
            if (configOption.name().equals(name)) {
                return configOption.value();
            }
        }
        gameTestHelper.fail("the schema has no option called " + name);
        return "";
    }
}
