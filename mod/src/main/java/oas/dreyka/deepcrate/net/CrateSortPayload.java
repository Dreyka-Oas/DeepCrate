package oas.dreyka.deepcrate.net;

import oas.dreyka.deepcrate.init.RegistryInit;
import oas.dreyka.deepcrate.inventory.DeepCrateMenu;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.Item;

/**
 * The order the player asked for, worked out on their machine and sent to the crate.
 *
 * The order travels rather than the button that was pressed, because it comes from item names in the
 * language the player reads and a server holds no language files. What the server does with it is
 * a rearrangement of what it already has, so a made-up order costs its sender a messy crate and
 * nothing else.
 */
public record CrateSortPayload(int containerId, List<Item> order) implements CustomPacketPayload {
    /**
     * A crate cannot name more items than it has slots. The largest is a pair of echo crates carrying
     * sixteen row modules: twenty-four rows a side, 432 slots.
     */
    private static final int MAX_ITEMS = 512;

    public static final Type<CrateSortPayload> TYPE = new Type<>(RegistryInit.id("sort"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CrateSortPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        CrateSortPayload::containerId,
        ByteBufCodecs.registry(Registries.ITEM).apply(ByteBufCodecs.list(MAX_ITEMS)),
        CrateSortPayload::order,
        CrateSortPayload::new
    );

    public static void register() {
        PayloadTypeRegistry.playC2S().register(TYPE, STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(TYPE, (payload, context) -> {
            // The container id pins the request to the screen the player has open, so a packet that
            // crossed a reopening cannot land on the crate that took its place.
            if (context.player().containerMenu instanceof DeepCrateMenu deepCrateMenu
                && deepCrateMenu.containerId == payload.containerId()) {
                deepCrateMenu.sort(payload.order());
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
