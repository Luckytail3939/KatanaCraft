package luckytail.katanacraft.combat;

/**
 * クライアントから送られる攻撃入力。
 * ordinal はルーンHUDのグリフ番号も兼ねる（textures/gui/runes.png の左から順）。
 */
public enum InputType {
    LIGHT_TAP,   // 左クリック
    HEAVY_TAP,   // 右クリック
    LIGHT_HOLD,  // 左クリック長押し
    HEAVY_HOLD;  // 右クリック長押し

    public static InputType byId(int id) {
        InputType[] v = values();
        return id >= 0 && id < v.length ? v[id] : LIGHT_TAP;
    }
}
