package doublemoon.mahjongcraft.paper.table;

import doublemoon.mahjongcraft.paper.MahjongPaperPlugin;
import doublemoon.mahjongcraft.paper.model.SeatWind;
import doublemoon.mahjongcraft.paper.render.MeldView;
import doublemoon.mahjongcraft.paper.render.TableDisplayRegistry;
import doublemoon.mahjongcraft.paper.render.TableRenderer;
import doublemoon.mahjongcraft.paper.riichi.ReactionOptions;
import doublemoon.mahjongcraft.paper.riichi.ReactionResponse;
import doublemoon.mahjongcraft.paper.riichi.RiichiPlayerState;
import doublemoon.mahjongcraft.paper.riichi.RiichiRoundEngine;
import doublemoon.mahjongcraft.paper.riichi.model.MahjongRule;
import doublemoon.mahjongcraft.paper.ui.SettlementUi;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import kotlin.Pair;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.mahjong4j.hands.Kantsu;

public final class MahjongTableSession {
    private final MahjongPaperPlugin plugin;
    private final String id;
    private final Location center;
    private final List<UUID> seats = new ArrayList<>(4);
    private final List<Entity> displayEntities = new ArrayList<>();
    private final TableRenderer renderer = new TableRenderer();
    private final Map<UUID, String> feedbackState = new HashMap<>();
    private RiichiRoundEngine engine;
    private String lastSettlementFingerprint = "";

    public MahjongTableSession(MahjongPaperPlugin plugin, String id, Location center, Player owner) {
        this.plugin = plugin;
        this.id = id;
        this.center = center.clone();
        this.addPlayer(owner);
    }

    public MahjongPaperPlugin plugin() {
        return this.plugin;
    }

    public String id() {
        return this.id;
    }

    public Location center() {
        return this.center.clone();
    }

    public boolean addPlayer(Player player) {
        if (this.seats.contains(player.getUniqueId()) || this.seats.size() >= 4) {
            return false;
        }
        this.seats.add(player.getUniqueId());
        return true;
    }

    public boolean removePlayer(UUID playerId) {
        if (this.engine != null && this.engine.getStarted()) {
            return false;
        }
        this.feedbackState.remove(playerId);
        return this.seats.remove(playerId);
    }

    public boolean contains(UUID playerId) {
        return this.seats.contains(playerId);
    }

    public boolean isEmpty() {
        return this.seats.isEmpty();
    }

    public int size() {
        return this.seats.size();
    }

    public List<UUID> players() {
        return List.copyOf(this.seats);
    }

    public UUID owner() {
        return this.seats.getFirst();
    }

    public void startRound() {
        if (this.seats.size() != 4) {
            throw new IllegalStateException("A riichi table needs exactly 4 players");
        }

        if (this.engine == null || this.engine.getGameFinished()) {
            List<RiichiPlayerState> players = new ArrayList<>();
            for (UUID playerId : this.seats) {
                Player player = Bukkit.getPlayer(playerId);
                players.add(new RiichiPlayerState(player == null ? playerId.toString() : player.getName(), playerId.toString(), true));
            }
            this.engine = new RiichiRoundEngine(players, new MahjongRule());
        }
        this.engine.startRound();
        this.lastSettlementFingerprint = "";
        this.render();
    }

    public boolean discard(UUID playerId, int tileIndex) {
        if (this.engine == null) {
            return false;
        }
        boolean result = this.engine.discard(playerId.toString(), tileIndex);
        this.render();
        return result;
    }

    public boolean declareRiichi(UUID playerId, int tileIndex) {
        if (this.engine == null) {
            return false;
        }
        boolean result = this.engine.declareRiichi(playerId.toString(), tileIndex);
        this.render();
        return result;
    }

    public boolean declareTsumo(UUID playerId) {
        if (this.engine == null) {
            return false;
        }
        boolean result = this.engine.tryTsumo(playerId.toString());
        this.render();
        return result;
    }

    public boolean declareKyuushuKyuuhai(UUID playerId) {
        if (this.engine == null) {
            return false;
        }
        boolean result = this.engine.declareKyuushuKyuuhai(playerId.toString());
        this.render();
        return result;
    }

