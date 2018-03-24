package rlbot.py;

import rlbot.Bot;
import rlbot.BotManager;

public abstract class PythonInterface {

    private static final BotManager botManager = new BotManager();

    public void startup() {
        botManager.start();
    }

    public void shutdown() {
        botManager.retire();
    }

    public void registerBot(final int index, final String botType) {
        botManager.registerBot(index, constructBot(index, botType));
    }

    abstract Bot constructBot(final int index, final String botType);
}
