package daedal.expirytime;

import me.clip.placeholderapi.expansion.Configurable;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.platform.PlayerAdapter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Map;

public class ExpiryExpansion extends PlaceholderExpansion implements Configurable {

    private final long infiniteSeconds = ChronoUnit.FOREVER.getDuration().getSeconds();
    private final PlayerAdapter<Player> luckPermsAdapter;

    private ConfigurationLoader loader;
    private Configuration config;

    public ExpiryExpansion() {
        luckPermsAdapter = LuckPermsProvider.get().getPlayerAdapter(Player.class);
        loader = new ConfigurationLoader(this);
    }

    @Override
    public Map<String, Object> getDefaults() {
        return loader.getDefaults();
    }

    @Override
    public boolean register() {
        if (super.register()) {
            // we can throw away a loader after using it, it will never get used in this instance ever again
            config = loader.load();
            loader = null;
            return true;
        }

        return false;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "expirytime";
    }

    @Override
    public @NotNull String getAuthor() {
        return "BlackBaroness";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String paramsRaw) {
        if (player == null) return null;

        final String[] params = paramsRaw.split("_");
        final User user = luckPermsAdapter.getUser(player);

        final String mode = params[0];
        final String permission = switch (mode) {
            case "auto" -> "group." + user.getPrimaryGroup();
            case "group" -> "group." + params[1];
            case "permission" -> params[1];
            default -> null;
        };
        if (permission == null) {
            return "Unknown mode '" + mode + "'";
        }

        long secondsLeft = 0L;
        for (final Node node : user.getNodes()) {
            if (!node.getKey().equals(permission)) continue;

            final Duration duration = node.getExpiryDuration();
            if (duration == null) {
                secondsLeft = infiniteSeconds;
                break;
            }

            secondsLeft = Math.max(secondsLeft, duration.getSeconds());
        }

        if (secondsLeft == 0L) {
            return null;
        }

        if (secondsLeft >= infiniteSeconds) {
            return config.never();
        }

        return format(Duration.ofSeconds(secondsLeft));
    }

    private String format(Duration duration) {
        final StringBuilder builder = new StringBuilder();

        duration = append(builder, duration, ChronoUnit.DAYS, config.days());
        duration = append(builder, duration, ChronoUnit.HOURS, config.hours());
        duration = append(builder, duration, ChronoUnit.MINUTES, config.minutes());
        append(builder, duration, ChronoUnit.SECONDS, config.seconds());

        return builder.toString();
    }

    // inspired by: https://github.com/BlackBaroness/duration-serializer-java/blob/9e31e75fd0d91516c03aa560536b368894f8d885/src/main/java/io/github/blackbaroness/durationserializer/DurationSerializer.java#L13
    private Duration append(StringBuilder builder, Duration current, TemporalUnit unit, String label) {
        if (!builder.isEmpty()) { // a subject to change, that's why we use StringBuilder
            return current;
        }

        final long value = current.dividedBy(unit.getDuration());
        if (value < 1) {
            return current;
        }

        builder.append(value).append(" ").append(label);
        return current.minus(value, unit);
    }
}
