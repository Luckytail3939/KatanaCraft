package luckytail.katanacraft.item;

import luckytail.katanacraft.combat.InputType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import javax.annotation.Nullable;

import java.util.List;

/**
 * 刀の基底クラス。
 * 通常攻撃・ガード・弾きは全刀共通（combat パッケージ）で、
 * 刀ごとに違うのは「固有技のルーン列」と「固有技の中身」だけ、という設計。
 * 新しい刀は、このクラスを継承して getArtSequence / artTick を実装する。
 */
public abstract class KatanaItem extends SwordItem {

    public KatanaItem(Tier tier, int attackDamageModifier, float attackSpeedModifier, Properties properties) {
        super(tier, attackDamageModifier, attackSpeedModifier, properties);
    }

    /** 固有技を発動させるルーン（入力）の並び */
    public abstract List<InputType> getArtSequence();

    /** 固有技の長さ (ticks) */
    public int getArtDuration() {
        return 22;
    }

    /** 固有技で、抜刀する（腰→手）tick。アニメーションで柄を握って引き抜く瞬間に合わせる */
    public int getArtDrawTick() {
        return 5;
    }

    /** 固有技で、刀身が鞘に収まる（手→腰）tick */
    public int getArtSheatheTick() {
        return getArtDuration() - 4;
    }

    /**
     * 固有技の処理。tick は 0 から getArtDuration()-1 まで毎tick呼ばれる。
     * @param empowered 弾きで強化された状態で発動したか（刀ごとに強化内容を決める）
     */
    public abstract void artTick(ServerPlayer player, int tick, boolean empowered);

    /**
     * 腰に差したとき描くモデル: assets/<namespace>/models/item/<刀のID>_sheathed_3d.json
     * （例: katanacraft:item/shiden_sheathed_3d）。新しい刀もこの名前でモデルを置けば帯刀される。
     */
    public ResourceLocation getHipModelLocation() {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(this);
        return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "item/" + id.getPath() + "_sheathed_3d");
    }

    public static boolean isKatana(ItemStack stack) {
        return stack.getItem() instanceof KatanaItem;
    }

    public static boolean isHoldingKatana(Player player) {
        return isKatana(player.getMainHandItem());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.katanacraft.controls.light").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.katanacraft.controls.heavy").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.katanacraft.controls.guard").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.katanacraft.controls.sneak").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.katanacraft.empower").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable(getDescriptionId() + ".art").withStyle(ChatFormatting.LIGHT_PURPLE));
        MutableComponent seq = Component.empty();
        List<InputType> art = getArtSequence();
        for (int i = 0; i < art.size(); i++) {
            if (i > 0) seq.append(" → ");
            seq.append(Component.translatable("input.katanacraft." + art.get(i).name().toLowerCase(java.util.Locale.ROOT)));
        }
        tooltip.add(seq.withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable(getDescriptionId() + ".art.empowered").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
