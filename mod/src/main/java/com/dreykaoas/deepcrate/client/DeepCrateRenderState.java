package com.dreykaoas.deepcrate.client;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

/** What the renderer needs from a crate for one frame: which way it faces, and how far it is open. */
public class DeepCrateRenderState extends BlockEntityRenderState {
    public float open;
    public float angle;
}
