package net.modclaim.asc.core.compatibility;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.modclaim.asc.api.ASCPlugin;
import net.modclaim.asc.api.mobcap.LoadBudget;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PlaceholderAPI expansion exposing ASC performance metrics to external scoreboards and tab lists.
 */
public final class ASCPlaceholderExpansion extends PlaceholderExpansion {

    private final ASCPlugin plugin;

    public ASCPlaceholderExpansion(@NotNull ASCPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "asc";
    }

    @Override
    @NotNull
    public String getAuthor() {
        return "ModClaim";
    }

    @Override
    @NotNull
    public String getVersion() {
        return plugin.getCoreVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        LoadBudget budget = plugin.getMobCapService().getCurrentBudget();

        switch (params.toLowerCase()) {
            case "tps":
                return String.format("%.2f", budget.getTps());
            case "mspt":
                return String.format("%.2f", budget.getMspt());
            case "budget":
            case "load_budget":
                return String.format("%.0f%%", budget.getBudgetMultiplier() * 100.0);
            case "budget_multiplier":
                return String.format("%.2f", budget.getBudgetMultiplier());
            case "mode":
                return plugin.getCompatibilityService().getMode().name();
            case "hibernating_chunks":
                return String.valueOf(plugin.getLazySimService().getTrackedHibernatingCount());
            default:
                return null;
        }
    }
}
