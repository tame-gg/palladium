package gg.tame.palladium.config.modules.opt;

import gg.tame.palladium.config.ConfigModules;
import gg.tame.palladium.config.EnumConfigCategory;
import gg.tame.palladium.config.annotations.Experimental;

public class CoalesceMoveWaypointUpdates extends ConfigModules {

    public String getBasePath() {
        return EnumConfigCategory.PERF.getBaseKeyName();
    }

    @Experimental
    public static boolean enabled = false;

    @Override
    public void onLoaded() {
        enabled = config.getBoolean(getBasePath() + ".coalesce-move-waypoint-updates", enabled, config.pickStringRegionBased("""
                Coalesce locator bar (waypoint) updates while a moving player is briefly snapped back to fire PlayerMoveEvent.
                Every other player then receives one update per move instead of new -> old -> new on each block crossing.
                Experimental. The final waypoint state is unchanged; false (default) keeps per-step updates.""",
            """
                在为触发 PlayerMoveEvent 而临时回退玩家位置时合并定位栏 (路径点) 更新.
                其他玩家每次移动只收到一次更新, 而不是每跨越一个方块收到 新 -> 旧 -> 新 三次更新.
                实验性功能. 最终路径点状态不变; false (默认) 保持逐步更新."""));
    }
}
