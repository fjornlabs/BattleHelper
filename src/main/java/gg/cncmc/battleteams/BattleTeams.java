package gg.cncmc.battleteams;

import gg.cncmc.battleteams.commands.battleTeamsCommand;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BattleTeams implements ModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("BattleTeams");

    @Override
    public void onInitialize() {
        LOGGER.info("[BattleTeams] BattleTeams has started!");
        battleTeamsCommand.register();
    }
}
