package com.dreykaoas.deepcrate.inventory;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What the client is told when a crate opens. None of it can be derived client side: the slot count
 * depends on the tier and on whether the crate is half of a pair, the paging comes from an event other
 * mods can answer, the capacity comes from the modules sitting in the crate, and the width is the
 * tier's own.
 */
public record CrateOpenData(int slotCount, int rowsPerPage, int pageCount, int capacity, int columns) {
    public static final StreamCodec<RegistryFriendlyByteBuf, CrateOpenData> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        CrateOpenData::slotCount,
        ByteBufCodecs.VAR_INT,
        CrateOpenData::rowsPerPage,
        ByteBufCodecs.VAR_INT,
        CrateOpenData::pageCount,
        ByteBufCodecs.VAR_INT,
        CrateOpenData::capacity,
        ByteBufCodecs.VAR_INT,
        CrateOpenData::columns,
        CrateOpenData::new
    );
}
