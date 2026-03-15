package doublemoon.mahjongcraft.paper.render

import doublemoon.mahjongcraft.paper.model.SeatWind
import kotlin.test.Test
import kotlin.test.assertEquals

class TableRendererTest {
    @Test
    fun `wall indexing stays within four seat winds`() {
        assertEquals(SeatWind.EAST, WallLayout.wallSeat(0))
        assertEquals(SeatWind.EAST, WallLayout.wallSeat(16))
        assertEquals(SeatWind.SOUTH, WallLayout.wallSeat(17))
        assertEquals(SeatWind.WEST, WallLayout.wallSeat(34))
        assertEquals(SeatWind.NORTH, WallLayout.wallSeat(51))
        assertEquals(SeatWind.EAST, WallLayout.wallSeat(68))
        assertEquals(SeatWind.SOUTH, WallLayout.wallSeat(85))
        assertEquals(SeatWind.WEST, WallLayout.wallSeat(102))
        assertEquals(SeatWind.NORTH, WallLayout.wallSeat(119))
        assertEquals(SeatWind.NORTH, WallLayout.wallSeat(135))
    }

    @Test
    fun `wall indexing computes expected column and layer`() {
        assertEquals(0, WallLayout.wallColumn(0))
        assertEquals(16, WallLayout.wallColumn(16))
        assertEquals(0, WallLayout.wallColumn(17))
        assertEquals(16, WallLayout.wallColumn(135))
        assertEquals(0, WallLayout.wallLayer(0))
        assertEquals(0, WallLayout.wallLayer(67))
        assertEquals(1, WallLayout.wallLayer(68))
        assertEquals(1, WallLayout.wallLayer(135))
    }
}
