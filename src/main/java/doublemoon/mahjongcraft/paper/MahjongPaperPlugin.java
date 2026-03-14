package doublemoon.mahjongcraft.paper;

import doublemoon.mahjongcraft.paper.command.MahjongCommand;
import doublemoon.mahjongcraft.paper.db.DatabaseService;
import doublemoon.mahjongcraft.paper.i18n.MessageService;
import doublemoon.mahjongcraft.paper.packet.PacketEventsBridge;
import doublemoon.mahjongcraft.paper.render.TableDisplayRegistry;
import doublemoon.mahjongcraft.paper.render.DisplayVisibilityRegistry;
import doublemoon.mahjongcraft.paper.table.MahjongTableManager;
import java.sql.SQLException;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class MahjongPaperPlugin extends JavaPlugin {
    private final MessageService messages = new MessageService();
    private DatabaseService database;
    private PacketEventsBridge packetEventsBridge;
    private MahjongTableManager tableManager;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        if (DatabaseService.isEnabled(this.getConfig().getConfigurationSection("database"))) {
            try {
                this.database = new DatabaseService(this, this.getConfig().getConfigurationSection("database"));
                this.getLogger().info("Database enabled: " + this.database.databaseType());
            } catch (SQLException ex) {
                throw new IllegalStateException("Failed to initialize database", ex);
            }
        }

        this.tableManager = new MahjongTableManager(this);
        this.packetEventsBridge = new PacketEventsBridge(this, this.tableManager);
        this.packetEventsBridge.enable();

        PluginCommand command = this.getCommand("mahjong");
        if (command == null) {
            throw new IllegalStateException("Command 'mahjong' is not defined in plugin.yml");
        }

        MahjongCommand mahjongCommand = new MahjongCommand(this, this.tableManager);
        command.setExecutor(mahjongCommand);
        command.setTabCompleter(mahjongCommand);

        this.getServer().getPluginManager().registerEvents(this.tableManager, this);
        this.getLogger().info("MahjongPaper enabled.");
    }

    @Override
    public void onDisable() {
        if (this.tableManager != null) {
            this.tableManager.shutdown();
        }
        TableDisplayRegistry.clear();
        DisplayVisibilityRegistry.clear();
        if (this.packetEventsBridge != null) {
            this.packetEventsBridge.disable();
        }
        if (this.database != null) {
            this.database.close();
            this.database = null;
        }
    }

    public MessageService messages() {
        return this.messages;
    }

    public DatabaseService database() {
        return this.database;
    }
}