    public boolean react(UUID playerId, ReactionResponse response) {
        if (this.engine == null) {
            return false;
        }
        boolean result = this.engine.react(playerId.toString(), response);
        this.render();
        return result;
    }

    public boolean declareKan(UUID playerId, String tileName) {
        if (this.engine == null) {
            return false;
        }
        try {
            doublemoon.mahjongcraft.paper.riichi.model.MahjongTile tile =
                doublemoon.mahjongcraft.paper.riichi.model.MahjongTile.valueOf(tileName.toUpperCase(Locale.ROOT));
            boolean result = this.engine.tryAnkanOrKakan(playerId.toString(), tile);
            this.render();
            return result;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public void render() {
        this.clearDisplays();
        this.displayEntities.addAll(this.renderer.render(this));
        this.syncPlayerFeedback();
    }

    public void clearDisplays() {
        for (Entity entity : this.displayEntities) {
            TableDisplayRegistry.unregister(entity.getEntityId());
            if (!entity.isDead()) {
                entity.remove();
            }
        }
        this.displayEntities.clear();
    }

    public void shutdown() {
        this.clearDisplays();
        this.feedbackState.clear();
        this.lastSettlementFingerprint = "";
        this.engine = null;
    }

    public SeatWind currentSeat() {
        if (this.engine == null || !this.engine.getStarted()) {
            return SeatWind.EAST;
        }
        return SeatWind.fromIndex(this.engine.getCurrentPlayerIndex());
    }

    public UUID playerAt(SeatWind wind) {
        if (this.engine != null && !this.engine.getSeats().isEmpty()) {
            return UUID.fromString(this.engine.getSeats().get(wind.index()).getUuid());
        }
        return wind.index() < this.seats.size() ? this.seats.get(wind.index()) : null;
    }

    public String playerName(SeatWind wind) {
        UUID playerId = this.playerAt(wind);
        if (playerId == null) {
            return "空位";
        }
        Player player = Bukkit.getPlayer(playerId);
        return player == null ? "离线" : player.getName();
    }

    public List<doublemoon.mahjongcraft.paper.model.MahjongTile> hand(UUID playerId) {
        RiichiPlayerState player = this.engine == null ? null : this.engine.seatPlayer(playerId.toString());
        if (player == null) {
            return List.of();
        }
        List<doublemoon.mahjongcraft.paper.model.MahjongTile> tiles = new ArrayList<>();
        player.getHands().forEach(tile -> tiles.add(doublemoon.mahjongcraft.paper.model.MahjongTile.valueOf(tile.getMahjongTile().name())));
        return tiles;
    }

    public List<doublemoon.mahjongcraft.paper.model.MahjongTile> discards(UUID playerId) {
        RiichiPlayerState player = this.engine == null ? null : this.engine.seatPlayer(playerId.toString());
        if (player == null) {
            return List.of();
        }
        List<doublemoon.mahjongcraft.paper.model.MahjongTile> tiles = new ArrayList<>();
        player.getDiscardedTilesForDisplay().forEach(tile -> tiles.add(doublemoon.mahjongcraft.paper.model.MahjongTile.valueOf(tile.getMahjongTile().name())));
        return tiles;
    }

    public List<doublemoon.mahjongcraft.paper.model.MahjongTile> remainingWall() {
        if (this.engine == null) {
            return List.of();
        }
        List<doublemoon.mahjongcraft.paper.model.MahjongTile> tiles = new ArrayList<>();
        this.engine.getWall().forEach(tile -> tiles.add(doublemoon.mahjongcraft.paper.model.MahjongTile.UNKNOWN));
        return tiles;
    }

    public List<MeldView> fuuro(UUID playerId) {
        RiichiPlayerState player = this.engine == null ? null : this.engine.seatPlayer(playerId.toString());
        if (player == null) {
            return List.of();
        }
        List<MeldView> melds = new ArrayList<>();
        player.getFuuroList().forEach(fuuro -> {
            List<doublemoon.mahjongcraft.paper.model.MahjongTile> tiles = new ArrayList<>();
            List<Boolean> faceDownFlags = new ArrayList<>();
            boolean concealedKan = fuuro.getMentsu() instanceof Kantsu && !fuuro.getMentsu().isOpen();
            for (int i = 0; i < fuuro.getTileInstances().size(); i++) {
                tiles.add(doublemoon.mahjongcraft.paper.model.MahjongTile.valueOf(fuuro.getTileInstances().get(i).getMahjongTile().name()));
                faceDownFlags.add(concealedKan && (i == 0 || i == fuuro.getTileInstances().size() - 1));
            }
            melds.add(new MeldView(List.copyOf(tiles), List.copyOf(faceDownFlags)));
        });
        return List.copyOf(melds);
    }

    public Player onlinePlayer(UUID playerId) {
        return Bukkit.getPlayer(playerId);
    }

    public String roundDisplay() {
        if (this.engine == null) {
            return "未开局";
        }
        return this.engine.getRound().getWind().name() + (this.engine.getRound().getRound() + 1) + "局 " + this.engine.getRound().getHonba() + "本场";
    }

    public String dealerName() {
        if (this.engine == null) {
            return "未知";
        }
        String uuid = this.engine.getDealer().getUuid();
        try {
            Player player = Bukkit.getPlayer(UUID.fromString(uuid));
            return player == null ? this.engine.getDealer().getDisplayName() : player.getName();
        } catch (IllegalArgumentException ex) {
            return this.engine.getDealer().getDisplayName();
        }
    }

    public List<doublemoon.mahjongcraft.paper.model.MahjongTile> doraIndicators() {
        if (this.engine == null) {
            return List.of();
        }
        List<doublemoon.mahjongcraft.paper.model.MahjongTile> tiles = new ArrayList<>();
        this.engine.getDoraIndicators().forEach(tile -> tiles.add(doublemoon.mahjongcraft.paper.model.MahjongTile.valueOf(tile.getMahjongTile().name())));
        return tiles;
    }

    public List<doublemoon.mahjongcraft.paper.model.MahjongTile> uraDoraIndicators() {
        if (this.engine == null) {
            return List.of();
        }
        List<doublemoon.mahjongcraft.paper.model.MahjongTile> tiles = new ArrayList<>();
        this.engine.getUraDoraIndicators().forEach(tile -> tiles.add(doublemoon.mahjongcraft.paper.model.MahjongTile.valueOf(tile.getMahjongTile().name())));
        return tiles;
    }

    public doublemoon.mahjongcraft.paper.riichi.RoundResolution lastResolution() {
        return this.engine == null ? null : this.engine.getLastResolution();
    }

    public boolean openSettlementUi(Player player) {
        if (this.lastResolution() == null) {
            return false;
        }
        SettlementUi.open(player, this);
        return true;
    }

    public String stateSummary(UUID playerId) {
        if (this.engine == null) {
            return "桌局尚未开始。";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("局况 ").append(this.engine.getRound().getWind().name()).append(this.engine.getRound().getRound() + 1);
        builder.append(" 本场").append(this.engine.getRound().getHonba());
        builder.append(" | 当前玩家: ").append(this.engine.getCurrentPlayer().getDisplayName());
        builder.append(" | 牌山: ").append(this.engine.getWall().size());
        if (this.engine.getPendingReaction() != null) {
            ReactionOptions options = this.engine.availableReactions(playerId.toString());
            if (options != null) {
                builder.append(" | 可操作:");
                if (options.getCanRon()) builder.append(" ron");
                if (options.getCanPon()) builder.append(" pon");
                if (options.getCanMinkan()) builder.append(" minkan");
                if (!options.getChiiPairs().isEmpty()) builder.append(" chii");
            }
        }
        if (this.engine.getLastResolution() != null) {
            builder.append(" | 结算: ").append(this.engine.getLastResolution().getTitle());
        }
        if (this.engine.getGameFinished()) {
            builder.append(" | 对局已结束，可重新建桌。");
        }
        return builder.toString();
    }

    public boolean isStarted() {
        return this.engine != null && this.engine.getStarted();
    }

    private void syncPlayerFeedback() {
        if (this.engine == null) {
            this.feedbackState.clear();
            this.lastSettlementFingerprint = "";
            return;
        }

        String settlementFingerprint = Objects.toString(this.engine.getLastResolution(), "");
        if (!settlementFingerprint.isBlank() && !settlementFingerprint.equals(this.lastSettlementFingerprint)) {
            for (UUID playerId : this.seats) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null) {
                    SettlementUi.open(player, this);
                }
            }
        }
        this.lastSettlementFingerprint = settlementFingerprint;

        for (UUID playerId : this.seats) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null) {
                continue;
            }
            String signature = this.feedbackSignature(playerId);
            String previous = this.feedbackState.put(playerId, signature);
            if (Objects.equals(signature, previous)) {
                continue;
            }
            if (signature.isBlank()) {
                player.sendActionBar(Component.empty());
                continue;
            }
            this.sendFeedback(player, playerId);
        }
    }

    private String feedbackSignature(UUID playerId) {
        ReactionOptions options = this.engine.availableReactions(playerId.toString());
        if (options != null && this.engine.getPendingReaction() != null) {
            return "reaction:" + this.engine.getPendingReaction().getTile().getId() + ":" + options;
        }
        if (this.engine.getStarted() && this.engine.getCurrentPlayer().getUuid().equals(playerId.toString())) {
            return "turn:" + this.engine.getWall().size() + ":" + this.engine.getCurrentPlayer().isRiichiable()
                + ":" + this.engine.getCurrentPlayer().getCanAnkan() + ":" + this.engine.getCurrentPlayer().getCanKakan()
                + ":" + this.engine.canKyuushuKyuuhai(playerId.toString());
        }
        return "";
    }

    private void sendFeedback(Player player, UUID playerId) {
        ReactionOptions options = this.engine.availableReactions(playerId.toString());
        if (options != null) {
            player.sendActionBar(Component.text("有副露/荣和机会，请查看聊天栏。", NamedTextColor.GOLD));
            player.sendMessage(this.reactionPrompt(options));
            return;
        }

        if (this.engine.getStarted() && this.engine.getCurrentPlayer().getUuid().equals(playerId.toString())) {
            List<String> actions = new ArrayList<>();
            if (this.engine.getCurrentPlayer().isRiichiable()) {
                actions.add("/mahjong riichi <index>");
            }
            if (this.engine.getCurrentPlayer().getCanAnkan() || this.engine.getCurrentPlayer().getCanKakan()) {
                actions.add("/mahjong kan <tile>");
            }
            if (this.engine.canKyuushuKyuuhai(playerId.toString())) {
                actions.add("/mahjong kyuushu");
            }
            actions.add("/mahjong tsumo");
            String suffix = actions.isEmpty() ? "" : " | " + String.join(" ", actions);
            player.sendActionBar(Component.text("轮到你了，点击自己的手牌打出。" + suffix, NamedTextColor.AQUA));
        }
    }

    private Component reactionPrompt(ReactionOptions options) {
        Component message = Component.text("可执行操作: ", NamedTextColor.GOLD);
        if (options.getCanRon()) {
            message = message.append(this.actionButton("荣和", "/mahjong ron", NamedTextColor.RED)).append(Component.space());
        }
        if (options.getCanPon()) {
            message = message.append(this.actionButton("碰", "/mahjong pon", NamedTextColor.YELLOW)).append(Component.space());
        }
        if (options.getCanMinkan()) {
            message = message.append(this.actionButton("明杠", "/mahjong minkan", NamedTextColor.DARK_AQUA)).append(Component.space());
        }
        for (Pair<doublemoon.mahjongcraft.paper.riichi.model.MahjongTile, doublemoon.mahjongcraft.paper.riichi.model.MahjongTile> pair : options.getChiiPairs()) {
            String command = "/mahjong chii " + pair.getFirst().name().toLowerCase(Locale.ROOT) + " " + pair.getSecond().name().toLowerCase(Locale.ROOT);
            String label = "吃 " + pair.getFirst().name().toLowerCase(Locale.ROOT) + " " + pair.getSecond().name().toLowerCase(Locale.ROOT);
            message = message.append(this.actionButton(label, command, NamedTextColor.GREEN)).append(Component.space());
        }
        return message.append(this.actionButton("跳过", "/mahjong skip", NamedTextColor.GRAY));
    }

    private Component actionButton(String label, String command, NamedTextColor color) {
        return Component.text("[" + label + "]", color)
            .clickEvent(ClickEvent.runCommand(command))
            .hoverEvent(HoverEvent.showText(Component.text(command, NamedTextColor.GRAY)));
    }
}
