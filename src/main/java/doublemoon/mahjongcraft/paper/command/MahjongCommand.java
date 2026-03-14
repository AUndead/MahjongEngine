package doublemoon.mahjongcraft.paper.command;

import doublemoon.mahjongcraft.paper.riichi.ReactionResponse;
import doublemoon.mahjongcraft.paper.riichi.ReactionType;
import doublemoon.mahjongcraft.paper.riichi.model.MahjongTile;
import doublemoon.mahjongcraft.paper.table.MahjongTableManager;
import doublemoon.mahjongcraft.paper.table.MahjongTableSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import kotlin.Pair;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class MahjongCommand implements CommandExecutor, TabCompleter {
    private final MahjongTableManager tableManager;

    public MahjongCommand(MahjongTableManager tableManager) {
        this.tableManager = tableManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("/mahjong create|join|leave|list|start|state|riichi|tsumo|ron|pon|minkan|chii|kan|skip|kyuushu|settlement|render|clear");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "create" -> {
                MahjongTableSession table = this.tableManager.createTable(player);
                table.render();
                player.sendMessage("已创建麻将桌: " + table.id());
            }
            case "join" -> {
                if (args.length < 2) {
                    player.sendMessage("用法: /mahjong join <桌号>");
                    return true;
                }
                MahjongTableSession table = this.tableManager.join(player, args[1]);
                player.sendMessage(table == null ? "加入失败，桌号不存在、已满，或你已经在别的桌里。" : "已加入麻将桌: " + table.id());
            }
            case "leave" -> {
                MahjongTableSession table = this.tableManager.leave(player.getUniqueId());
                player.sendMessage(table == null ? "你当前不在任何麻将桌中。" : "已离开麻将桌。");
            }
            case "list" -> {
                if (this.tableManager.tables().isEmpty()) {
                    player.sendMessage("当前没有活动中的麻将桌。");
                    return true;
                }
                this.tableManager.tables().forEach(table ->
                    player.sendMessage(table.id() + " - " + table.size() + "/4 @ " + table.center().getBlockX() + "," + table.center().getBlockY() + "," + table.center().getBlockZ())
                );
            }
            case "start" -> {
                try {
                    this.tableManager.start(player);
                    player.sendMessage("已发牌并渲染牌桌。点击自己的手牌出牌，或用 /mahjong state 查看当前可操作项。");
                } catch (IllegalStateException ex) {
                    player.sendMessage("开始失败: " + ex.getMessage());
                }
            }
            case "state" -> {
                MahjongTableSession table = this.tableManager.tableFor(player.getUniqueId());
                if (table == null) {
                    player.sendMessage("你当前不在任何麻将桌中。");
                    return true;
                }
                player.sendMessage(table.stateSummary(player.getUniqueId()));
            }
            case "riichi" -> {
                MahjongTableSession table = this.tableManager.tableFor(player.getUniqueId());
                if (table == null || args.length < 2) {
                    player.sendMessage("用法: /mahjong riichi <手牌索引>");
                    return true;
                }
                java.util.OptionalInt index = parseIndex(args[1]);
                boolean ok = index.isPresent() && table.declareRiichi(player.getUniqueId(), index.getAsInt());
                player.sendMessage(ok ? "已宣告立直并打出指定手牌。" : "立直失败。");
            }
            case "tsumo" -> {
                MahjongTableSession table = this.tableManager.tableFor(player.getUniqueId());
                if (table == null) {
                    player.sendMessage("你当前不在任何麻将桌中。");
                    return true;
                }
                player.sendMessage(table.declareTsumo(player.getUniqueId()) ? "自摸成立。" : "当前不能自摸。");
            }
            case "ron" -> sendReaction(player, ReactionType.RON, null, "荣和");
            case "pon" -> sendReaction(player, ReactionType.PON, null, "碰");
            case "minkan" -> sendReaction(player, ReactionType.MINKAN, null, "明杠");
            case "skip" -> sendReaction(player, ReactionType.SKIP, null, "跳过");
            case "chii" -> {
                if (args.length < 3) {
                    player.sendMessage("用法: /mahjong chii <tileA> <tileB>");
                    return true;
                }
                MahjongTile first;
                MahjongTile second;
                try {
                    first = MahjongTile.valueOf(args[1].toUpperCase(Locale.ROOT));
                    second = MahjongTile.valueOf(args[2].toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ex) {
                    player.sendMessage("牌名无效，例如 m2、m3、east、p5_red。");
                    return true;
                }
                sendReaction(player, ReactionType.CHII, new Pair<>(first, second), "吃");
            }
            case "kan" -> {
                MahjongTableSession table = this.tableManager.tableFor(player.getUniqueId());
                if (table == null || args.length < 2) {
                    player.sendMessage("用法: /mahjong kan <tile>");
                    return true;
                }
                player.sendMessage(table.declareKan(player.getUniqueId(), args[1]) ? "已执行杠。" : "当前不能这样杠。");
            }
            case "kyuushu", "kyuushukyuuhai" -> {
                MahjongTableSession table = this.tableManager.tableFor(player.getUniqueId());
                if (table == null) {
                    player.sendMessage("你当前不在任何麻将桌中。");
                    return true;
                }
                player.sendMessage(table.declareKyuushuKyuuhai(player.getUniqueId()) ? "已宣告九种九牌流局。" : "当前不能九种九牌。");
            }
            case "settlement" -> {
                MahjongTableSession table = this.tableManager.tableFor(player.getUniqueId());
                if (table == null) {
                    player.sendMessage("你当前不在任何麻将桌中。");
                    return true;
                }
                player.sendMessage(table.openSettlementUi(player) ? "已打开本局结算界面。" : "当前还没有可查看的结算。");
            }
            case "render" -> {
                MahjongTableSession table = this.tableManager.tableFor(player.getUniqueId());
                if (table == null) {
                    player.sendMessage("你当前不在任何麻将桌中。");
                    return true;
                }
                table.render();
                player.sendMessage("已重新渲染牌桌。");
            }
            case "clear" -> {
                MahjongTableSession table = this.tableManager.tableFor(player.getUniqueId());
                if (table == null) {
                    player.sendMessage("你当前不在任何麻将桌中。");
                    return true;
                }
                table.clearDisplays();
                player.sendMessage("已清理展示实体。");
            }
            default -> player.sendMessage("/mahjong create|join|leave|list|start|state|riichi|tsumo|ron|pon|minkan|chii|kan|skip|kyuushu|settlement|render|clear");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("create", "join", "leave", "list", "start", "state", "riichi", "tsumo", "ron", "pon", "minkan", "chii", "kan", "skip", "kyuushu", "settlement", "render", "clear");
        }
        if (args.length == 2 && "join".equalsIgnoreCase(args[0])) {
            List<String> ids = new ArrayList<>();
            this.tableManager.tables().forEach(table -> ids.add(table.id()));
            return ids;
        }
        if (args.length == 2 && "kan".equalsIgnoreCase(args[0])) {
            return List.of("m1", "m9", "p1", "p9", "s1", "s9", "east", "south", "west", "north", "white_dragon", "green_dragon", "red_dragon", "m5_red", "p5_red", "s5_red");
        }
        if (args.length == 2 && "chii".equalsIgnoreCase(args[0])) {
            return List.of("m1", "m2", "m3", "p3", "p4", "p5", "s6", "s7", "s8");
        }
        if (args.length == 3 && "chii".equalsIgnoreCase(args[0])) {
            return List.of("m2", "m3", "m4", "p4", "p5", "p6", "s7", "s8", "s9");
        }
        return List.of();
    }

    private void sendReaction(Player player, ReactionType type, Pair<MahjongTile, MahjongTile> pair, String actionName) {
        MahjongTableSession table = this.tableManager.tableFor(player.getUniqueId());
        if (table == null) {
            player.sendMessage("你当前不在任何麻将桌中。");
            return;
        }
        boolean ok = table.react(player.getUniqueId(), new ReactionResponse(type, pair));
        player.sendMessage(ok ? "已提交" + actionName + "。" : "当前不能" + actionName + "。");
    }

    private java.util.OptionalInt parseIndex(String raw) {
        try {
            return java.util.OptionalInt.of(Integer.parseInt(raw));
        } catch (NumberFormatException ex) {
            return java.util.OptionalInt.empty();
        }
    }
}
