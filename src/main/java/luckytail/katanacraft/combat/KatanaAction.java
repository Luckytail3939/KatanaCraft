package luckytail.katanacraft.combat;

/**
 * 刀のモーション一覧。
 * duration はモーション全体の長さ (ticks)。0 のものは「リアクション」で、
 * サーバーではモーションとして走らせず、アニメーション通知だけに使う。
 *
 * ゆくゆく Player Animation Library を入れるときは、この enum 名と
 * assets/katanacraft/player_animation/*.json のアニメ名を 1:1 で対応させる想定。
 */
public enum KatanaAction {
    LIGHT_1(9),      // 抜刀 → 右上から左下へ袈裟斬り
    LIGHT_2(9),      // 左下から右上へ斬り上げ
    LIGHT_3(16),     // 両手持ちで唐竹割り → 納刀
    FLURRY(34),      // 抜刀 → 連撃 → 納刀（左長押し）
    HEAVY(26),       // タメ → 横一文字の斬り払い（右クリック）
    DASH_STORM(34),  // 前方ダッシュ → 広範囲連続斬り（右長押し）
    ART(22),         // 刀固有技（長さは刀ごとに上書き可）

    PARRY(0),        // 弾き成功
    GUARD_IMPACT(0), // 通常ガード
    GUARD_BROKEN(0), // ガード崩し
    EXHAUSTED(0),    // 体幹不足で攻撃できなかった
    EMPOWERED(0);    // 弾きで刀が強化された

    public final int duration;

    KatanaAction(int duration) {
        this.duration = duration;
    }

    public boolean isReaction() {
        return duration == 0;
    }

    public static KatanaAction byId(int id) {
        KatanaAction[] v = values();
        return id >= 0 && id < v.length ? v[id] : LIGHT_1;
    }
}
