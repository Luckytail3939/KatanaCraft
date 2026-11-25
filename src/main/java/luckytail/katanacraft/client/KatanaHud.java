package luckytail.katanacraft.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import luckytail.katanacraft.Katanacraft;
import luckytail.katanacraft.item.KatanaItem;
import luckytail.katanacraft.registry.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/** 体幹ゲージ（経験値バーの位置）とルーン（画面中央下部）の描画 */
public final class KatanaHud {
    private KatanaHud() {}

    private static final ResourceLocation RUNE_TEX = ResourceLocation.fromNamespaceAndPath(Katanacraft.MODID, "textures/gui/runes.png");
    private static final int CELL = 32, ATLAS_W = 192, ATLAS_H = 32;
    private static final int GLYPH_EMPTY = 4, GLYPH_GLOW = 5;
    private static final int FLASH_TICKS = 16;

    // ------------------------------------------------------------------
    // 体幹ゲージ: 中央から左右に伸びるバー。削られると中央へ縮む
    // ------------------------------------------------------------------
    // posture.png (256x32) のレイアウト
    private static final ResourceLocation POSTURE_TEX = ResourceLocation.fromNamespaceAndPath(Katanacraft.MODID, "textures/gui/posture.png");
    private static final int TEX_W = 256, TEX_H = 32;
    private static final int FRAME_U = 0, FRAME_V = 0, FRAME_W = 186, FRAME_H = 9;     // ゲージの枠（内側182x5は透明）
    private static final int FILL_U = 0, FILL_V = 10, FILL_W = 182, FILL_H = 5;        // ゲージの中身（着色）
    private static final int EMBLEM_U = 0, EMBLEM_V = 16, EMBLEM_W = 9, EMBLEM_H = 13; // 紋の枠
    private static final int CORE_U = 10, CORE_V = 16, CORE_W = 7, CORE_H = 11;        // 紋の中身（着色）
    private static final int PIP_EMPTY_U = 18, PIP_LIT_U = 24, PIP_V = 16, PIP_SIZE = 5; // 弾き回数の目盛り

    // ゲージの色
    private static final int COLOR_HIGH = 0xF2D45C;      // 黄
    private static final int COLOR_MID = 0xF0A040;       // オレンジ
    private static final int COLOR_LOW = 0xE0402B;       // 赤
    private static final int COLOR_LOW_DARK = 0xA02414;  // 赤（点滅の暗い側）
    private static final int COLOR_EMPOWERED = 0xD9BFFF; // 紫（強化中）
    private static final int COLOR_PURPLE = 0x9B4DFF;     // 紫（ガード中の紋・弾き目盛り）
    private static final int COLOR_EMBLEM_IDLE = 0x5A5068; // 通常時の紋

    public static final IGuiOverlay POSTURE = (gui, g, partialTick, width, height) -> {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.options.hideGui || !KatanaItem.isHoldingKatana(p) || !gui.shouldDrawSurvivalElements()) return;

        int cx = width / 2;
        int barX = cx - FILL_W / 2;  // ゲージ中身の左端（経験値バーと同じ位置）
        int barY = height - 29;
        int y = barY;                 // モヤ描画用の基準
        int half = FILL_W / 2;
        long now = ClientKatanaState.clientTicks;
        float t = now + partialTick;

        float ratio = Mth.clamp(ClientKatanaState.displayPosture / ClientKatanaState.maxPosture, 0F, 1F);
        boolean broken = p.hasEffect(ModEffects.GUARD_BREAK.get());
        boolean empowered = ClientKatanaState.isEmpowered();
        boolean exhausted = now - ClientKatanaState.exhaustedTime < 8;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // 強化状態: ゲージの周りにモヤ（ゲージの下に描く）
        if (empowered) {
            drawEmpowerMist(g, cx, y, half, t, true);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
        }

        // 下地（枠の内側）
        g.fill(barX + 1, barY, barX + FILL_W - 1, barY + FILL_H, 0x70201028);

        // ゲージの中身: 中央から左右に伸び、削られると中央へ縮む
        int fillHalf = Math.round(half * ratio);
        if (fillHalf > 0) {
            setColor(g, fillColor(ratio, broken, empowered, now), 1F);
            g.blit(POSTURE_TEX, cx - fillHalf, barY, FILL_U + half - fillHalf, FILL_V, fillHalf * 2, FILL_H, TEX_W, TEX_H);
        }

        // 固有技クールダウン（ゲージ下端の細い線）
        if (ClientKatanaState.artCooldown > 0) {
            int cd = Math.round(half * ClientKatanaState.artCooldown / (float) ClientKatanaState.artCooldownMax);
            g.setColor(1F, 1F, 1F, 1F);
            g.fill(cx - cd, barY + FILL_H - 1, cx + cd, barY + FILL_H, 0xCC9B6BFF);
        }

