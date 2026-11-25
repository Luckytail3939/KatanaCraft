package luckytail.katanacraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import luckytail.katanacraft.item.KatanaItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 帯刀: 刀を手に持っているが抜いていない間、利き手と逆側の腰に鞘ごと差して描く。
 * 柄が前、鞘尻（こじり）が後ろ、刃を上に向けた侍の差し方。
 *
 * 位置や角度を調整したいときは下の定数をいじる（単位: 1 = 1ピクセル = 1/16ブロック）。
 */
public class KatanaHipLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    /** 腰の位置（胴体の中心から）: 横方向（外側へ）、縦方向（下へ）、前後（マイナスで前） */
    private static final float HIP_X = 4.6F;
    private static final float HIP_Y = 11.0F;
    private static final float HIP_Z = -2.2F;
    /** 鞘の傾き: 90で水平、小さくすると鞘尻が下がる */
    private static final float TILT_DEGREES = 68F;
    /** 鞘尻を少し外側へ開く角度 */
    private static final float SPLAY_DEGREES = 8F;
    /** 腰に差したときの大きさ（手に持った時と同じなら 1.0） */
    private static final float SCALE = 0.9F;
    /** モデル上の「鍔の位置」(モデル座標)。ここを腰の位置に合わせる */
    private static final float TSUBA_Y = 10.2F;
    private static final float CENTER_Z = 8.5F;

    public KatanaHipLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof KatanaItem katana)) return;
        if (player.isInvisible() || ClientDrawState.isDrawn(player)) return; // 抜刀中は手に持つ

        Minecraft mc = Minecraft.getInstance();
        BakedModel model = mc.getModelManager().getModel(katana.getHipModelLocation());
        if (model == null || model == mc.getModelManager().getMissingModel()) return;

        // 右利きなら左腰、左利きなら右腰（モデル空間では +X がプレイヤーの左）
        float side = player.getMainArm() == HumanoidArm.RIGHT ? 1F : -1F;

        pose.pushPose();
        getParentModel().body.translateAndRotate(pose); // しゃがみ等の胴体の動きに追従
        pose.translate(side * HIP_X / 16F, HIP_Y / 16F, HIP_Z / 16F);
        pose.mulPose(Axis.YP.rotationDegrees(side * SPLAY_DEGREES)); // 鞘尻を外へ
        pose.mulPose(Axis.XP.rotationDegrees(TILT_DEGREES));          // 刀身の向き(モデル+Y)を後ろ下がりへ
        pose.mulPose(Axis.YP.rotationDegrees(180F));                  // 刃を上に向ける
        pose.scale(SCALE, SCALE, SCALE);
        // ItemRenderer はモデル中心(8,8,8)を原点に置くので、鍔の位置が原点に来るようずらす
        pose.translate(0F, -(TSUBA_Y - 8F) / 16F, -(CENTER_Z - 8F) / 16F);

        mc.getItemRenderer().render(stack, ItemDisplayContext.NONE, false, pose, buffers, light,
                OverlayTexture.NO_OVERLAY, model);
        pose.popPose();
    }
}
