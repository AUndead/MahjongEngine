package doublemoon.mahjongcraft.paper.riichi.model

import org.mahjong4j.hands.Kantsu
import org.mahjong4j.hands.Mentsu
import org.mahjong4j.tile.Tile
import org.mahjong4j.tile.TileType
import org.mahjong4j.yaku.normals.NormalYaku
import org.mahjong4j.yaku.yakuman.Yakuman
import java.util.UUID

enum class MahjongTile {
    M1,
    M2,
    M3,
    M4,
    M5,
    M6,
    M7,
    M8,
    M9,
    P1,
    P2,
    P3,
    P4,
    P5,
    P6,
    P7,
    P8,
    P9,
    S1,
    S2,
    S3,
    S4,
    S5,
    S6,
    S7,
    S8,
    S9,
    EAST,
    SOUTH,
    WEST,
    NORTH,
    WHITE_DRAGON,
    GREEN_DRAGON,
    RED_DRAGON,
    M5_RED,
    P5_RED,
    S5_RED,
    UNKNOWN;

    val isRed: Boolean
        get() = this == M5_RED || this == P5_RED || this == S5_RED

    val code: Int = ordinal

    val mahjong4jTile: Tile
        get() {
            val tileCode = when (this) {
                M5_RED -> M5.code
                P5_RED -> P5.code
                S5_RED -> S5.code
                UNKNOWN -> M1.code
                else -> code
            }
            return Tile.valueOf(tileCode)
        }

    val sortOrder: Int
        get() = when (this) {
            M5_RED -> 4
            P5_RED -> 13
            S5_RED -> 22
            else -> code
        }

    val nextTile: MahjongTile
        get() {
            val tile = mahjong4jTile
            val nextTileCode = when (tile.type) {
                TileType.FONPAI -> if (tile == Tile.PEI) Tile.TON.code else tile.code + 1
                TileType.SANGEN -> if (tile == Tile.CHN) Tile.HAK.code else tile.code + 1
                else -> if (tile.number == 9) tile.code - 8 else tile.code + 1
            }
            return entries[nextTileCode]
        }

    val previousTile: MahjongTile
        get() {
            val tile = mahjong4jTile
            val previousTileCode = when (tile.type) {
                TileType.FONPAI -> if (tile == Tile.TON) Tile.PEI.code else tile.code - 1
                TileType.SANGEN -> if (tile == Tile.HAK) Tile.CHN.code else tile.code - 1
                else -> if (tile.number == 1) tile.code + 8 else tile.code - 1
            }
            return entries[previousTileCode]
        }

    fun itemModelPath(): String = "mahjong_tile/${name.lowercase()}"

    companion object {
        val normalWall = buildList {
            entries.forEach { tile ->
                repeat(4) { add(tile) }
                if (tile == RED_DRAGON) return@buildList
            }
        }

        val redFive3Wall = normalWall.toMutableList().apply {
            remove(M5)
            remove(P5)
            remove(S5)
            add(M5_RED)
            add(P5_RED)
            add(S5_RED)
        }

        val redFive4Wall = redFive3Wall.toMutableList().apply {
            remove(P5)
            add(P5_RED)
        }
    }
}

enum class Wind(val tile: Tile) {
    EAST(Tile.TON),
    SOUTH(Tile.NAN),
    WEST(Tile.SHA),
    NORTH(Tile.PEI)
}

enum class ClaimTarget {
    SELF,
    RIGHT,
    LEFT,
    ACROSS
}

enum class DoubleYakuman {
    DAISUSHI,
    SUANKO_TANKI,
    JUNSEI_CHURENPOHTO,
    KOKUSHIMUSO_JUSANMENMACHI
}

enum class ScoringStick(val point: Int) {
    P100(100),
    P1000(1000),
    P5000(5000),
    P10000(10000)
}

enum class ExhaustiveDraw {
    NORMAL,
    KYUUSHU_KYUUHAI,
    SUUFON_RENDA,
    SUUCHA_RIICHI,
    SUUKAIKAN
}

data class MahjongRound(
    var wind: Wind = Wind.EAST,
    var round: Int = 0,
    var honba: Int = 0
) {
    private var spentRounds = 0

    fun nextRound() {
        val nextRound = (round + 1) % 4
        honba = 0
        if (nextRound == 0) {
            val nextWindNum = (wind.ordinal + 1) % 4
            wind = Wind.entries[nextWindNum]
        }
        round = nextRound
        spentRounds++
    }

    fun isAllLast(rule: MahjongRule): Boolean = (spentRounds + 1) >= rule.length.rounds
}