        // 枠（体幹不足で攻撃できなかった時は赤く点滅）
        if (exhausted && (now / 2) % 2 == 0) setColor(g, 0xFF6050, 1F);
        else g.setColor(1F, 1F, 1F, 1F);
        g.blit(POSTURE_TEX, barX - 2, barY - 2, FRAME_U, FRAME_V, FRAME_W, FRAME_H, TEX_W, TEX_H);

        // 強化状態: ゲージの上を漂う光の粒
        if (empowered) {
            drawEmpowerMist(g, cx, y, half, t, false);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
        }

        // 中央の紋: 中身を着色 → 枠を重ねる
        int ex = cx - EMBLEM_W / 2;
        int ey = barY + FILL_H / 2 - EMBLEM_H / 2;
        setColor(g, emblemColor(broken, empowered, now), 1F);
        g.blit(POSTURE_TEX, ex + 1, ey + 1, CORE_U, CORE_V, CORE_W, CORE_H, TEX_W, TEX_H);
        g.setColor(1F, 1F, 1F, 1F);
        g.blit(POSTURE_TEX, ex, ey, EMBLEM_U, EMBLEM_V, EMBLEM_W, EMBLEM_H, TEX_W, TEX_H);

        // 弾き回数の目盛り（紋の上）。強化中は表示しない
        if (!empowered && ClientKatanaState.parryStacks > 0) {
            int n = ClientKatanaState.parriesToEmpower;
            int step = PIP_SIZE + 1;
            int px0 = cx - (n * step - 1) / 2;
            int py = ey - PIP_SIZE - 1;
            for (int i = 0; i < n; i++) {
                boolean lit = i < ClientKatanaState.parryStacks;
                if (lit) setColor(g, COLOR_PURPLE, 1F);
                else g.setColor(1F, 1F, 1F, 1F);
                g.blit(POSTURE_TEX, px0 + i * step, py, lit ? PIP_LIT_U : PIP_EMPTY_U, PIP_V, PIP_SIZE, PIP_SIZE, TEX_W, TEX_H);
            }
        }

