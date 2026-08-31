package com.dreykaoas.deepcrate.client;

import com.dreykaoas.deepcrate.DeepCrate;
import com.dreykaoas.deepcrate.block.DeepCrateBlock;
import com.dreykaoas.deepcrate.block.DeepCrateBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the crate with the chest model of the base game, lid animation included, over an iron
 * coloured sheet.
 *
 * Vanilla's own ChestRenderer cannot be subclassed into this: it picks its texture from a closed
 * enum of chest kinds, which no mod can extend. The model, the atlas and the opening curve are all
 * reused as is, only the material differs.
 */
public class DeepCrateRenderer implements BlockEntityRenderer<DeepCrateBlockEntity, DeepCrateRenderState> {
    private static final Material MATERIAL = Sheets.CHEST_MAPPER.apply(Identifier.fromNamespaceAndPath(DeepCrate.MOD_ID, "deep_crate"));

    private final MaterialSet materials;
    private final ChestModel model;

    public DeepCrateRenderer(BlockEntityRendererProvider.Context context) {
        this.materials = context.materials();
        this.model = new ChestModel(context.bakeLayer(ModelLayers.CHEST));
    }

    @Override
    public DeepCrateRenderState createRenderState() {
        return new DeepCrateRenderState();
    }

    @Override
    public void extractRenderState(
        DeepCrateBlockEntity deepCrateBlockEntity,
        DeepCrateRenderState deepCrateRenderState,
        float f,
        Vec3 vec3,
        ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay
    ) {
        BlockEntityRenderer.super.extractRenderState(deepCrateBlockEntity, deepCrateRenderState, f, vec3, crumblingOverlay);
        deepCrateRenderState.angle = deepCrateBlockEntity.getBlockState().getValue(DeepCrateBlock.FACING).toYRot();
        deepCrateRenderState.open = deepCrateBlockEntity.getOpenNess(f);
    }

    @Override
    public void submit(
        DeepCrateRenderState deepCrateRenderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState
    ) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-deepCrateRenderState.angle));
        poseStack.translate(-0.5F, -0.5F, -0.5F);

        // Same easing as a chest, so a crate next to one opens at the same pace.
        float f = 1.0F - deepCrateRenderState.open;
        f = 1.0F - f * f * f;

        submitNodeCollector.submitModel(
            this.model,
            f,
            poseStack,
            MATERIAL.renderType(RenderTypes::entityCutout),
            deepCrateRenderState.lightCoords,
            OverlayTexture.NO_OVERLAY,
            -1,
            this.materials.get(MATERIAL),
            0,
            deepCrateRenderState.breakProgress
        );

        poseStack.popPose();
    }
}
