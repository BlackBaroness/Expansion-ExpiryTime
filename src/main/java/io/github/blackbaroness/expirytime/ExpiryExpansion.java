package io.github.blackbaroness.expirytime;

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
        final RenderContext context = new RenderContext(duration);

        render(context, ChronoUnit.DAYS, config.days());
        render(context, ChronoUnit.HOURS, config.hours());
        render(context, ChronoUnit.MINUTES, config.minutes());
        render(context, ChronoUnit.SECONDS, config.seconds());

        return context.builder.toString();
    }

    // inspired by: https://github.com/BlackBaroness/duration-serializer-java/blob/9e31e75fd0d91516c03aa560536b368894f8d885/src/main/java/io/github/blackbaroness/durationserializer/DurationSerializer.java#L13
    private void render(RenderContext context, TemporalUnit unit, String label) {
        if (context.renderCount == config.unitsToShow()) {
            return;
        }

        final long value = context.duration.dividedBy(unit.getDuration());
        if (value < 1) {
            return;
        }

        if (context.renderCount > 0) {
            context.builder.append(" ");
        }

        context.builder.append(value).append(label);
        context.duration = context.duration.minus(value, unit);
        context.renderCount += 1;
    }

    private class RenderContext {
        private final StringBuilder builder = new StringBuilder(config.numbersColor());
        private int renderCount = 0;
        private Duration duration;

        private RenderContext(Duration duration) {
            this.duration = duration;
        }
    }
}
