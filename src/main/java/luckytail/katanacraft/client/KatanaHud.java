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
    public static final IGuiOverlay POSTURE = (gui, g, partialTick, width, height) -> {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.options.hideGui || !KatanaItem.isHoldingKatana(p) || !gui.shouldDrawSurvivalElements()) return;

        int cx = width / 2;
        int y = height - 29;
        int half = 91;
        long now = ClientKatanaState.clientTicks;
        float t = now + partialTick;

        float ratio = Mth.clamp(ClientKatanaState.displayPosture / ClientKatanaState.maxPosture, 0F, 1F);
        boolean broken = p.hasEffect(ModEffects.GUARD_BREAK.get());
        boolean empowered = ClientKatanaState.isEmpowered();

        // 強化状態: ゲージの周りにモヤ（バーの下に描く）
        if (empowered) drawEmpowerMist(g, cx, y, half, t, true);

        // 枠と下地
        boolean exhausted = now - ClientKatanaState.exhaustedTime < 8;
        int frame = exhausted ? ((now / 2) % 2 == 0 ? 0xFFE0402B : 0xB0000000) : 0xB0000000;
        g.fill(cx - half - 1, y - 1, cx + half + 1, y + 6, frame);
        g.fill(cx - half, y, cx + half, y + 5, 0x60402040);

        int color;
        if (broken) color = (now / 2) % 2 == 0 ? 0xFFFF3B30 : 0xFFFFFFFF;
        else if (now - ClientKatanaState.postureHitTime < 3) color = 0xFFFFFFFF;
        else if (empowered) color = 0xFFE6D2FF;
        else if (ratio > 0.5F) color = 0xFFEADFB4;
        else if (ratio > 0.25F) color = 0xFFF0A040;
        else color = ((now / 4) % 2 == 0) ? 0xFFE0402B : 0xFFB02818; // 残り少ないと点滅

        int fillHalf = Math.round(half * ratio);
        if (fillHalf > 0) {
            g.fill(cx - fillHalf, y, cx + fillHalf, y + 5, color);
            g.fill(cx - fillHalf, y, cx + fillHalf, y + 1, 0x60FFFFFF); // ハイライト
        }

        // 強化状態: バーの上を漂う光の粒（バーの上に描く）
        if (empowered) drawEmpowerMist(g, cx, y, half, t, false);

        // 固有技クールダウン（バーの上の細い線）
        if (ClientKatanaState.artCooldown > 0) {
            int cd = Math.round(half * ClientKatanaState.artCooldown / (float) ClientKatanaState.artCooldownMax);
            g.fill(cx - cd, y - 3, cx + cd, y - 2, 0xCC9B6BFF);
        }

        // 中央の紋（ガード中は紫に光る）
        int emblem = empowered ? ((now / 3) % 2 == 0 ? 0xFFFFFFFF : 0xFFC9A2FF)
                : ClientKatanaState.guarding ? 0xFFC9A2FF : 0xFF5A4A6A;
        g.fill(cx - 3, y - 2, cx + 3, y + 7, 0xFF000000);
        g.fill(cx - 2, y - 1, cx + 2, y + 6, emblem);

        // 弾き回数の目盛り（中央の紋の上）。強化中は表示しない
        if (!empowered && ClientKatanaState.parryStacks > 0) {
            int n = ClientKatanaState.parriesToEmpower;
            int pipX = cx - (n * 5 - 2) / 2;
            for (int i = 0; i < n; i++) {
                int x = pipX + i * 5;
                g.fill(x - 1, y - 9, x + 4, y - 4, 0xC0000000);
                g.fill(x, y - 8, x + 3, y - 5, i < ClientKatanaState.parryStacks ? 0xFFC9A2FF : 0xFF3A2E48);
            }
        }
    };

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
