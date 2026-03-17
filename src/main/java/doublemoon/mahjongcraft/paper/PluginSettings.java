package doublemoon.mahjongcraft.paper;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public final class PluginSettings {
    private final ConfigurationSection debugSection;
    private final ConfigurationSection databaseSection;
    private final ConfigurationSection craftEngineSection;
    private final boolean databaseFailOnError;
    private final boolean tablePersistenceEnabled;
    private final String tablePersistenceFile;
    private final int tableStartupRebuildBatchSize;
    private final boolean rankingEnabled;
    private final String rankingEastRoom;
    private final String rankingSouthRoom;

    private PluginSettings(
        ConfigurationSection debugSection,
        ConfigurationSection databaseSection,
        ConfigurationSection craftEngineSection,
        boolean databaseFailOnError,
        boolean tablePersistenceEnabled,
        String tablePersistenceFile,
        int tableStartupRebuildBatchSize,
        boolean rankingEnabled,
        String rankingEastRoom,
        String rankingSouthRoom
    ) {
        this.debugSection = debugSection;
        this.databaseSection = databaseSection;
        this.craftEngineSection = craftEngineSection;
        this.databaseFailOnError = databaseFailOnError;
        this.tablePersistenceEnabled = tablePersistenceEnabled;
        this.tablePersistenceFile = tablePersistenceFile;
        this.tableStartupRebuildBatchSize = tableStartupRebuildBatchSize;
        this.rankingEnabled = rankingEnabled;
        this.rankingEastRoom = rankingEastRoom;
        this.rankingSouthRoom = rankingSouthRoom;
    }

    public static PluginSettings from(FileConfiguration config) {
        ConfigurationSection tablesSection = ConfigAccess.firstSection(config, "tables");
        ConfigurationSection databaseSection = ConfigAccess.firstSection(config, "database");
        ConfigurationSection rankingSection = ConfigAccess.firstSection(config, "ranking");
        ConfigurationSection tablePersistenceSection = ConfigAccess.firstSection(config, "tables.persistence", "tablePersistence");
        return new PluginSettings(
            ConfigAccess.firstSection(config, "debug"),
            databaseSection,
            ConfigAccess.firstSection(config, "integrations.craftengine", "craftengine"),
            ConfigAccess.bool(databaseSection, false, "failOnError"),
            ConfigAccess.bool(tablePersistenceSection, true, "enabled"),
            ConfigAccess.string(tablePersistenceSection, "tables.yml", "file"),
            Math.max(1, ConfigAccess.integer(tablesSection, 3, "startupRebuildBatchSize", "startup-rebuild-batch-size")),
            ConfigAccess.bool(rankingSection, true, "enabled"),
            ConfigAccess.string(rankingSection, "SILVER", "eastRoom"),
            ConfigAccess.string(rankingSection, "GOLD", "southRoom")
        );
    }

    public ConfigurationSection debugSection() {
        return this.debugSection;
    }

    public ConfigurationSection databaseSection() {
        return this.databaseSection;
    }

    public ConfigurationSection craftEngineSection() {
        return this.craftEngineSection;
    }

    public boolean databaseFailOnError() {
        return this.databaseFailOnError;
    }

    public boolean tablePersistenceEnabled() {
        return this.tablePersistenceEnabled;
    }

    public String tablePersistenceFile() {
        return this.tablePersistenceFile;
    }

    public int tableStartupRebuildBatchSize() {
        return this.tableStartupRebuildBatchSize;
    }

    public boolean rankingEnabled() {
        return this.rankingEnabled;
    }

    public String rankingEastRoom() {
        return this.rankingEastRoom;
    }

    public String rankingSouthRoom() {
        return this.rankingSouthRoom;
    }
}