        g.setColor(1F, 1F, 1F, 1F);
        RenderSystem.disableBlend();
    };

    /** ゲージ中身の色: 黄 → オレンジ → 赤（残り少ないと点滅）。強化中は紫 */
    private static int fillColor(float ratio, boolean broken, boolean empowered, long now) {
        if (broken) return (now / 2) % 2 == 0 ? 0xFF3B30 : 0xFFFFFF;
        if (now - ClientKatanaState.postureHitTime < 3) return 0xFFFFFF;
        if (empowered) return COLOR_EMPOWERED;
        if (ratio > 0.6F) return COLOR_HIGH;
        if (ratio > 0.3F) return lerpColor(COLOR_MID, COLOR_HIGH, (ratio - 0.3F) / 0.3F);
        int low = lerpColor(COLOR_LOW, COLOR_MID, ratio / 0.3F);
        if (ratio < 0.2F && (now / 4) % 2 == 0) return COLOR_LOW_DARK;
        return low;
    }

    /** 紋の色: 通常はくすんだ紫、ガード中は紫、強化中は白と紫で明滅、崩れたら赤 */
    private static int emblemColor(boolean broken, boolean empowered, long now) {
        if (broken) return 0xFF3B30;
        if (empowered) return (now / 3) % 2 == 0 ? 0xFFFFFF : COLOR_PURPLE;
        if (ClientKatanaState.guarding) return COLOR_PURPLE;
        return COLOR_EMBLEM_IDLE;
    }

    private static int lerpColor(int a, int b, float f) {
        f = Mth.clamp(f, 0F, 1F);
        int r = (int) Mth.lerp(f, (a >> 16) & 0xFF, (b >> 16) & 0xFF);
        int gg = (int) Mth.lerp(f, (a >> 8) & 0xFF, (b >> 8) & 0xFF);
        int bb = (int) Mth.lerp(f, a & 0xFF, b & 0xFF);
        return (r << 16) | (gg << 8) | bb;
    }

    private static void setColor(GuiGraphics g, int rgb, float alpha) {
        g.setColor(((rgb >> 16) & 0xFF) / 255F, ((rgb >> 8) & 0xFF) / 255F, (rgb & 0xFF) / 255F, alpha);
    }

    /**
     * 強化状態のモヤ。
     * back=true: バーの下に大きくぼんやりした光、back=false: バーの上を立ち昇る小さな光の粒
     */
    private static void drawEmpowerMist(GuiGraphics g, int cx, int y, int half, float t, boolean back) {
        float sinceStart = t - ClientKatanaState.empowerStart;
        float intro = Mth.clamp(sinceStart / 10F, 0F, 1F);
        float burst = sinceStart < 12 ? (1F - sinceStart / 12F) : 0F;  // 強化した瞬間に強く光る
        float remain = ClientKatanaState.empoweredTicks / 20F;
        float ending = remain < 3F ? (0.5F + 0.5F * Mth.sin(t * 0.8F)) : 1F; // 残り3秒で明滅
        float strength = intro * ending;

        RenderSystem.enableBlend();
        if (back) {
            int blobs = 14;
            for (int i = 0; i < blobs; i++) {
                float fx = (i + 0.5F) / blobs;
                int x = Math.round(cx - half + fx * half * 2) - 8;
                float wob = Mth.sin(t * 0.12F + i * 1.7F);
                float size = 1.5F + 0.35F * wob + burst * 1.5F;
                float a = (0.30F + 0.12F * Mth.sin(t * 0.2F + i)) * strength + burst * 0.5F;
                drawGlyph(g, GLYPH_GLOW, x, y - 6 + Math.round(wob * 1.5F), 16, size, 0.62F, 0.38F, 1F, a, true);
            }
        } else {
            int motes = 18;
            for (int i = 0; i < motes; i++) {
                float phase = (t * 0.025F + i * 0.618F) % 1F;
                int x = cx - half + Math.floorMod(i * 53 + (int) (Mth.sin(i * 3.1F) * 20), half * 2);
                float drift = Mth.sin(t * 0.1F + i) * 2F;
                int py = Math.round(y + 3 - phase * 14F);
                float a = (1F - phase) * 0.8F * strength;
                drawGlyph(g, GLYPH_GLOW, x + Math.round(drift) - 3, py - 3, 6, 1F, 0.85F, 0.7F, 1F, a, true);
            }
        }
        g.setColor(1F, 1F, 1F, 1F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    // ------------------------------------------------------------------
    // ルーン: 入力が揃っていくごとに紋様が浮かび上がる
    // ------------------------------------------------------------------
    public static final IGuiOverlay RUNES = (gui, g, partialTick, width, height) -> {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.options.hideGui || !KatanaItem.isHoldingKatana(p)) return;

        int n = ClientKatanaState.sequenceLength;
        if (n <= 0) return;
        float now = ClientKatanaState.clientTicks + partialTick;
        float flashAge = now - ClientKatanaState.flashStart;
        boolean flashing = flashAge >= 0 && flashAge < FLASH_TICKS;
        byte[] runes = ClientKatanaState.runes;
        if (runes.length == 0 && !flashing) return;

        int size = 20, gap = 6;
        int total = n * size + (n - 1) * gap;
        int x0 = width / 2 - total / 2;
        int y = height - 80;
        float fade = runes.length > 0 ? Mth.clamp((ClientKatanaState.runeTimer - partialTick) / 10F, 0F, 1F) : 1F;
        float pulse = 0.85F + 0.15F * Mth.sin(now * 0.3F);

        RenderSystem.enableBlend();
        for (int i = 0; i < n; i++) {
            int x = x0 + i * (size + gap);

            if (flashing) {
                // 発動: 全ルーンが白く弾けて消える
                float t = flashAge / FLASH_TICKS;
                int glyph = i < ClientKatanaState.flashRunes.length ? ClientKatanaState.flashRunes[i] : GLYPH_EMPTY;
                drawGlyph(g, GLYPH_GLOW, x, y, size, 2.2F + t, 0.75F, 0.5F, 1F, (1 - t) * 0.9F, true);
                drawGlyph(g, glyph, x, y, size, 1F + t * 0.8F, 1F, 1F, 1F, 1 - t, true);
                continue;
            }

            if (i < runes.length) {
                float age = now - ClientKatanaState.runeTimes[i];
                float appear = Mth.clamp(age / 5F, 0F, 1F);
                float ease = 1 - (1 - appear) * (1 - appear);
                float scale = 1.8F - 0.8F * ease;
                float a = appear * fade;
                drawGlyph(g, GLYPH_GLOW, x, y, size, 1.9F, 0.6F, 0.35F, 1F, a * 0.55F * pulse, true);
                drawGlyph(g, runes[i], x, y, size, scale, 0.9F, 0.75F, 1F, a, false);
            } else {
                drawGlyph(g, GLYPH_EMPTY, x, y, size, 1F, 0.6F, 0.5F, 0.8F, 0.35F * fade, false);
            }
        }
        g.setColor(1F, 1F, 1F, 1F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    };

    private static void drawGlyph(GuiGraphics g, int glyph, int x, int y, int size, float scale,
                                  float r, float gr, float b, float a, boolean additive) {
        if (a <= 0.01F) return;
        g.pose().pushPose();
        g.pose().translate(x + size / 2F, y + size / 2F, 0);
        g.pose().scale(scale, scale, 1F);
        if (additive) {
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        } else {
            RenderSystem.defaultBlendFunc();
        }
        g.setColor(r, gr, b, Mth.clamp(a, 0F, 1F));
        g.blit(RUNE_TEX, -size / 2, -size / 2, size, size, glyph * CELL, 0, CELL, CELL, ATLAS_W, ATLAS_H);
        g.setColor(1F, 1F, 1F, 1F);
        RenderSystem.defaultBlendFunc();
        g.pose().popPose();
    }
}
