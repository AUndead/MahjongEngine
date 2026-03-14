package doublemoon.mahjongcraft.paper.render;

import doublemoon.mahjongcraft.paper.model.MahjongTile;
import doublemoon.mahjongcraft.paper.model.SeatWind;
import doublemoon.mahjongcraft.paper.table.MahjongTableSession;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;

public final class TableRenderer {
    private static final double HAND_RADIUS = 1.45D;
    private static final double MELD_RADIUS = 1.13D;
    private static final double WALL_RADIUS = 1.9D;
    private static final double HAND_STEP = 0.16D;
    private static final double WALL_STEP = 0.145D;
    private static final double MELD_GAP = 0.07D;

    public List<Entity> render(MahjongTableSession session) {
        List<Entity> spawned = new ArrayList<>();
        Location center = session.center().clone().add(0.0D, 1.02D, 0.0D);

        for (SeatWind wind : SeatWind.values()) {
            UUID playerId = session.playerAt(wind);
            Location handBase = seatBase(center, wind, HAND_RADIUS);
            float yaw = seatYaw(wind);

            spawned.add(DisplayEntities.spawnLabel(
                handBase.clone().add(0.0D, 0.45D, 0.0D),
                Component.text(wind.displayName() + "  " + session.playerName(wind)),
                Color.fromARGB(96, 0, 0, 0)
            ));

            if (playerId == null) {
                continue;
            }

            renderHand(session, spawned, wind, yaw, playerId, handBase);
            renderDiscards(session, spawned, wind, yaw, playerId, center);
            renderMelds(session, spawned, center, wind, yaw, playerId);
        }

        int wallCount = session.remainingWall().size();
        for (int i = 0; i < wallCount; i++) {
            int side = i / 17;
            int sideIndex = i % 17;
            int layer = (i / 68) % 2;
            SeatWind wind = SeatWind.fromIndex(side);
            Location wallBase = seatBase(center, wind, WALL_RADIUS);
            Location tileLocation = offsetAlongSeat(wallBase, wind, centeredOffset(17, sideIndex, WALL_STEP))
                .add(0.0D, layer * 0.08D, 0.0D);
            spawned.add(DisplayEntities.spawnTileDisplay(session.plugin(), tileLocation, seatYaw(wind), MahjongTile.M1, true, null, true));
        }

        spawned.add(DisplayEntities.spawnLabel(
            center.clone().add(0.0D, 0.3D, 0.0D),
            Component.text("牌山 " + wallCount + " 张 | 当前 " + session.currentSeat().displayName()),
            Color.fromARGB(112, 20, 80, 20)
        ));

        return spawned;
    }

    private static void renderHand(
        MahjongTableSession session,
        List<Entity> spawned,
        SeatWind wind,
        float yaw,
        UUID playerId,
        Location handBase
    ) {
        List<MahjongTile> hand = session.hand(playerId);
        Player owner = session.onlinePlayer(playerId);
        for (int i = 0; i < hand.size(); i++) {
            Location tileLocation = offsetAlongSeat(handBase, wind, centeredOffset(hand.size(), i, HAND_STEP));
            DisplayClickAction clickAction = new DisplayClickAction(session.id(), playerId, i);
            ItemDisplay publicDisplay = DisplayEntities.spawnTileDisplay(session.plugin(), tileLocation, yaw, hand.get(i), true, null, true);
            spawned.add(publicDisplay);
            if (owner != null) {
                ItemDisplay privateDisplay = DisplayEntities.spawnTileDisplay(session.plugin(), tileLocation, yaw, hand.get(i), false, clickAction, false);
                spawned.add(privateDisplay);
                owner.hideEntity(session.plugin(), publicDisplay);
                owner.showEntity(session.plugin(), privateDisplay);
            }
        }
    }

    private static void renderDiscards(
        MahjongTableSession session,
        List<Entity> spawned,
        SeatWind wind,
        float yaw,
        UUID playerId,
        Location center
    ) {
        List<MahjongTile> discards = session.discards(playerId);
        for (int i = 0; i < discards.size(); i++) {
            int row = i / 6;
            int column = i % 6;
            Location discardBase = seatBase(center, wind, 0.75D + row * 0.22D);
            Location tileLocation = offsetAlongSeat(discardBase, wind, centeredOffset(6, column, HAND_STEP));
            spawned.add(DisplayEntities.spawnTileDisplay(session.plugin(), tileLocation, yaw, discards.get(i), false, null, true));
        }
    }

    private static void renderMelds(
        MahjongTableSession session,
        List<Entity> spawned,
        Location center,
        SeatWind wind,
        float yaw,
        UUID playerId
    ) {
        List<MeldView> melds = session.fuuro(playerId);
        if (melds.isEmpty()) {
            return;
        }

        double totalWidth = 0.0D;
        for (MeldView meld : melds) {
            totalWidth += meld.tiles().size() * HAND_STEP;
        }
        totalWidth += Math.max(0, melds.size() - 1) * MELD_GAP;

        double cursor = -totalWidth / 2.0D + HAND_STEP / 2.0D;
        Location meldBase = seatBase(center, wind, MELD_RADIUS);
        for (int meldIndex = 0; meldIndex < melds.size(); meldIndex++) {
            MeldView meld = melds.get(meldIndex);
            for (int i = 0; i < meld.tiles().size(); i++) {
                Location tileLocation = offsetAlongSeat(meldBase, wind, cursor);
                spawned.add(DisplayEntities.spawnTileDisplay(
                    session.plugin(),
                    tileLocation,
                    yaw,
                    meld.tiles().get(i),
                    meld.faceDownAt(i),
                    null,
                    true
                ));
                cursor += HAND_STEP;
            }
            if (meldIndex + 1 < melds.size()) {
                cursor += MELD_GAP;
            }
        }
    }

    private static Location seatBase(Location center, SeatWind wind, double radius) {
        return switch (wind) {
            case EAST -> center.clone().add(radius, 0.0D, 0.0D);
            case SOUTH -> center.clone().add(0.0D, 0.0D, radius);
            case WEST -> center.clone().add(-radius, 0.0D, 0.0D);
            case NORTH -> center.clone().add(0.0D, 0.0D, -radius);
        };
    }

    private static Location offsetAlongSeat(Location base, SeatWind wind, double offset) {
        return switch (wind) {
            case EAST, WEST -> base.clone().add(0.0D, 0.0D, offset);
            case SOUTH, NORTH -> base.clone().add(offset, 0.0D, 0.0D);
        };
    }

    private static double centeredOffset(int size, int index, double step) {
        return (index - (size - 1) / 2.0D) * step;
    }

    private static float seatYaw(SeatWind wind) {
        return switch (wind) {
            case EAST -> -90.0F;
            case SOUTH -> 0.0F;
            case WEST -> 90.0F;
            case NORTH -> 180.0F;
        };
    }
}
