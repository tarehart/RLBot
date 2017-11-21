package tarehart.rlbot.tuning;

import java.util.HashMap;
import java.util.Map;

public class BotLog {

    private static Map<Integer, StringBuilder> logMap = new HashMap<>();
    private static String timeStamp = "";

    public static void println(String message, int playerIndex) {

        getLog(playerIndex).append(timeStamp + message).append("\n");
        System.out.println(message);
    }

    private static StringBuilder getLog(int playerIndex) {
        if (!logMap.containsKey(playerIndex)) {
            logMap.put(playerIndex, new StringBuilder());
        }
        return logMap.get(playerIndex);
    }

    public static void setTimeStamp(long time) {
        timeStamp =  time > 0 ? "(" + ((int)time / 60) + ":" + ((int)time%60) + ")" : "";
    }

    public static String collect(int playerIndex) {
        StringBuilder log = getLog(playerIndex);
        String contents = log.toString();
        log.setLength(0);
        return contents;
    }
}
