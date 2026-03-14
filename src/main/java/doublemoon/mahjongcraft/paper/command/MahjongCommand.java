package doublemoon.mahjongcraft.paper.command;

import doublemoon.mahjongcraft.paper.MahjongPaperPlugin;
import doublemoon.mahjongcraft.paper.i18n.MessageService;
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
    private final MessageService messages;
    private final MahjongTableManager tableManager;

    public MahjongCommand(MahjongPaperPlugin plugin, MahjongTableManager tableManager) {
        this.messages = plugin.messages();
        this.tableManager = tableManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            this.messages.send(sender, "common.only_players");
            return true;
        }

        if (args.length == 0) {
            this.messages.send(player, "command.usage");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "create" -> {
                MahjongTableSession table = this.tableManager.createTable(player);
                table.render();
                this.messages.send(player, "command.created_table", this.messages.tag("table_id", table.id()));
            }
            case "join" -> {
                if (args.length < 2) {
                    this.messages.send(player, "command.join_usage");
                    return true;
                }
                MahjongTableSession table = this.tableManager.join(player, args[1]);
                if (table == null) {
                    this.messages.send(player, "command.join_failed");
                } else {
                    this.messages.send(player, "command.joined_table", this.messages.tag("table_id", table.id()));
                }
            }
            case "leave" -> {
                MahjongTableSession current = this.tableManager.sessionForViewer(player.getUniqueId());
                if (current == null) {
                    this.messages.send(player, "command.not_in_table");
                    return true;
                }
                MahjongTableSession table = this.tableManager.leave(player.getUniqueId());
                if (table != null) {
                    this.messages.send(player, "command.left_table");
                    return true;
                }
                MahjongTableSession spectatorTable = this.tableManager.unspectate(player.getUniqueId());
                this.messages.send(player, spectatorTable == null ? "command.leave_blocked_started" : "command.unspectated");
            }
            case "list" -> {
                if (this.tableManager.tables().isEmpty()) {
                    this.messages.send(player, "command.no_active_tables");
                    return true;
                }
                Locale locale = this.messages.resolveLocale(player);
                this.tableManager.tables().forEach(table -> player.sendMessage(this.messages.render(
                    locale,
                    "command.table_list_entry",
                    this.messages.tag("table_id", table.id()),
                    this.messages.tag("summary", table.waitingSummary()),
                    this.messages.number(locale, "x", table.center().getBlockX()),
                    this.messages.number(locale, "y", table.center().getBlockY()),
                    this.messages.number(locale, "z", table.center().getBlockZ())
                )));
            }
            case "spectate" -> {
                if (args.length < 2) {
                    this.messages.send(player, "command.spectate_usage");
                    return true;
                }
                MahjongTableSession table = this.tableManager.spectate(player, args[1]);
                if (table == null) {
                    this.messages.send(player, "command.spectate_failed");
                } else {
                    this.messages.send(player, "command.spectate_joined", this.messages.tag("table_id", table.id()));
                }
            }
            case "unspectate" -> {
                MahjongTableSession table = this.tableManager.unspectate(player.getUniqueId());
                this.messages.send(player, table == null ? "command.not_in_table" : "command.unspectated");
            }
            case "addbot" -> {
                MahjongTableSession table = requireTable(player);
                if (table == null) {
                    return true;
                }
                this.messages.send(player, table.addBot() ? "command.bot_added" : "command.bot_add_failed");
            }
            case "removebot" -> {
                MahjongTableSession table = requireTable(player);
                if (table == null) {
                    return true;
                }
                this.messages.send(player, table.removeBot() ? "command.bot_removed" : "command.bot_remove_failed");
            }
            case "rule" -> {
                MahjongTableSession table = requireTable(player);
                if (table == null) {
                    return true;
                }
                if (args.length == 1) {
                    this.messages.send(player, "command.rule_summary", this.messages.tag("summary", table.ruleSummary()));
                    return true;
                }
                if (args.length < 3) {
                    this.messages.send(player, "command.rule_usage");
                    return true;
                }
                this.messages.send(
                    player,
                    table.setRuleOption(args[1], args[2]) ? "command.rule_updated" : "command.rule_update_failed",
                    this.messages.tag("summary", table.ruleSummary())
                );
            }
            case "start" -> {
                try {
                    this.tableManager.start(player);
                    this.messages.send(player, "command.round_started");
                } catch (IllegalStateException ex) {
                    this.messages.send(player, "command.start_failed", this.messages.tag("reason", ex.getMessage()));
                }
            }
            case "state" -> {
                MahjongTableSession table = requireViewedTable(player);
                if (table == null) {
                    return true;
                }
                player.sendMessage(table.stateSummary(player));
            }
            case "riichi" -> {
                MahjongTableSession table = requireTable(player);
                if (table == null || args.length < 2) {
                    this.messages.send(player, "command.riichi_usage");
                    return true;
                }
                java.util.OptionalInt index = parseIndex(args[1]);
                this.messages.send(player, index.isPresent() && table.declareRiichi(player.getUniqueId(), index.getAsInt())
                    ? "command.riichi_success"
                    : "command.riichi_failed");
            }
            case "tsumo" -> {
                MahjongTableSession table = requireTable(player);
                if (table == null) {
                    return true;
                }
                this.messages.send(player, table.declareTsumo(player.getUniqueId()) ? "command.tsumo_success" : "command.tsumo_failed");
            }
            case "ron" -> sendReaction(player, ReactionType.RON, null, "command.action.ron");
            case "pon" -> sendReaction(player, ReactionType.PON, null, "command.action.pon");
            case "minkan" -> sendReaction(player, ReactionType.MINKAN, null, "command.action.minkan");
            case "skip" -> sendReaction(player, ReactionType.SKIP, null, "command.action.skip");
            case "chii" -> {
                if (args.length < 3) {
                    this.messages.send(player, "command.chii_usage");
                    return true;
                }
                try {
                    MahjongTile first = MahjongTile.valueOf(args[1].toUpperCase(Locale.ROOT));
                    MahjongTile second = MahjongTile.valueOf(args[2].toUpperCase(Locale.ROOT));
                    sendReaction(player, ReactionType.CHII, new Pair<>(first, second), "command.action.chii");
                } catch (IllegalArgumentException ex) {
                    this.messages.send(player, "command.invalid_tile");
                }
            }
            case "kan" -> {
                MahjongTableSession table = requireTable(player);
                if (table == null || args.length < 2) {
                    this.messages.send(player, "command.kan_usage");
                    return true;
                }
                this.messages.send(player, table.declareKan(player.getUniqueId(), args[1]) ? "command.kan_success" : "command.kan_failed");
            }
            case "kyuushu", "kyuushukyuuhai" -> {
                MahjongTableSession table = requireTable(player);
                if (table == null) {
                    return true;
                }
                this.messages.send(player, table.declareKyuushuKyuuhai(player.getUniqueId()) ? "command.kyuushu_success" : "command.kyuushu_failed");
            }
            case "settlement" -> {
                MahjongTableSession table = requireViewedTable(player);
                if (table == null) {
                    return true;
                }
                this.messages.send(player, table.openSettlementUi(player) ? "command.settlement_opened" : "command.settlement_unavailable");
            }
            case "render" -> {
                MahjongTableSession table = requireTable(player);
                if (table == null) {
                    return true;
                }
                table.render();
                this.messages.send(player, "command.rendered");
            }
            case "clear" -> {
                MahjongTableSession table = requireTable(player);
                if (table == null) {
                    return true;
                }
                table.clearDisplays();
                this.messages.send(player, "command.cleared");
            }
            default -> this.messages.send(player, "command.usage");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("create", "join", "leave", "list", "spectate", "unspectate", "addbot", "removebot", "rule", "start", "state", "riichi", "tsumo", "ron", "pon", "minkan", "chii", "kan", "skip", "kyuushu", "settlement", "render", "clear");
        }
        if (args.length == 2 && ("join".equalsIgnoreCase(args[0]) || "spectate".equalsIgnoreCase(args[0]))) {
            List<String> ids = new ArrayList<>();
            this.tableManager.tables().forEach(table -> ids.add(table.id()));
            return ids;
        }
        if (args.length == 2 && "kan".equalsIgnoreCase(args[0])) {
            return List.of("m1", "m9", "p1", "p9", "s1", "s9", "east", "south", "west", "north", "white_dragon", "green_dragon", "red_dragon", "m5_red", "p5_red", "s5_red");
        }
        if (args.length == 2 && "rule".equalsIgnoreCase(args[0]) && sender instanceof Player player) {
            MahjongTableSession table = this.tableManager.tableFor(player.getUniqueId());
            return table == null ? List.of() : table.ruleKeys();
        }
        if (args.length == 3 && "rule".equalsIgnoreCase(args[0]) && sender instanceof Player player) {
            MahjongTableSession table = this.tableManager.tableFor(player.getUniqueId());
            return table == null ? List.of() : table.ruleValues(args[1]);
        }
        if (args.length == 2 && "chii".equalsIgnoreCase(args[0])) {
            return List.of("m1", "m2", "m3", "p3", "p4", "p5", "s6", "s7", "s8");
        }
        if (args.length == 3 && "chii".equalsIgnoreCase(args[0])) {
            return List.of("m2", "m3", "m4", "p4", "p5", "p6", "s7", "s8", "s9");
        }
        return List.of();
    }

    private MahjongTableSession requireTable(Player player) {
        MahjongTableSession table = this.tableManager.tableFor(player.getUniqueId());
        if (table == null) {
            this.messages.send(player, "command.not_in_table");
        }
        return table;
    }

    private MahjongTableSession requireViewedTable(Player player) {
        MahjongTableSession table = this.tableManager.sessionForViewer(player.getUniqueId());
        if (table == null) {
            this.messages.send(player, "command.not_in_table");
        }
        return table;
    }

    private void sendReaction(Player player, ReactionType type, Pair<MahjongTile, MahjongTile> pair, String actionKey) {
        MahjongTableSession table = requireTable(player);
        if (table == null) {
            return;
        }
        boolean ok = table.react(player.getUniqueId(), new ReactionResponse(type, pair));
        Locale locale = this.messages.resolveLocale(player);
        String action = this.messages.plain(locale, actionKey);
        this.messages.send(
            player,
            ok ? "command.reaction_submitted" : "command.reaction_failed",
            this.messages.tag("action", action)
        );
    }

    private java.util.OptionalInt parseIndex(String raw) {
        try {
            return java.util.OptionalInt.of(Integer.parseInt(raw));
        } catch (NumberFormatException ex) {
            return java.util.OptionalInt.empty();
        }
    }
}
