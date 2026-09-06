package oas.dreyka.deepcrate.net;

import oas.dreyka.deepcrate.config.ConfigOption;
import oas.dreyka.deepcrate.config.ConfigRange;
import oas.dreyka.deepcrate.config.ConfigRuntime;
import oas.dreyka.deepcrate.config.schema.ConfigPrimitive;
import oas.dreyka.deepcrate.init.RegistryInit;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * The whole settings table on its way from the server to the screen that edits it.
 *
 * A client joining a server has a settings file of its own, and the values in it are not the ones the
 * crates obey. So the screen is never allowed to read the config classes: it draws what arrives here
 * and nothing else. The ranges travel alongside the values because the screen has to refuse a number
 * before it is sent, and the bounds table is server-side.
 *
 * The same payload is the answer to an accepted change, which is how a clamped value reaches the
 * screen in place of what the player typed.
 */
public record ConfigSyncPayload(List<ConfigOption> options) implements CustomPacketPayload {
    /**
     * The mod ships five options and an addon adds a handful more, so a few hundred leaves plenty of
     * room while still refusing a list built to exhaust the reader.
     */
    private static final int MAX_OPTIONS = 256;

    /** An option name, its category and its value are identifiers or short numbers, never prose. */
    private static final int MAX_TEXT = 256;

    private static final StreamCodec<RegistryFriendlyByteBuf, ConfigRange> RANGE_CODEC = StreamCodec.composite(
        ByteBufCodecs.DOUBLE,
        ConfigRange::min,
        ByteBufCodecs.DOUBLE,
        ConfigRange::max,
        ConfigRange::new
    );

    /**
     * An option nothing bounds carries no range, so the pair is optional. The nullable field of the
     * record and the optional on the wire meet here rather than leaking either shape into the other.
     */
    private static final StreamCodec<RegistryFriendlyByteBuf, ConfigOption> OPTION_CODEC = StreamCodec.composite(
        ByteBufCodecs.stringUtf8(MAX_TEXT),
        ConfigOption::name,
        ByteBufCodecs.stringUtf8(MAX_TEXT),
        ConfigOption::category,
        ByteBufCodecs.stringUtf8(MAX_TEXT).map(ConfigPrimitive::valueOf, ConfigPrimitive::name),
        ConfigOption::kind,
        ByteBufCodecs.stringUtf8(MAX_TEXT),
        ConfigOption::value,
        ByteBufCodecs.optional(RANGE_CODEC).map(optional -> optional.orElse(null), Optional::ofNullable),
        ConfigOption::range,
        ConfigOption::new
    );

    public static final Type<ConfigSyncPayload> TYPE = new Type<>(RegistryInit.id("config_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigSyncPayload> STREAM_CODEC = StreamCodec.composite(
        OPTION_CODEC.apply(ByteBufCodecs.list(MAX_OPTIONS)),
        ConfigSyncPayload::options,
        ConfigSyncPayload::new
    );

    public static void register() {
        PayloadTypeRegistry.playS2C().register(TYPE, STREAM_CODEC);
    }

    /** The snapshot is taken here rather than by the caller, so no stale table can be sent by mistake. */
    public static void sendTo(ServerPlayer serverPlayer) {
        ServerPlayNetworking.send(serverPlayer, new ConfigSyncPayload(ConfigRuntime.snapshot()));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
