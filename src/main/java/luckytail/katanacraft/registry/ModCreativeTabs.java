package luckytail.katanacraft.registry;

import luckytail.katanacraft.Katanacraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Katanacraft.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.katanacraft"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> ModItems.SHIDEN.get().getDefaultInstance())
            .displayItems((params, output) -> {
                output.accept(ModItems.SHIDEN.get());
            })
            .build());
}
