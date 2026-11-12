package luckytail.katanacraft.registry;

import luckytail.katanacraft.Katanacraft;
import luckytail.katanacraft.effect.KatanaEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, Katanacraft.MODID);

    /** 体勢崩し: 行動不能（移動ほぼ不可・攻撃/ガード入力を受け付けない） */
    public static final RegistryObject<MobEffect> GUARD_BREAK = EFFECTS.register("guard_break",
            () -> new KatanaEffect(MobEffectCategory.HARMFUL, 0xC23B22)
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED, "6f1d3c2a-8a51-4c6e-9d0e-2b7a51c4e901",
                            -0.95D, AttributeModifier.Operation.MULTIPLY_TOTAL));

    /** 無防備: 被ダメージ上昇（倍率はコンフィグ） */
    public static final RegistryObject<MobEffect> VULNERABLE = EFFECTS.register("vulnerable",
            () -> new KatanaEffect(MobEffectCategory.HARMFUL, 0x8E2DE2));
}
