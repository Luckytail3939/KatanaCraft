package luckytail.katanacraft;

import com.mojang.logging.LogUtils;
import luckytail.katanacraft.network.ModNetwork;
import luckytail.katanacraft.registry.ModCreativeTabs;
import luckytail.katanacraft.registry.ModEffects;
import luckytail.katanacraft.registry.ModEntities;
import luckytail.katanacraft.registry.ModItems;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * KatanaCraft - 抜刀剣に代わる刀mod。
 *
 * パッケージ構成
 *  registry/  : アイテム・エフェクト・エンティティ・クリエイティブタブの登録
 *  item/      : 刀アイテム（KatanaItem が基底。刀ごとの固有技は派生クラスで実装）
 *  combat/    : サーバー側の戦闘ロジック（弾き・ガード・体幹・攻撃モーション・ルーン）
 *  entity/    : 光波斬撃などのエンティティ
 *  network/   : クライアント⇔サーバー通信
 *  client/    : 入力処理・HUD（体幹ゲージ/ルーン）・レンダラー・アニメーションフック
 */
@Mod(Katanacraft.MODID)
public class Katanacraft {

    public static final String MODID = "katanacraft";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Katanacraft(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        ModItems.ITEMS.register(modEventBus);
        ModEffects.EFFECTS.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);

        modEventBus.addListener(this::commonSetup);

        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
    }
}
