package oas.dreyka.deepcrate.net;

import oas.dreyka.deepcrate.DeepCrate;
import oas.dreyka.deepcrate.config.ConfigRuntime;
import oas.dreyka.deepcrate.init.RegistryInit;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

/**
 * One setting, named and given a new value by the screen, for the server to accept or ignore.
 *
 * The value travels as the text that was typed, not as a number. The receiver parses it against the
 * option's own kind anyway, so a malformed entry costs a refusal, not a decoding failure that drops
 * the connection.
 *
 * The permission is checked here too, in addition to the command that opens the screen. A client can
 * send this packet without ever running the command, so a gate placed on the command alone reads as
 * correct and hands every player the settings file.
 */
public record ConfigSetPayload(String name, String value) implements CustomPacketPayload {
    /** Both fields are an identifier or a short number, so the reader refuses anything longer. */
    private static final int MAX_TEXT = 256;

    public static final Type<ConfigSetPayload> TYPE = new Type<>(RegistryInit.id("config_set"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigSetPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.stringUtf8(MAX_TEXT),
        ConfigSetPayload::name,
        ByteBufCodecs.stringUtf8(MAX_TEXT),
        ConfigSetPayload::value,
        ConfigSetPayload::new
    );

    public static void register() {
        PayloadTypeRegistry.playC2S().register(TYPE, STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(TYPE, (payload, context) -> {
            ServerPlayer serverPlayer = context.player();
            // Checked on the network thread, before anything is scheduled, so a forged packet costs
            // nothing at all. It gets no answer either: replying would confirm the option exists.
            if (!serverPlayer.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
                return;
            }

            context.server().execute(() -> apply(serverPlayer, payload));
        });
    }

    private static void apply(ServerPlayer serverPlayer, ConfigSetPayload payload) {
        if (!ConfigRuntime.set(payload.name(), payload.value())) {
            return;
        }

        DeepCrate.LOGGER.info("[DeepCrate] {} set {} to {}", serverPlayer.getName().getString(), payload.name(), payload.value());
        // What was stored may have been pulled into range, so the screen gets the table as it now
        // stands, not left showing the number that was typed.
        ConfigSyncPayload.sendTo(serverPlayer);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
