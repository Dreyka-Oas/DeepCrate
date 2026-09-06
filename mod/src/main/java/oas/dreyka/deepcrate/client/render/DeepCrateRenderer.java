package oas.dreyka.deepcrate.client.render;

import oas.dreyka.deepcrate.api.CrateTier;
import oas.dreyka.deepcrate.api.DeepCrateApi;
import oas.dreyka.deepcrate.block.DeepCrateBlock;
import oas.dreyka.deepcrate.block.DeepCrateBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.BlockPos;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws a crate with the chest model of the base game, lid animation included.
 *
 * Vanilla's own ChestRenderer cannot be subclassed into this: it picks its texture from a closed enum
 * of chest kinds, which no mod can extend. The models, the atlas and the opening curve are reused as
 * they are; only the material changes.
 */
public class DeepCrateRenderer implements BlockEntityRenderer<DeepCrateBlockEntity, DeepCrateRenderState> {
    private final MaterialSet materials;
    private final ChestModel singleModel;
    private final ChestModel leftModel;
    private final ChestModel rightModel;

    public DeepCrateRenderer(BlockEntityRendererProvider.Context context) {
        this.materials = context.materials();
        this.singleModel = new ChestModel(context.bakeLayer(ModelLayers.CHEST));
        this.leftModel = new ChestModel(context.bakeLayer(ModelLayers.DOUBLE_CHEST_LEFT));
        this.rightModel = new ChestModel(context.bakeLayer(ModelLayers.DOUBLE_CHEST_RIGHT));
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
        deepCrateRenderState.type = deepCrateBlockEntity.getBlockState().getValue(DeepCrateBlock.TYPE);
        deepCrateRenderState.open = openness(deepCrateBlockEntity, f);

        CrateTier crateTier = DeepCrateApi.tierOf(deepCrateBlockEntity.getBlockState().getBlock());
        deepCrateRenderState.material = crateTier == null
            ? CrateMaterials.MISSING
            : CrateMaterials.of(crateTier, deepCrateRenderState.type);

        // Both halves take the brighter of the two lights, as a double chest does; lit separately the
        // model shows a seam down the middle.
        if (deepCrateRenderState.type != ChestType.SINGLE && deepCrateBlockEntity.getLevel() != null) {
            BlockPos blockPos = DeepCrateBlock.connectedPos(deepCrateBlockEntity.getBlockState(), deepCrateBlockEntity.getBlockPos());
            int other = LevelRenderer.getLightColor(deepCrateBlockEntity.getLevel(), blockPos);
            deepCrateRenderState.lightCoords = LightTexture.pack(
                Math.max(LightTexture.block(deepCrateRenderState.lightCoords), LightTexture.block(other)),
                Math.max(LightTexture.sky(deepCrateRenderState.lightCoords), LightTexture.sky(other))
            );
        }
    }

    /**
     * The wider of the two lids, as the chest does. A re-sent chunk rebuilds one half with a lid
     * controller at zero, and that half would animate on its own.
     */
    private static float openness(DeepCrateBlockEntity deepCrateBlockEntity, float f) {
        float own = deepCrateBlockEntity.getOpenNess(f);
        if (deepCrateBlockEntity.getBlockState().getValue(DeepCrateBlock.TYPE) == ChestType.SINGLE || deepCrateBlockEntity.getLevel() == null) {
            return own;
        }

        BlockPos blockPos = DeepCrateBlock.connectedPos(deepCrateBlockEntity.getBlockState(), deepCrateBlockEntity.getBlockPos());
        return deepCrateBlockEntity.getLevel().getBlockEntity(blockPos) instanceof DeepCrateBlockEntity other
            ? Math.max(own, other.getOpenNess(f))
            : own;
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

        ChestModel chestModel = switch (deepCrateRenderState.type) {
            case SINGLE -> this.singleModel;
            case LEFT -> this.leftModel;
            case RIGHT -> this.rightModel;
        };

        submitNodeCollector.submitModel(
            chestModel,
            f,
            poseStack,
            deepCrateRenderState.material.renderType(RenderTypes::entityCutout),
            deepCrateRenderState.lightCoords,
            OverlayTexture.NO_OVERLAY,
            -1,
            this.materials.get(deepCrateRenderState.material),
            0,
            deepCrateRenderState.breakProgress
        );

        poseStack.popPose();
    }
}
