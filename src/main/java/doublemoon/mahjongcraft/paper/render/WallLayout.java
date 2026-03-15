package doublemoon.mahjongcraft.paper.render;

import doublemoon.mahjongcraft.paper.model.SeatWind;

public final class WallLayout {
    private WallLayout() {
    }

    public static SeatWind wallSeat(int tileIndex) {
        return SeatWind.fromIndex((tileIndex % 68) / 17);
    }

    public static int wallColumn(int tileIndex) {
        return tileIndex % 17;
    }

    public static int wallLayer(int tileIndex) {
        return tileIndex / 68;
    }
}
