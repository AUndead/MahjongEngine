package doublemoon.mahjongcraft.paper.model;

public enum SeatWind {
    EAST(0, "东"),
    SOUTH(1, "南"),
    WEST(2, "西"),
    NORTH(3, "北");

    private final int index;
    private final String displayName;

    SeatWind(int index, String displayName) {
        this.index = index;
        this.displayName = displayName;
    }

    public int index() {
        return this.index;
    }

    public String displayName() {
        return this.displayName;
    }

    public static SeatWind fromIndex(int index) {
        for (SeatWind seatWind : values()) {
            if (seatWind.index == index) {
                return seatWind;
            }
        }
        throw new IllegalArgumentException("Unknown seat index: " + index);
    }
}
