package luckytail.katanacraft.combat;

import java.util.ArrayList;
import java.util.List;

/** プレイヤー1人分の戦闘状態（サーバー側・非永続） */
public class CombatState {
    // 体幹（満タンから削られていき、0で崩れる）
    public float posture = -1F; // -1 = 未初期化（初回tickで最大値を入れる）
    public int ticksSincePostureHit = 9999;

    // ガード
    public boolean guardHeld;
    public boolean guardLocked;      // ガード崩し後、キーを離すまでガード不可
    public int parryTicks;           // 弾き受付の残りtick
    public int lastGuardPressTick = -9999;
    public int guardCount;           // 連続通常ガード回数
    public int lastGuardHitTick = -9999;

    // モーション
    public KatanaAction action;
    public int actionTick;
    public int actionDuration;
    public InputType buffered;
    public int lightStep;            // 次に出る通常斬撃の段 (0..2)
    public int comboWindow;          // 次段へ繋げられる残りtick

    // ルーン / 固有技
    public final List<InputType> runes = new ArrayList<>();
    public int runeTimer;
    public int artCooldown;
    public boolean artFiredPending;

    // 弾きによる強化
    public int parryStacks;
    public int lastParryTick = -9999;
    public int empoweredTicks;       // >0 なら強化状態（残りtick）
    public boolean artEmpowered;     // 実行中の固有技が強化版か

    public boolean dirty = true;

    public boolean isGuarding() {
        return guardHeld && !guardLocked && action == null;
    }
}
