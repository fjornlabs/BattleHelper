package gg.cncmc.battlehelper;

import gg.cncmc.battlehelper.commands.battleCommand;
import gg.cncmc.battlehelper.commands.battleTeamsCommand;
import gg.cncmc.battlehelper.events.deathEvent;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BattleHelper implements ModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("BattleHelper");

    public static boolean battleActive = false;
    public static boolean prepActive = false;

    @Override
    public void onInitialize() {
        LOGGER.info("[BattleHelper] BattleHelper has started!");
        battleTeamsCommand.register();
        deathEvent.register();
        battleCommand.register();
    }
}
