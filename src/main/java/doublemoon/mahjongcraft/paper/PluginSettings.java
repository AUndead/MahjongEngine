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

    private PluginSettings(
        ConfigurationSection debugSection,
        ConfigurationSection databaseSection,
        ConfigurationSection craftEngineSection,
        boolean databaseFailOnError,
        boolean tablePersistenceEnabled,
        String tablePersistenceFile
    ) {
        this.debugSection = debugSection;
        this.databaseSection = databaseSection;
        this.craftEngineSection = craftEngineSection;
        this.databaseFailOnError = databaseFailOnError;
        this.tablePersistenceEnabled = tablePersistenceEnabled;
        this.tablePersistenceFile = tablePersistenceFile;
    }

    public static PluginSettings from(FileConfiguration config) {
        ConfigurationSection databaseSection = ConfigAccess.firstSection(config, "database");
        ConfigurationSection tablePersistenceSection = ConfigAccess.firstSection(config, "tables.persistence", "tablePersistence");
        return new PluginSettings(
            ConfigAccess.firstSection(config, "debug"),
            databaseSection,
            ConfigAccess.firstSection(config, "integrations.craftengine", "craftengine"),
            ConfigAccess.bool(databaseSection, false, "failOnError"),
            ConfigAccess.bool(tablePersistenceSection, true, "enabled"),
            ConfigAccess.string(tablePersistenceSection, "tables.yml", "file")
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
}
