package com.teddy.stormwand.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.teddy.stormwand.StormWandMod;
import com.teddy.stormwand.entity.StormBoltProjectile;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class StormBoltRenderer extends EntityRenderer<StormBoltProjectile> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(StormWandMod.MOD_ID, "textures/entity/projectiles/storm_bolt.png");
    private static final RenderType RENDER_TYPE = RenderType.entityCutoutNoCull(TEXTURE);
    private static final double CAMERA_HIDE_DISTANCE_SQR = 12.25D;

    public StormBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected int getBlockLightLevel(StormBoltProjectile entity, BlockPos pos) {
        return 15;
    }

    @Override
    public void render(StormBoltProjectile entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (entity.tickCount < 2 && this.entityRenderDispatcher.camera != null && this.entityRenderDispatcher.camera.getEntity() != null
                && entity.distanceToSqr(this.entityRenderDispatcher.camera.getEntity()) < CAMERA_HIDE_DISTANCE_SQR) {
            return;
        }

        poseStack.pushPose();
        poseStack.scale(0.65F, 0.65F, 0.65F);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        PoseStack.Pose pose = poseStack.last();
        Matrix4f poseMatrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();
        VertexConsumer vertexConsumer = buffer.getBuffer(RENDER_TYPE);

        vertex(vertexConsumer, poseMatrix, normalMatrix, packedLight, 0.0F, 0, 0, 1);
        vertex(vertexConsumer, poseMatrix, normalMatrix, packedLight, 1.0F, 0, 1, 1);
        vertex(vertexConsumer, poseMatrix, normalMatrix, packedLight, 1.0F, 1, 1, 0);
        vertex(vertexConsumer, poseMatrix, normalMatrix, packedLight, 0.0F, 1, 0, 0);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(StormBoltProjectile entity) {
        return TEXTURE;
    }

    private static void vertex(VertexConsumer vertexConsumer, Matrix4f poseMatrix, Matrix3f normalMatrix, int packedLight, float x, int y, int u, int v) {
        vertexConsumer.vertex(poseMatrix, x - 0.5F, y - 0.25F, 0.0F)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(normalMatrix, 0.0F, 1.0F, 0.0F)
                .endVertex();
    }
}