data class MahjongRule(
    var length: GameLength = GameLength.TWO_WIND,
    var thinkingTime: ThinkingTime = ThinkingTime.NORMAL,
    var startingPoints: Int = 25000,
    var minPointsToWin: Int = 30000,
    var minimumHan: MinimumHan = MinimumHan.ONE,
    var spectate: Boolean = true,
    var redFive: RedFive = RedFive.THREE,
    var openTanyao: Boolean = true,
    var localYaku: Boolean = false
) {
    enum class GameLength(
        private val startingWind: Wind,
        val rounds: Int,
        val finalRound: Pair<Wind, Int>
    ) {
        ONE_GAME(Wind.EAST, 1, Wind.EAST to 3),
        EAST(Wind.EAST, 4, Wind.SOUTH to 3),
        SOUTH(Wind.SOUTH, 4, Wind.WEST to 3),
        TWO_WIND(Wind.EAST, 8, Wind.WEST to 3);

        fun getStartingRound(): MahjongRound = MahjongRound(wind = startingWind)
    }

    enum class MinimumHan(val han: Int) {
        ONE(1),
        TWO(2),
        FOUR(4),
        YAKUMAN(13)
    }

    enum class ThinkingTime(val base: Int, val extra: Int) {
        VERY_SHORT(3, 5),
        SHORT(5, 10),
        NORMAL(5, 20),
        LONG(60, 0),
        VERY_LONG(300, 0)
    }

    enum class RedFive(val quantity: Int) {
        NONE(0),
        THREE(3),
        FOUR(4)
    }
}

data class TileInstance(
    val id: UUID = UUID.randomUUID(),
    var mahjongTile: MahjongTile
) {
    val code: Int
        get() = mahjongTile.code

    val mahjong4jTile: Tile
        get() = mahjongTile.mahjong4jTile
}

fun List<TileInstance>.toMahjongTileList(): List<MahjongTile> = map { it.mahjongTile }

open class Fuuro(
    val mentsu: Mentsu,
    val tileInstances: List<TileInstance>,
    val claimTarget: ClaimTarget,
    val claimTile: TileInstance
)

class Kakantsu(identifierTile: Tile) : Kantsu(true, identifierTile)

data class YakuSettlement(
    val displayName: String,
    val uuid: String,
    val yakuList: List<NormalYaku>,
    val yakumanList: List<Yakuman>,
    val doubleYakumanList: List<DoubleYakuman>,
    val nagashiMangan: Boolean = false,
    val redFiveCount: Int = 0,
    val riichi: Boolean,
    val winningTile: MahjongTile,
    val hands: List<MahjongTile>,
    val fuuroList: List<Pair<Boolean, List<MahjongTile>>>,
    val doraIndicators: List<MahjongTile>,
    val uraDoraIndicators: List<MahjongTile>,
    val fu: Int,
    val han: Int,
    val score: Int
) {
    companion object {
        val NO_YAKU = YakuSettlement(
            displayName = "",
            uuid = "",
            yakuList = emptyList(),
            yakumanList = emptyList(),
            doubleYakumanList = emptyList(),
            riichi = false,
            winningTile = MahjongTile.UNKNOWN,
            hands = emptyList(),
            fuuroList = emptyList(),
            doraIndicators = emptyList(),
            uraDoraIndicators = emptyList(),
            fu = 0,
            han = 0,
            score = 0
        )

        fun nagashiMangan(
            displayName: String,
            uuid: String,
            doraIndicators: List<MahjongTile>,
            uraDoraIndicators: List<MahjongTile>,
            isDealer: Boolean
        ): YakuSettlement = YakuSettlement(
            displayName = displayName,
            uuid = uuid,
            yakuList = emptyList(),
            yakumanList = emptyList(),
            doubleYakumanList = emptyList(),
            nagashiMangan = true,
            riichi = false,
            winningTile = MahjongTile.UNKNOWN,
            hands = emptyList(),
            fuuroList = emptyList(),
            doraIndicators = doraIndicators,
            uraDoraIndicators = uraDoraIndicators,
            fu = 30,
            han = 5,
            score = if (isDealer) 12000 else 8000
        )
    }
}

data class ScoreItem(
    val displayName: String,
    val stringUUID: String,
    val scoreOrigin: Int,
    val scoreChange: Int
)

data class RankedScoreItem(
    val scoreItem: ScoreItem,
    val scoreTotal: Int,
    val scoreChangeText: String,
    val rankFloatText: String
)

data class ScoreSettlement(
    val title: String,
    val scoreList: List<ScoreItem>
) {
    val rankedScoreList: List<RankedScoreItem> = buildList {
        val origin = scoreList.sortedWith(compareBy<ScoreItem> { it.scoreOrigin }.thenBy { it.stringUUID }).reversed()
        val after = scoreList.sortedWith(compareBy<ScoreItem> { it.scoreOrigin + it.scoreChange }.thenBy { it.stringUUID }).reversed()
        after.forEachIndexed { index, playerScore ->
            val rankOrigin = origin.indexOf(playerScore)
            val rankFloat = when {
                index < rankOrigin -> "↑"
                index > rankOrigin -> "↓"
                else -> ""
            }
            val scoreChangeString = when {
                playerScore.scoreChange == 0 -> ""
                playerScore.scoreChange > 0 -> "+${playerScore.scoreChange}"
                else -> playerScore.scoreChange.toString()
            }
            add(
                RankedScoreItem(
                    scoreItem = playerScore,
                    scoreTotal = playerScore.scoreOrigin + playerScore.scoreChange,
                    scoreChangeText = scoreChangeString,
                    rankFloatText = rankFloat
                )
            )
        }
    }
}
