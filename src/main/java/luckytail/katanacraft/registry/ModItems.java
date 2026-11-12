package luckytail.katanacraft.registry;

import luckytail.katanacraft.Katanacraft;
import luckytail.katanacraft.item.ShidenItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Katanacraft.MODID);

    /** 刀「紫電」: 攻撃力 1 + 3(ダイヤ) + 4 = 8、攻撃速度は表示用（実際の攻撃間隔はモーションで決まる） */
    public static final RegistryObject<Item> SHIDEN = ITEMS.register("shiden",
            () -> new ShidenItem(Tiers.DIAMOND, 4, -2.2F,
                    new Item.Properties().rarity(Rarity.RARE)));
}
