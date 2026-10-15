package gg.tame.palladium.config.modules.opt;

import gg.tame.palladium.config.ConfigModules;
import gg.tame.palladium.config.EnumConfigCategory;

public class CoalesceMoveWaypointUpdates extends ConfigModules {

    public String getBasePath() {
        return EnumConfigCategory.PERF.getBaseKeyName();
    }

    public static boolean enabled = true;

    @Override
    public void onLoaded() {
        enabled = config.getBoolean(getBasePath() + ".coalesce-move-waypoint-updates", enabled, config.pickStringRegionBased("""
                Coalesce locator bar (waypoint) updates while a moving player is briefly snapped back to fire PlayerMoveEvent.
                Every other player then receives one update per move instead of new -> old -> new on each block crossing.
                The final waypoint state is unchanged. Set to false to restore per-step updates.""",
            """
                在为触发 PlayerMoveEvent 而临时回退玩家位置时合并定位栏 (路径点) 更新.
                其他玩家每次移动只收到一次更新, 而不是每跨越一个方块收到 新 -> 旧 -> 新 三次更新.
                最终路径点状态不变. 设为 false 可恢复逐步更新."""));
    }
}
