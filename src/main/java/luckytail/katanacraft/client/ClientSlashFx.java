package luckytail.katanacraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import luckytail.katanacraft.Katanacraft;
import luckytail.katanacraft.combat.KatanaAction;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 斬撃エフェクト（弧を描く剣閃）。
 * プレイヤーの正面に、刀の軌跡に沿った光の帯を一瞬だけ描く。描画はクライアントのみ。
 *
 * 各技の「どの tick に、どの向きの弧を出すか」は {@link #onAction} の表で決める。
 * 当たりの tick（KatanaMoves）とアニメーションの振り抜く瞬間に合わせてある。
 *
 * 弧の向き angle（度）: 弧が「進んでいく先」の方向を、画面上の向きで表す。
 *   0 = 左から右へ（横一文字）, 180 = 右から左へ,
 *   270 = 上から下へ（唐竹割り）, 90 = 下から上へ,
 *   225 = 右上から左下へ（袈裟斬り）, 45 = 左下から右上へ（斬り上げ）
 */
@Mod.EventBusSubscriber(modid = Katanacraft.MODID, value = Dist.CLIENT)
public final class ClientSlashFx {
    private ClientSlashFx() {}

    // ---- 見た目の調整用 ----
    /** 色（外側の芯 / 内側のにじみ）。紫電の紫 */
    private static final float[] CORE = {0.92F, 0.86F, 1.0F};
    private static final float[] GLOW = {0.62F, 0.36F, 1.0F};
    /** 全体の明るさ（「控えめ」にしたいときは下げる） */
    private static final float INTENSITY = 0.8F;
    /** 弧を構成する分割数 */
    private static final int SEGMENTS = 24;
    /**
     * 弧の面の傾き（度）。0だと一人称で真横から見る形になり線にしか見えないので、
     * 少し傾けて三日月形に見せる
     */
    private static final float TILT = 22F;
    /** 弧の中心: 足元からの高さ / 視線方向へのずらし（ブロック） */
    private static final float CENTER_HEIGHT = 1.45F;
    private static final float CENTER_FORWARD = 0.9F;

    private record Slash(int ownerId, long startTick, float delay,
                         Vec3 forward, Vec3 right, Vec3 up,
                         float angle, float radius, float width, float spanDeg, float tiltDeg,
                         float sweepTicks, float fadeTicks, float height, float forwardOffset) {
    }

    private static final List<Slash> SLASHES = new ArrayList<>();

    // ------------------------------------------------------------------
    // 技ごとの斬撃の出し方
    // ------------------------------------------------------------------

    public static void onAction(Player player, KatanaAction action) {
        switch (action) {
            // 通常斬撃
            case LIGHT_1 -> add(player, 4.8F, 225, 1.6F, 0.28F, 120, TILT, 2.5F);   // 右上→左下（袈裟斬り）
            case LIGHT_2 -> add(player, 1.8F, 45, 1.6F, 0.28F, 120, TILT, 2.5F);    // 左下→右上（斬り上げ）
            case LIGHT_3 -> add(player, 5.8F, 270, 1.7F, 0.36F, 125, 10, 2.5F);     // 上→下（唐竹割り）
            // 横一文字（タメの後）
            case HEAVY -> add(player, 13.0F, 0, 2.1F, 0.42F, 140, TILT, 2.5F);
            // 燕返し: 三方向から同時に
            case FLURRY -> tsubame(player, 4.6F);
            // 突進: 燕返し + 横一閃 + 返しの横一閃
            case DASH_STORM -> {
                tsubame(player, 11.6F);
                add(player, 15.8F, 0, 2.0F, 0.4F, 140, TILT, 2.5F);
                add(player, 18.8F, 180, 2.0F, 0.42F, 140, TILT, 2.5F);
            }
            // 紫電一閃: 大きな横一閃
            case ART -> add(player, 5.6F, 0, 2.4F, 0.5F, 150, TILT, 2.5F);
            // 弾き: 小さく鋭い火花の弧
            case PARRY -> add(player, 0F, 30, 1.0F, 0.15F, 80, TILT, 1.5F);
            default -> {
            }
        }
    }

    /** 燕返し: 縦・右上から・左上からの三本がほぼ同時に走る */
    private static void tsubame(Player player, float tick) {
        add(player, tick, 270, 1.7F, 0.3F, 120, 10, 2.0F);
        add(player, tick + 0.6F, 210, 1.75F, 0.3F, 120, TILT, 2.0F);
        add(player, tick + 1.2F, 330, 1.75F, 0.3F, 120, TILT, 2.0F);
    }

    /**
     * @param delay  モーション開始から何 tick 後に出すか（小数可）
     * @param angle  弧が進む方向（画面上の角度、度）
     * @param radius 弧の半径（ブロック）
     * @param width  帯の太さ（ブロック）
     * @param span   弧の長さ（度）
     * @param tilt   弧の面の傾き（度）
     * @param sweep  弧が走り切るまでの時間（tick）
     */
    private static void add(Player player, float delay, float angle, float radius, float width, float span, float tilt, float sweep) {
        Vec3 f = player.getViewVector(1F);
        Vec3 worldUp = new Vec3(0, 1, 0);
        Vec3 r = f.cross(worldUp);
        if (r.lengthSqr() < 1.0E-4) r = new Vec3(1, 0, 0);
        r = r.normalize();          // 画面右
        Vec3 u = r.cross(f).normalize(); // 画面上
        SLASHES.add(new Slash(player.getId(), ClientKatanaState.clientTicks, delay, f, r, u,
                angle, radius, width, span, tilt, sweep, 5F, CENTER_HEIGHT, CENTER_FORWARD));
    }

    // ------------------------------------------------------------------
    // 描画
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || SLASHES.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        float pt = event.getPartialTick();
        float now = ClientKatanaState.clientTicks + pt;
        Camera cam = event.getCamera();
        Vec3 camPos = cam.getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());

        pose.pushPose();
        pose.translate(-camPos.x, -camPos.y, -camPos.z);
        Matrix4f m = pose.last().pose();

        Iterator<Slash> it = SLASHES.iterator();
        while (it.hasNext()) {
            Slash s = it.next();
            float age = now - (s.startTick + s.delay);
            if (age > s.sweepTicks + s.fadeTicks) {
                it.remove();
                continue;
            }
            if (age < 0) continue;
            Entity owner = mc.level.getEntity(s.ownerId);
            if (owner == null) {
                it.remove();
                continue;
            }
            // 持ち主に追従（突進中でもついてくる）
            Vec3 origin = owner.getPosition(pt).add(0, s.height, 0).add(s.forward.scale(s.forwardOffset));
            drawSlash(vc, m, s, origin, age);
        }
        pose.popPose();
        buffers.endBatch(RenderType.lightning());
    }

    private static void drawSlash(VertexConsumer vc, Matrix4f m, Slash s, Vec3 c, float age) {
        double a = Math.toRadians(s.angle);
        // 弧が進む方向 A（画面内）と、弧の面の中心方向（正面）F
        Vec3 A = s.right.scale(Math.cos(a)).add(s.up.scale(Math.sin(a)));
        // 弧の面を少し傾ける: 正面 F を、A と直交する画面内の向き P へ倒す
        Vec3 P = s.right.scale(Math.cos(a + Math.PI / 2)).add(s.up.scale(Math.sin(a + Math.PI / 2)));
        double tilt = Math.toRadians(s.tiltDeg);
        Vec3 F = s.forward.scale(Math.cos(tilt)).add(P.scale(Math.sin(tilt)));
        float half = (float) Math.toRadians(s.spanDeg / 2F);

        // 進行度: 先端(head)は走り抜け、尾(tail)が追いかけて消える
        float sweep = Mth.clamp(age / s.sweepTicks, 0F, 1F);
        float head = 1F - (1F - sweep) * (1F - sweep);            // ease-out
        float tail = Math.max(0F, head - 0.55F);
        float fade = age <= s.sweepTicks ? 1F : 1F - (age - s.sweepTicks) / s.fadeTicks;
        if (fade <= 0F) return;
        tail += (1F - fade) * (head - tail);                        // 消えるときは尾から縮む

        float r0 = s.radius - s.width, r1 = s.radius;
        for (int i = 0; i < SEGMENTS; i++) {
            float f0 = tail + (head - tail) * i / SEGMENTS;
            float f1 = tail + (head - tail) * (i + 1) / SEGMENTS;
            float ph0 = -half + 2F * half * f0;
            float ph1 = -half + 2F * half * f1;
            // 尾は透明、先端ほど明るい
            float k0 = (float) i / SEGMENTS, k1 = (float) (i + 1) / SEGMENTS;
            float al0 = INTENSITY * fade * k0 * k0, al1 = INTENSITY * fade * k1 * k1;
            // 太さも先端ほど太く（三日月型）
            float w0 = 0.35F + 0.65F * Mth.sin(k0 * Mth.PI * 0.85F + 0.15F);
            float w1 = 0.35F + 0.65F * Mth.sin(k1 * Mth.PI * 0.85F + 0.15F);

            Vec3 o0 = arc(c, F, A, ph0, r1), o1 = arc(c, F, A, ph1, r1);
            Vec3 i0 = arc(c, F, A, ph0, r1 - (r1 - r0) * w0), i1 = arc(c, F, A, ph1, r1 - (r1 - r0) * w1);
            Vec3 m0 = arc(c, F, A, ph0, r1 - (r1 - r0) * w0 * 0.3F), m1 = arc(c, F, A, ph1, r1 - (r1 - r0) * w1 * 0.3F);

            // 外側の芯（明るい）: 外周 → 少し内側
            quad(vc, m, o0, o1, m1, m0, CORE, al0, al1, al1 * 0.9F, al0 * 0.9F);
            // 内側のにじみ（紫）: 少し内側 → 内周（透明）
            quad(vc, m, m0, m1, i1, i0, GLOW, al0 * 0.8F, al1 * 0.8F, 0F, 0F);
        }
    }

    private static Vec3 arc(Vec3 c, Vec3 F, Vec3 A, double phi, double r) {
        return c.add(F.scale(Math.cos(phi) * r)).add(A.scale(Math.sin(phi) * r));
    }

    /** 両面から見えるように、表と裏の2枚を描く */
    private static void quad(VertexConsumer vc, Matrix4f m, Vec3 a, Vec3 b, Vec3 c, Vec3 d,
                             float[] col, float aa, float ab, float ac, float ad) {
        v(vc, m, a, col, aa); v(vc, m, b, col, ab); v(vc, m, c, col, ac); v(vc, m, d, col, ad);
        v(vc, m, d, col, ad); v(vc, m, c, col, ac); v(vc, m, b, col, ab); v(vc, m, a, col, aa);
    }

    private static void v(VertexConsumer vc, Matrix4f m, Vec3 p, float[] col, float alpha) {
        vc.vertex(m, (float) p.x, (float) p.y, (float) p.z)
                .color(col[0], col[1], col[2], Mth.clamp(alpha, 0F, 1F))
                .endVertex();
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        SLASHES.clear();
    }
}
