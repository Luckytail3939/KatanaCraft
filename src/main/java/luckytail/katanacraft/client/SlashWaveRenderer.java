package luckytail.katanacraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import luckytail.katanacraft.Katanacraft;
import luckytail.katanacraft.entity.SlashWaveEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** 光波斬撃: 進行方向を向いた三日月形の発光板を描く */
public class SlashWaveRenderer extends EntityRenderer<SlashWaveEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Katanacraft.MODID, "textures/entity/slash_wave.png");

    public SlashWaveRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(SlashWaveEntity entity, float entityYaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight) {
        float age = (entity.tickCount + partialTick) / SlashWaveEntity.LIFETIME;
        float alpha = Mth.clamp(1F - age * age, 0F, 1F);
        float grow = 1F + age * 0.6F;

        float sc = entity.getWaveScale();

        pose.pushPose();
        pose.translate(0, 0.6 * sc, 0);
        pose.mulPose(Axis.YP.rotationDegrees(-Mth.lerp(partialTick, entity.yRotO, entity.getYRot())));
        pose.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTick, entity.xRotO, entity.getXRot())));
        pose.scale(grow * sc, 1F, grow * sc);

        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));
        PoseStack.Pose last = pose.last();
        Matrix4f m = last.pose();
        Matrix3f n = last.normal();
        int a = (int) (alpha * 255);

        // 板（横2.2 × 奥行1.2）。テクスチャの上側(v=0)が進行方向
        float w = 2.2F, d = 1.2F;
        // 表
        vertex(vc, m, n, -w, d, 0, 0, a);
        vertex(vc, m, n, w, d, 1, 0, a);
        vertex(vc, m, n, w, -d, 1, 1, a);
        vertex(vc, m, n, -w, -d, 0, 1, a);
        // 裏
        vertex(vc, m, n, -w, -d, 0, 1, a);
        vertex(vc, m, n, w, -d, 1, 1, a);
        vertex(vc, m, n, w, d, 1, 0, a);
        vertex(vc, m, n, -w, d, 0, 0, a);

        pose.popPose();
        super.render(entity, entityYaw, partialTick, pose, buffers, packedLight);
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float z, float u, float v, int a) {
        vc.vertex(m, x, 0, z)
                .color(255, 255, 255, a)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(n, 0, 1, 0)
                .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(SlashWaveEntity entity) {
        return TEXTURE;
    }
}
