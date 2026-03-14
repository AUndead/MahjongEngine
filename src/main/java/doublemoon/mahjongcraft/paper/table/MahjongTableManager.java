package doublemoon.mahjongcraft.paper.table;

import doublemoon.mahjongcraft.paper.MahjongPaperPlugin;
import doublemoon.mahjongcraft.paper.ui.SettlementUi;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class MahjongTableManager implements Listener {
    private final MahjongPaperPlugin plugin;
    private final Map<String, MahjongTableSession> tables = new LinkedHashMap<>();
    private final Map<UUID, String> playerTables = new LinkedHashMap<>();

    public MahjongTableManager(MahjongPaperPlugin plugin) {
        this.plugin = plugin;
    }

    public MahjongTableSession createTable(Player owner) {
        if (this.playerTables.containsKey(owner.getUniqueId())) {
            return this.tableFor(owner.getUniqueId());
        }
        String id = nextId();
        MahjongTableSession session = new MahjongTableSession(this.plugin, id, owner.getLocation().toCenterLocation(), owner);
        this.tables.put(id, session);
        this.playerTables.put(owner.getUniqueId(), id);
        return session;
    }

    public MahjongTableSession join(Player player, String tableId) {
        MahjongTableSession session = this.tables.get(tableId.toUpperCase(Locale.ROOT));
        if (session == null) {
            return null;
        }
        if (this.playerTables.containsKey(player.getUniqueId())) {
            return this.tableFor(player.getUniqueId());
        }
        if (!session.addPlayer(player)) {
            return null;
        }
        this.playerTables.put(player.getUniqueId(), session.id());
        session.render();
        return session;
    }

    public MahjongTableSession leave(UUID playerId) {
        MahjongTableSession session = this.tableFor(playerId);
        if (session == null) {
            return null;
        }
        session.removePlayer(playerId);
        this.playerTables.remove(playerId);
        if (session.isEmpty()) {
            session.shutdown();
            this.tables.remove(session.id());
        } else {
            session.render();
        }
        return session;
    }

    public MahjongTableSession tableFor(UUID playerId) {
        String tableId = this.playerTables.get(playerId);
        return tableId == null ? null : this.tables.get(tableId);
    }

    public Collection<MahjongTableSession> tables() {
        return this.tables.values();
    }

    public void start(Player player) {
        MahjongTableSession session = this.tableFor(player.getUniqueId());
        if (session == null) {
            throw new IllegalStateException("Player is not in a table");
        }
        session.startRound();
    }

    public boolean clickTile(Player player, String tableId, UUID ownerId, int tileIndex) {
        MahjongTableSession session = this.tables.get(tableId);
        if (session == null || !session.contains(player.getUniqueId()) || !player.getUniqueId().equals(ownerId)) {
            return false;
        }
        return session.discard(ownerId, tileIndex);
    }

    public void shutdown() {
        this.tables.values().forEach(MahjongTableSession::shutdown);
        this.tables.clear();
        this.playerTables.clear();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.leave(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onSettlementClick(InventoryClickEvent event) {
        if (SettlementUi.isSettlementInventory(event.getView().getTopInventory())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSettlementDrag(InventoryDragEvent event) {
        if (SettlementUi.isSettlementInventory(event.getView().getTopInventory())) {
            event.setCancelled(true);
        }
    }

    private static String nextId() {
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder builder = new StringBuilder(6);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 6; i++) {
            builder.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return builder.toString();
    }
}
