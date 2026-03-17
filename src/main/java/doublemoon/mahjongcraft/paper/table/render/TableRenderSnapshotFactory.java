package doublemoon.mahjongcraft.paper.table;

import doublemoon.mahjongcraft.paper.model.SeatWind;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Location;

final class TableRenderSnapshotFactory {
    MahjongTableSession.RenderSnapshot create(MahjongTableSession session, long version, long cancellationNonce) {
        Location tableCenter = session.center();
        EnumMap<SeatWind, MahjongTableSession.SeatRenderSnapshot> seats = new EnumMap<>(SeatWind.class);
        for (SeatWind wind : SeatWind.values()) {
            seats.put(wind, this.captureSeatSnapshot(session, wind));
        }
        return new MahjongTableSession.RenderSnapshot(
            version,
            cancellationNonce,
            Objects.toString(tableCenter.getWorld() == null ? null : tableCenter.getWorld().getName(), ""),
            tableCenter.getX(),
            tableCenter.getY(),
            tableCenter.getZ(),
            session.isStarted(),
            session.engine() != null && session.engine().getGameFinished(),
            session.remainingWall().size(),
            session.kanCount(),
            session.dicePoints(),
            session.roundIndex(),
            session.honbaCount(),
            session.dealerSeat(),
            session.currentSeat(),
            session.openDoorSeat(),
            session.waitingDisplaySummary(),
            session.ruleDisplaySummary(),
            session.publicCenterText(),
            session.lastPublicDiscardPlayerIdValue(),
            session.lastPublicDiscardTile(),
            List.copyOf(session.doraIndicators()),
            seats
        );
    }

    private MahjongTableSession.SeatRenderSnapshot captureSeatSnapshot(MahjongTableSession session, SeatWind wind) {
        UUID playerId = session.playerAt(wind);
        return new MahjongTableSession.SeatRenderSnapshot(
            wind,
            playerId,
            session.displayName(playerId),
            session.publicSeatStatus(wind),
            session.points(playerId),
            session.isRiichi(playerId),
            session.isReady(playerId),
            session.isQueuedToLeave(playerId),
            playerId != null && session.onlinePlayer(playerId) != null,
            playerId == null ? "" : session.viewerMembershipSignatureFor(playerId),
            playerId == null ? -1 : session.selectedHandTileIndex(playerId),
            playerId == null ? -1 : session.riichiDiscardIndex(playerId),
            session.stickLayoutCount(wind),
            playerId == null ? List.of() : session.viewerIdsExcluding(playerId),
            playerId == null ? List.of() : session.hand(playerId),
            playerId == null ? List.of() : session.discards(playerId),
            playerId == null ? List.of() : session.fuuro(playerId),
            playerId == null ? List.of() : session.scoringSticks(playerId),
            session.cornerSticks(wind)
        );
    }
}
