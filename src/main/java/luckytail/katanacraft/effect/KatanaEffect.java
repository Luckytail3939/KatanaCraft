package luckytail.katanacraft.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** 効果そのものは属性修飾子・イベント側で処理するシンプルなエフェクト */
public class KatanaEffect extends MobEffect {
    public KatanaEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}
