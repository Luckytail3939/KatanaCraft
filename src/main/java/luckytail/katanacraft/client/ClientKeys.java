package luckytail.katanacraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * キー設定。
 * ガードはデフォルト R キー。設定 → 操作設定 → 「KatanaCraft」欄から
 * マウスのサイドボタン（ボタン4/5）を含め自由に割り当て直せます。
 * 攻撃はバニラの「攻撃/破壊」「使う/置く」キーに従います。
 */
public final class ClientKeys {
    private ClientKeys() {}

    public static final String CATEGORY = "key.categories.katanacraft";

    public static final KeyMapping GUARD = new KeyMapping(
            "key.katanacraft.guard",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            CATEGORY);
}
