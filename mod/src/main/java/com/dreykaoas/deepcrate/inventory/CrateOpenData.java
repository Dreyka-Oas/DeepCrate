package com.dreykaoas.deepcrate.inventory;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What the client is told when a crate opens. None of it can be derived client side: the slot count
 * depends on the tier and on whether the crate is half of a pair, the paging comes from an event
 * other mods can answer, and the capacity comes from the module sitting in the crate.
 */
public record CrateOpenData(int slotCount, int rowsPerPage, int pageCount, int capacity) {
    public static final StreamCodec<RegistryFriendlyByteBuf, CrateOpenData> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        CrateOpenData::slotCount,
        ByteBufCodecs.VAR_INT,
        CrateOpenData::rowsPerPage,
        ByteBufCodecs.VAR_INT,
        CrateOpenData::pageCount,
        ByteBufCodecs.VAR_INT,
        CrateOpenData::capacity,
        CrateOpenData::new
    );
}
