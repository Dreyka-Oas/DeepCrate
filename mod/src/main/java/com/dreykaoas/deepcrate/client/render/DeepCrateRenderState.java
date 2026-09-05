package com.dreykaoas.deepcrate.client.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.resources.model.Material;
import net.minecraft.world.level.block.state.properties.ChestType;

/** What the renderer needs from a crate for one frame. */
public class DeepCrateRenderState extends BlockEntityRenderState {
    public float open;
    public float angle;
    public ChestType type = ChestType.SINGLE;
    public Material material = CrateMaterials.MISSING;
}
