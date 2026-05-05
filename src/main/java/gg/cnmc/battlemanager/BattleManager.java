package gg.cnmc.battlemanager;

import gg.cnmc.battlemanager.battle.BattleState;
import gg.cnmc.battlemanager.battle.ui.TimerBar;
import gg.cnmc.battlemanager.commands.AssaultCommand;
import gg.cnmc.battlemanager.commands.BattleCommand;
import gg.cnmc.battlemanager.commands.BattleTeamsCommand;
import gg.cnmc.battlemanager.events.BannerBlockEvent;
import gg.cnmc.battlemanager.events.BannerReturnEvent;
import gg.cnmc.battlemanager.events.DeathEvent;
import gg.cnmc.battlemanager.events.JoinHandler;
import gg.cnmc.battlemanager.utils.time.TimerManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BattleManager implements ModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("BattleManager");

    public static BattleState state = BattleState.IDLE;
    public static String battleMode = "none";

    public static boolean isActive() {
        return state == BattleState.ROUND_ACTIVE;
    }

    @Override
    public void onInitialize() {
        BattleTeamsCommand.register();
        BattleCommand.register();
        AssaultCommand.register();
        DeathEvent.register();
        BannerBlockEvent.register();
        BannerReturnEvent.register();
        JoinHandler.register();

        LOGGER.info("[BattleManager] BattleManager has started!");

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            TimerManager.tick();
            TimerBar.tick(server);
        });
    }
}