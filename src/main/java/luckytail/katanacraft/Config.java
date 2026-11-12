package luckytail.katanacraft;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 戦闘バランス調整用コンフィグ（config/katanacraft-common.toml に出力されます）。
 * 単位の "ticks" は 20ticks = 1秒。
 */
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // ---- 弾き（ジャストガード） ----
    public static final ForgeConfigSpec.IntValue PARRY_WINDOW_TICKS;
    public static final ForgeConfigSpec.IntValue PARRY_REPRESS_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.DoubleValue PARRY_POSTURE_RECOVER;
    public static final ForgeConfigSpec.IntValue PARRY_STAGGER_TICKS;

    // ---- ガード / 体幹 ----
    public static final ForgeConfigSpec.DoubleValue MAX_POSTURE;
    public static final ForgeConfigSpec.DoubleValue GUARD_POSTURE_BASE;
    public static final ForgeConfigSpec.DoubleValue GUARD_POSTURE_PER_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue GUARD_CHIP_RATIO;
    public static final ForgeConfigSpec.IntValue GUARD_BREAK_COUNT;
    public static final ForgeConfigSpec.IntValue GUARD_CHAIN_RESET_TICKS;
    public static final ForgeConfigSpec.IntValue GUARD_BREAK_TICKS;
    public static final ForgeConfigSpec.DoubleValue VULNERABLE_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue POSTURE_REGEN_DELAY_TICKS;
    public static final ForgeConfigSpec.DoubleValue POSTURE_REGEN_PER_TICK;
    public static final ForgeConfigSpec.DoubleValue GUARD_MOVE_SLOW;

    // ---- 攻撃の体幹消費 ----
    public static final ForgeConfigSpec.DoubleValue COST_LIGHT;
    public static final ForgeConfigSpec.DoubleValue COST_LIGHT_FINISHER;
    public static final ForgeConfigSpec.DoubleValue COST_HEAVY;
    public static final ForgeConfigSpec.DoubleValue COST_FLURRY;
    public static final ForgeConfigSpec.DoubleValue COST_DASH_STORM;
    public static final ForgeConfigSpec.IntValue ATTACK_REGEN_DELAY_TICKS;

    // ---- 弾きによる強化 ----
    public static final ForgeConfigSpec.IntValue PARRIES_TO_EMPOWER;
    public static final ForgeConfigSpec.IntValue PARRY_STACK_RESET_TICKS;
    public static final ForgeConfigSpec.IntValue EMPOWER_DURATION_TICKS;

    // ---- ルーン / 固有技 ----
    public static final ForgeConfigSpec.IntValue RUNE_TIMEOUT_TICKS;
    public static final ForgeConfigSpec.IntValue ART_COOLDOWN_TICKS;

    static {
        BUILDER.push("parry");
        PARRY_WINDOW_TICKS = BUILDER.comment("ガードキーを押してから弾きが成立する猶予 (ticks)。4 = 0.2秒")
                .defineInRange("parryWindowTicks", 4, 1, 40);
        PARRY_REPRESS_COOLDOWN_TICKS = BUILDER.comment("連打対策: 前回のガード入力からこのticks以内の再入力では弾き判定が発生しない")
                .defineInRange("parryRepressCooldownTicks", 6, 0, 40);
        PARRY_POSTURE_RECOVER = BUILDER.comment("弾き成功時に回復する体幹")
                .defineInRange("parryPostureRecover", 8.0, 0.0, 1000.0);
        PARRY_STAGGER_TICKS = BUILDER.comment("弾かれた攻撃者が怯む時間 (ticks)")
                .defineInRange("parryStaggerTicks", 12, 0, 200);
        BUILDER.pop();

        BUILDER.push("guard");
        MAX_POSTURE = BUILDER.comment("体幹の最大値")
                .defineInRange("maxPosture", 100.0, 1.0, 10000.0);
        GUARD_POSTURE_BASE = BUILDER.comment("通常ガード1回で削られる体幹（固定値）")
                .defineInRange("guardPostureBase", 14.0, 0.0, 1000.0);
        GUARD_POSTURE_PER_DAMAGE = BUILDER.comment("通常ガードで削られる体幹（被ダメージ1あたり）")
                .defineInRange("guardPosturePerDamage", 3.0, 0.0, 100.0);
        GUARD_CHIP_RATIO = BUILDER.comment("通常ガード時に貫通するダメージの割合")
                .defineInRange("guardChipRatio", 0.2, 0.0, 1.0);
        GUARD_BREAK_COUNT = BUILDER.comment("この回数連続で通常ガードするとガードが崩れる")
                .defineInRange("guardBreakCount", 4, 1, 100);
        GUARD_CHAIN_RESET_TICKS = BUILDER.comment("通常ガードの間隔がこれを超えたら連続カウントをリセット (ticks)")
                .defineInRange("guardChainResetTicks", 50, 1, 1200);
        GUARD_BREAK_TICKS = BUILDER.comment("ガード崩し時の行動不能・被ダメージ上昇の時間 (ticks)。10 = 0.5秒")
                .defineInRange("guardBreakTicks", 10, 1, 200);
        VULNERABLE_MULTIPLIER = BUILDER.comment("ガード崩し中の被ダメージ倍率")
                .defineInRange("vulnerableMultiplier", 1.5, 1.0, 10.0);
        POSTURE_REGEN_DELAY_TICKS = BUILDER.comment("最後に体幹を削られてから回復が始まるまで (ticks)")
                .defineInRange("postureRegenDelayTicks", 40, 0, 1200);
        POSTURE_REGEN_PER_TICK = BUILDER.comment("体幹の自然回復量 (1tickあたり。ガード中は1/3)")
                .defineInRange("postureRegenPerTick", 0.8, 0.0, 100.0);
        GUARD_MOVE_SLOW = BUILDER.comment("ガード中の移動速度低下率")
                .defineInRange("guardMoveSlow", 0.35, 0.0, 1.0);
        BUILDER.pop();

        BUILDER.push("attackCost");
        COST_LIGHT = BUILDER.comment("通常斬撃 1・2段目の体幹消費").defineInRange("light", 5.0, 0.0, 1000.0);
        COST_LIGHT_FINISHER = BUILDER.comment("通常斬撃 3段目の体幹消費").defineInRange("lightFinisher", 8.0, 0.0, 1000.0);
        COST_HEAVY = BUILDER.comment("横薙ぎ（右クリック）の体幹消費").defineInRange("heavy", 12.0, 0.0, 1000.0);
        COST_FLURRY = BUILDER.comment("抜刀連撃（左長押し）の体幹消費").defineInRange("flurry", 18.0, 0.0, 1000.0);
        COST_DASH_STORM = BUILDER.comment("踏み込み乱舞（右長押し）の体幹消費").defineInRange("dashStorm", 25.0, 0.0, 1000.0);
        ATTACK_REGEN_DELAY_TICKS = BUILDER.comment("攻撃で体幹を消費してから回復が始まるまで (ticks)。被ガード時の遅延より短めにしておく")
                .defineInRange("attackRegenDelayTicks", 25, 0, 1200);
        BUILDER.pop();

        BUILDER.push("empower");
        PARRIES_TO_EMPOWER = BUILDER.comment("この回数弾くと刀が強化状態になる").defineInRange("parriesToEmpower", 3, 1, 100);
        PARRY_STACK_RESET_TICKS = BUILDER.comment("弾きの間隔がこれを超えると弾き回数がリセットされる (ticks)")
                .defineInRange("parryStackResetTicks", 300, 1, 12000);
        EMPOWER_DURATION_TICKS = BUILDER.comment("強化状態の持続時間 (ticks)。次の固有技で消費される")
                .defineInRange("empowerDurationTicks", 600, 1, 72000);
        BUILDER.pop();

        BUILDER.push("art");
        RUNE_TIMEOUT_TICKS = BUILDER.comment("ルーンが消えるまでの入力猶予 (ticks)")
                .defineInRange("runeTimeoutTicks", 40, 5, 400);
        ART_COOLDOWN_TICKS = BUILDER.comment("固有技のクールダウン (ticks)")
                .defineInRange("artCooldownTicks", 100, 0, 12000);
        BUILDER.pop();
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();
}
