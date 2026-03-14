package doublemoon.mahjongcraft.paper.ui;

import doublemoon.mahjongcraft.paper.model.MahjongTile;
import doublemoon.mahjongcraft.paper.riichi.RoundResolution;
import doublemoon.mahjongcraft.paper.riichi.model.RankedScoreItem;
import doublemoon.mahjongcraft.paper.riichi.model.ScoreSettlement;
import doublemoon.mahjongcraft.paper.riichi.model.YakuSettlement;
import doublemoon.mahjongcraft.paper.table.MahjongTableSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class SettlementUi {
    private static final int INVENTORY_SIZE = 54;

    private SettlementUi() {
    }

    public static boolean isSettlementInventory(Inventory inventory) {
        return inventory.getHolder() instanceof SettlementHolder;
    }

    public static void open(Player player, MahjongTableSession session) {
        RoundResolution resolution = session.lastResolution();
        if (resolution == null) {
            return;
        }
        player.openInventory(createInventory(session, resolution));
    }

    private static Inventory createInventory(MahjongTableSession session, RoundResolution resolution) {
        SettlementHolder holder = new SettlementHolder(session.id());
        Inventory inventory = Bukkit.createInventory(holder, INVENTORY_SIZE, Component.text("结算 - " + resolution.getTitle()));
        holder.bind(inventory);
        fillBackground(inventory);
        inventory.setItem(4, summaryItem(session, resolution));
        placeIndicatorRow(inventory, 0, "宝牌指示牌", session.doraIndicators());
        placeIndicatorRow(inventory, 9, "里宝牌指示牌", session.uraDoraIndicators());
        placeScoreItems(inventory, resolution.getScoreSettlement());
        placeSettlementDetails(inventory, resolution);
        return inventory;
    }

    private static void fillBackground(Inventory inventory) {
        ItemStack filler = namedItem(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler.clone());
        }
    }

    private static ItemStack summaryItem(MahjongTableSession session, RoundResolution resolution) {
        List<String> lore = new ArrayList<>();
        lore.add("局况: " + session.roundDisplay());
        lore.add("当前庄家: " + session.dealerName());
        if (resolution.getDraw() != null) {
            lore.add("流局类型: " + resolution.getDraw().name());
        }
        if (resolution.getScoreSettlement() != null) {
            lore.add("本次结算玩家数: " + resolution.getScoreSettlement().getScoreList().size());
        }
        return namedItem(Material.NETHER_STAR, "结算结果: " + resolution.getTitle(), lore);
    }

    private static void placeIndicatorRow(Inventory inventory, int startSlot, String label, List<MahjongTile> tiles) {
        for (int i = 0; i < 5; i++) {
            if (i < tiles.size()) {
                inventory.setItem(startSlot + i, tileItem(tiles.get(i), false, label));
            } else {
                inventory.setItem(startSlot + i, namedItem(Material.GRAY_STAINED_GLASS_PANE, label, List.of("本局没有更多指示牌")));
            }
        }
    }

    private static void placeScoreItems(Inventory inventory, ScoreSettlement settlement) {
        if (settlement == null) {
            inventory.setItem(22, namedItem(Material.PAPER, "无点数变动", List.of("本次为展示性结算")));
            return;
        }
        int[] slots = {18, 20, 24, 26};
        List<RankedScoreItem> ranked = settlement.getRankedScoreList();
        for (int i = 0; i < ranked.size() && i < slots.length; i++) {
            RankedScoreItem item = ranked.get(i);
            List<String> lore = new ArrayList<>();
            lore.add("原点数: " + item.getScoreItem().getScoreOrigin());
            lore.add("变动: " + (item.getScoreChangeText().isBlank() ? "0" : item.getScoreChangeText()));
            lore.add("现点数: " + item.getScoreTotal());
            if (!item.getRankFloatText().isBlank()) {
                lore.add("名次变化: " + item.getRankFloatText());
            }
            inventory.setItem(slots[i], namedItem(Material.NAME_TAG, playerName(item.getScoreItem().getStringUUID()), lore));
        }
    }

    private static void placeSettlementDetails(Inventory inventory, RoundResolution resolution) {
        if (resolution.getYakuSettlements().isEmpty()) {
            inventory.setItem(40, namedItem(Material.BOOK, "本局无和牌者", List.of("请查看上方点数区与流局结果")));
            return;
        }

        int detailSlot = 36;
        for (YakuSettlement settlement : resolution.getYakuSettlements()) {
            if (detailSlot >= INVENTORY_SIZE) {
                break;
            }
            inventory.setItem(detailSlot, namedItem(Material.ENCHANTED_BOOK, settlement.getDisplayName(), settlementLore(settlement)));
            if (detailSlot + 1 < INVENTORY_SIZE) {
                inventory.setItem(detailSlot + 1, tileItem(toDisplayTile(settlement.getWinningTile().name()), false, "和了牌"));
            }
            detailSlot += 3;
        }
    }

    private static List<String> settlementLore(YakuSettlement settlement) {
        List<String> lore = new ArrayList<>();
        lore.add("符番: " + settlement.getFu() + "符 " + settlement.getHan() + "番");
        lore.add("得点: " + settlement.getScore());
        lore.add("立直: " + (settlement.getRiichi() ? "是" : "否"));
        lore.add("和牌: " + settlement.getWinningTile().name().toLowerCase(Locale.ROOT));
        lore.add("手牌: " + joinTiles(settlement.getHands()));
        if (!settlement.getFuuroList().isEmpty()) {
            List<String> melds = new ArrayList<>();
            settlement.getFuuroList().forEach(pair -> melds.add((pair.getFirst() ? "明" : "暗") + ":" + joinTiles(pair.getSecond())));
            lore.add("副露: " + String.join(" | ", melds));
        }
        if (!settlement.getYakuList().isEmpty()) {
            settlement.getYakuList().forEach(yaku -> lore.add("役: " + yaku.name()));
        }
        if (!settlement.getYakumanList().isEmpty()) {
            settlement.getYakumanList().forEach(yaku -> lore.add("役满: " + yaku.name()));
        }
        if (!settlement.getDoubleYakumanList().isEmpty()) {
            settlement.getDoubleYakumanList().forEach(yaku -> lore.add("双倍役满: " + yaku.name()));
        }
        if (settlement.getRedFiveCount() > 0) {
            lore.add("赤宝牌: " + settlement.getRedFiveCount());
        }
        if (!settlement.getDoraIndicators().isEmpty()) {
            lore.add("宝牌指示: " + joinTiles(settlement.getDoraIndicators()));
        }
        if (!settlement.getUraDoraIndicators().isEmpty()) {
            lore.add("里宝牌指示: " + joinTiles(settlement.getUraDoraIndicators()));
        }
        return lore;
    }

    private static String joinTiles(List<?> tiles) {
        List<String> names = new ArrayList<>();
        for (Object tile : tiles) {
            names.add(tile.toString().toLowerCase(Locale.ROOT));
        }
        return String.join(" ", names);
    }

    private static String playerName(String stringUuid) {
        try {
            OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(stringUuid));
            return player.getName() == null ? stringUuid : player.getName();
        } catch (IllegalArgumentException ex) {
            return stringUuid;
        }
    }

    private static ItemStack tileItem(MahjongTile tile, boolean faceDown, String name) {
        String path = faceDown ? "mahjong_tile/back" : tile.itemModelPath();
        ItemStack itemStack = new ItemStack(Material.PAPER);
        ItemMeta meta = itemStack.getItemMeta();
        meta.setItemModel(new NamespacedKey("mahjongcraft", path));
        meta.displayName(Component.text(name, NamedTextColor.GOLD));
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    private static MahjongTile toDisplayTile(String name) {
        return MahjongTile.valueOf(name);
    }

    private static ItemStack namedItem(Material material, String name, List<String> loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.YELLOW));
        if (!loreLines.isEmpty()) {
            List<Component> lore = new ArrayList<>();
            loreLines.forEach(line -> lore.add(Component.text(line, NamedTextColor.GRAY)));
            meta.lore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }

    private static final class SettlementHolder implements InventoryHolder {
        private final String tableId;
        private Inventory inventory;

        private SettlementHolder(String tableId) {
            this.tableId = tableId;
        }

        private void bind(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return this.inventory;
        }

        @SuppressWarnings("unused")
        public String tableId() {
            return this.tableId;
        }
    }
}
