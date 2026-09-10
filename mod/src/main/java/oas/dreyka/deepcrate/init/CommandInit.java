package oas.dreyka.deepcrate.init;

import oas.dreyka.deepcrate.config.access.ConfigOption;
import oas.dreyka.deepcrate.config.bounds.ConfigRange;
import oas.dreyka.deepcrate.config.access.ConfigRuntime;
import oas.dreyka.deepcrate.net.ConfigSyncPayload;
import java.util.List;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

/**
 * The one command of the mod, {@code /deepcrateconfig}, and the first one in this tree.
 *
 * There is no {@code set} subcommand on purpose. Editing happens in the screen, and a second way in
 * would be a second copy of the clamp and the rewrite, which drifts from the first the day one of
 * them changes. A sender with a player behind it gets the screen; the server console, which has no
 * screen to open, gets the same options printed as text.
 */
public final class CommandInit {
    private CommandInit() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(
                Commands.literal("deepcrateconfig")
                    .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                    .executes(CommandInit::run)
            )
        );
    }

    private static int run(CommandContext<CommandSourceStack> context) {
        ServerPlayer serverPlayer = context.getSource().getPlayer();
        if (serverPlayer != null) {
            ConfigSyncPayload.sendTo(serverPlayer);
            return Command.SINGLE_SUCCESS;
        }

        List<ConfigOption> options = ConfigRuntime.snapshot();
        context.getSource().sendSuccess(
            () -> Component.translatable("deepcrate.notice.config_console", options.size()),
            false
        );
        for (ConfigOption configOption : options) {
            context.getSource().sendSuccess(() -> line(configOption), false);
        }

        return Command.SINGLE_SUCCESS;
    }

    /**
     * Built out of pieces rather than out of a sentence: the label and the range carry the only
     * words, and the separators are single glyphs so nothing here needs translating.
     */
    private static Component line(ConfigOption configOption) {
        MutableComponent line = Component.empty()
            .append(Component.translatable(configOption.categoryKey()))
            .append(Component.literal("/"))
            .append(Component.translatable(configOption.labelKey()))
            .append(Component.literal("="))
            .append(Component.literal(configOption.value()));

        ConfigRange configRange = configOption.range();
        if (configRange == null) {
            return line;
        }

        return line
            .append(Component.literal(" "))
            .append(Component.translatable(
                "screen.deepcrate.config.range",
                number(configRange.min()),
                number(configRange.max())
            ));
    }

    /** A bound is held as a double, and a range of 1 to 32767 reads badly as "1.0 to 32767.0". */
    private static String number(double bound) {
        return bound == Math.rint(bound) && !Double.isInfinite(bound)
            ? Long.toString((long) bound)
            : Double.toString(bound);
    }
}
