package luckytail.katanacraft.client;

import luckytail.katanacraft.network.S2CCombatStatePacket;

/** サーバーから受け取った自分の戦闘状態（HUD描画用） */
public final class ClientKatanaState {
    private ClientKatanaState() {}

    public static final int MAX_RUNES = 16;

    public static long clientTicks;

    public static float posture = 100F;
    public static float maxPosture = 100F;
    public static float displayPosture = 100F; // 表示用（なめらかに追従）
    public static long postureHitTime = -1000;  // 削られた瞬間（フラッシュ演出）
    public static boolean guarding;

    public static byte[] runes = new byte[0];
    public static final long[] runeTimes = new long[MAX_RUNES];
    public static int sequenceLength;
    public static int runeTimer;
    public static int artCooldown;
    public static int artCooldownMax = 1;

    public static int parryStacks;
    public static int parriesToEmpower = 3;
    public static int empoweredTicks;
    public static int empowerDuration = 1;
    public static long empowerStart = -1000;  // 強化された瞬間（演出用）
    public static long exhaustedTime = -1000; // 体幹不足で攻撃が出なかった瞬間

    public static boolean isEmpowered() {
        return empoweredTicks > 0;
    }

    public static byte[] flashRunes = new byte[0];
    public static long flashStart = -1000;

    public static void apply(S2CCombatStatePacket p) {
        if (p.posture < posture - 0.01F) postureHitTime = clientTicks;
        posture = p.posture;
        maxPosture = Math.max(1F, p.maxPosture);
        guarding = p.guarding;
        sequenceLength = Math.min(MAX_RUNES, p.sequenceLength);
        runeTimer = p.runeTimer;
        artCooldown = p.artCooldown;
        artCooldownMax = Math.max(1, p.artCooldownMax);
        if (p.empoweredTicks > 0 && empoweredTicks <= 0) empowerStart = clientTicks;
        parryStacks = p.parryStacks;
        parriesToEmpower = Math.max(1, p.parriesToEmpower);
        empoweredTicks = p.empoweredTicks;
        empowerDuration = Math.max(1, p.empowerDuration);

        byte[] incoming = p.runes.length > MAX_RUNES ? java.util.Arrays.copyOf(p.runes, MAX_RUNES) : p.runes;
        for (int i = 0; i < incoming.length; i++) {
            if (i >= runes.length || runes[i] != incoming[i]) runeTimes[i] = clientTicks;
        }
        if (p.artFired) {
            flashRunes = incoming.clone();
            flashStart = clientTicks;
            runes = new byte[0];
        } else {
            runes = incoming;
        }
    }

    public static void tick() {
        clientTicks++;
        if (artCooldown > 0) artCooldown--;
        if (runeTimer > 0) runeTimer--;
        if (empoweredTicks > 0) empoweredTicks--;
        displayPosture += (posture - displayPosture) * 0.35F;
    }
}
