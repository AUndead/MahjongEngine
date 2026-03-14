package doublemoon.mahjongcraft.paper;

import doublemoon.mahjongcraft.paper.command.MahjongCommand;
import doublemoon.mahjongcraft.paper.packet.PacketEventsBridge;
import doublemoon.mahjongcraft.paper.render.TableDisplayRegistry;
import doublemoon.mahjongcraft.paper.table.MahjongTableManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class MahjongPaperPlugin extends JavaPlugin {
    private PacketEventsBridge packetEventsBridge;
    private MahjongTableManager tableManager;

    @Override
    public void onEnable() {
        this.tableManager = new MahjongTableManager(this);
        this.packetEventsBridge = new PacketEventsBridge(this, this.tableManager);
        this.packetEventsBridge.enable();

        PluginCommand command = this.getCommand("mahjong");
        if (command == null) {
            throw new IllegalStateException("Command 'mahjong' is not defined in plugin.yml");
        }

        MahjongCommand mahjongCommand = new MahjongCommand(this.tableManager);
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
        if (this.packetEventsBridge != null) {
            this.packetEventsBridge.disable();
        }
    }
}
