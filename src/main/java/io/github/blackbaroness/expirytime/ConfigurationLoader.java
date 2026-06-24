package io.github.blackbaroness.expirytime;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

import java.util.LinkedHashMap;
import java.util.Map;

public class ConfigurationLoader {

    private final String numbersColorPath = "numbers_color";
    private final String numbersColorDefault = "&7";

    private final String neverPath = "never";
    private final String neverDefault = "&6∞";

    private final String daysPath = "days";
    private final String daysDefault = "&6 дн.";

    private final String hoursPath = "hours";
    private final String hoursDefault = "&6 ч.";

    private final String minutesPath = "minutes";
    private final String minutesDefault = "&6 мин.";

    private final String secondsPath = "seconds";
    private final String secondsDefault = "&6 сек.";

    private final PlaceholderExpansion expansion;

    public ConfigurationLoader(PlaceholderExpansion expansion) {
        this.expansion = expansion;
    }

    public Map<String, Object> getDefaults() {
        final Map<String, Object> map = new LinkedHashMap<>();
        map.put(numbersColorPath, numbersColorDefault);
        map.put(neverPath, neverDefault);
        map.put(daysPath, daysDefault);
        map.put(hoursPath, hoursDefault);
        map.put(minutesPath, minutesDefault);
        map.put(secondsPath, secondsDefault);
        return map;
    }

    public Configuration load() {
        return new Configuration(
                expansion.getString(numbersColorPath, numbersColorDefault),
                expansion.getString(neverPath, neverDefault),
                expansion.getString(daysPath, daysDefault),
                expansion.getString(hoursPath, hoursDefault),
                expansion.getString(minutesPath, minutesDefault),
                expansion.getString(secondsPath, secondsDefault)
        );
    }
}
