package luckytail.katanacraft.registry;

import luckytail.katanacraft.Katanacraft;
import luckytail.katanacraft.entity.SlashWaveEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Katanacraft.MODID);

    public static final RegistryObject<EntityType<SlashWaveEntity>> SLASH_WAVE = ENTITIES.register("slash_wave",
            () -> EntityType.Builder.<SlashWaveEntity>of(SlashWaveEntity::new, MobCategory.MISC)
                    .sized(3.0F, 1.2F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .fireImmune()
                    .build("slash_wave"));
}
