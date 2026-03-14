package doublemoon.mahjongcraft.paper.riichi

import doublemoon.mahjongcraft.paper.riichi.model.ClaimTarget
import doublemoon.mahjongcraft.paper.riichi.model.DoubleYakuman
import doublemoon.mahjongcraft.paper.riichi.model.Fuuro
import doublemoon.mahjongcraft.paper.riichi.model.Kakantsu
import doublemoon.mahjongcraft.paper.riichi.model.MahjongRule
import doublemoon.mahjongcraft.paper.riichi.model.MahjongTile
import doublemoon.mahjongcraft.paper.riichi.model.ScoringStick
import doublemoon.mahjongcraft.paper.riichi.model.TileInstance
import doublemoon.mahjongcraft.paper.riichi.model.Wind
import doublemoon.mahjongcraft.paper.riichi.model.YakuSettlement
import doublemoon.mahjongcraft.paper.riichi.model.toMahjongTileList
import org.mahjong4j.GeneralSituation
import org.mahjong4j.Mahjong4jYakuConfig
import org.mahjong4j.PersonalSituation
import org.mahjong4j.Player
import org.mahjong4j.Score
import org.mahjong4j.hands.Hands
import org.mahjong4j.hands.Kantsu
import org.mahjong4j.hands.Kotsu
import org.mahjong4j.hands.Mentsu
import org.mahjong4j.hands.MentsuComp
import org.mahjong4j.hands.Shuntsu
import org.mahjong4j.tile.Tile
import org.mahjong4j.tile.TileType
import org.mahjong4j.yaku.normals.NormalYaku
import org.mahjong4j.yaku.yakuman.Yakuman
import kotlin.math.absoluteValue

open class RiichiPlayerState(
    val displayName: String,
    val uuid: String,
    val isRealPlayer: Boolean = true
) {
    val hands: MutableList<TileInstance> = mutableListOf()
    var autoArrangeHands: Boolean = true
    val fuuroList: MutableList<Fuuro> = mutableListOf()
    var riichiSengenTile: TileInstance? = null
    val discardedTiles: MutableList<TileInstance> = mutableListOf()
    val discardedTilesForDisplay: MutableList<TileInstance> = mutableListOf()
    var ready: Boolean = false
    var riichi: Boolean = false
    var doubleRiichi: Boolean = false
    val sticks: MutableList<ScoringStick> = mutableListOf()
    var points: Int = 0
    var basicThinkingTime: Int = 0
    var extraThinkingTime: Int = 0

    val riichiStickAmount: Int
        get() = sticks.count { it == ScoringStick.P1000 }

    val isMenzenchin: Boolean
        get() = fuuroList.isEmpty() || fuuroList.all { it.mentsu is Kantsu && !it.mentsu.isOpen }

    val isRiichiable: Boolean
        get() = isMenzenchin && !(riichi || doubleRiichi) && tilePairsForRiichi.isNotEmpty() && points >= 1000

    val numbersOfYaochuuhaiTypes: Int
        get() = hands.map { it.mahjong4jTile }.distinct().count { it.isYaochu }

    fun chii(
        tile: TileInstance,
        tilePair: Pair<MahjongTile, MahjongTile>,
        target: RiichiPlayerState
    ) {
        val tilePairCode = tilePair.first.mahjong4jTile to tilePair.second.mahjong4jTile
        val tileShuntsu = mutableListOf(
            tile,
            hands.find { it.mahjong4jTile == tilePairCode.first }!!,
            hands.find { it.mahjong4jTile == tilePairCode.second }!!
        ).also {
            it.sortBy { candidate -> candidate.mahjong4jTile.code }
        }
        val middleTile = tileShuntsu[1].mahjong4jTile
        val shuntsu = Shuntsu(true, middleTile)
        val fuuro = Fuuro(
            mentsu = shuntsu,
            tileInstances = tileShuntsu,
            claimTarget = ClaimTarget.LEFT,
            claimTile = tile
        )
        hands -= tileShuntsu.toMutableList().also { it -= tile }.toSet()
        target.discardedTilesForDisplay -= tile
        fuuroList += fuuro
    }

    fun pon(tile: TileInstance, claimTarget: ClaimTarget, target: RiichiPlayerState) {
        val kotsu = Kotsu(true, tile.mahjong4jTile)
        val tilesForPon = tilesForPon(tile)
        val fuuro = Fuuro(kotsu, tilesForPon, claimTarget, tile)
        hands -= tilesForPon.toSet()
        target.discardedTilesForDisplay -= tile
        fuuroList += fuuro
    }

    fun minkan(tile: TileInstance, claimTarget: ClaimTarget, target: RiichiPlayerState) {
        val kantsu = Kantsu(true, tile.mahjong4jTile)
        val tilesForMinkan = tilesForMinkan(tile)
        val fuuro = Fuuro(kantsu, tilesForMinkan, claimTarget, tile)
        hands -= tilesForMinkan.toSet()
        target.discardedTilesForDisplay -= tile
        fuuroList += fuuro
    }

    fun ankan(tile: TileInstance) {
        val kantsu = Kantsu(false, tile.mahjong4jTile)
        val tilesForAnkan = tilesForAnkan(tile)
        val fuuro = Fuuro(kantsu, tilesForAnkan, ClaimTarget.SELF, tile)
        hands -= tilesForAnkan.toSet()
        discardedTilesForDisplay -= tile
        fuuroList += fuuro
    }

    fun kakan(tile: TileInstance) {
        val minKotsu = fuuroList.find { tile.mahjongTile in it.tileInstances.toMahjongTileList() && it.mentsu is Kotsu } ?: return
        fuuroList -= minKotsu
        val kakantsu = Kakantsu(tile.mahjong4jTile)
        val tiles = minKotsu.tileInstances.toMutableList().also { it += tile }
        val fuuro = Fuuro(kakantsu, tiles, minKotsu.claimTarget, minKotsu.claimTile)
        hands -= tile
        fuuroList += fuuro
    }

    fun canPon(tile: TileInstance): Boolean = !(riichi || doubleRiichi) && sameTilesInHands(tile).size >= 2

    fun canMinkan(tile: TileInstance): Boolean = !(riichi || doubleRiichi) && sameTilesInHands(tile).size == 3

    val canKakan: Boolean
        get() = tilesCanKakan.isNotEmpty()

    val canAnkan: Boolean
        get() = tilesCanAnkan.isNotEmpty()

    fun canChii(tile: TileInstance): Boolean = !(riichi || doubleRiichi) && tilePairsForChii(tile).isNotEmpty()

    private fun tilesForPon(tile: TileInstance): List<TileInstance> =
        sameTilesInHands(tile).apply {
            if (size > 2) {
                remove(first { !it.mahjongTile.isRed })
                sortBy { it.mahjongTile.isRed }
            }
            add(tile)
        }

    private fun tilesForMinkan(tile: TileInstance): List<TileInstance> =
        sameTilesInHands(tile).also { it += tile }

    private fun tilesForAnkan(tile: TileInstance): List<TileInstance> =
        sameTilesInHands(tile)

    val tilesCanAnkan: Set<TileInstance>
        get() {
            val candidates = hands.distinct().filter { distinct ->
                hands.count { it.mahjong4jTile.code == distinct.mahjong4jTile.code } == 4
            }.toMutableSet()
            if (!riichi && !doubleRiichi) {
                return candidates
            }

            val machiBefore = machi
            val iterator = candidates.toList()
            iterator.forEach { candidate ->
                val handsCopy = hands.toMutableList()
                val anKanTilesInHands = hands.filter { tile -> tile.mahjong4jTile == candidate.mahjong4jTile }.toMutableList()
                handsCopy -= anKanTilesInHands.toSet()
                val fuuroListCopy = fuuroList.toMutableList().apply {
                    add(Fuuro(Kantsu(false, candidate.mahjong4jTile), anKanTilesInHands, ClaimTarget.SELF, candidate))
                }
                val mentsuList = fuuroListCopy.map { it.mentsu }
                val calculatedMachi = buildList {
                    MahjongTile.entries.filter { it.mahjong4jTile != candidate.mahjong4jTile }.forEach { mjTile ->
                        val nowHands = handsCopy.toTileCounts().apply { this[mjTile.mahjong4jTile.code]++ }
                        if (tilesWinnable(nowHands, mentsuList, mjTile.mahjong4jTile)) {
                            add(mjTile)
                        }
                    }
                }
                if (calculatedMachi != machiBefore) {
                    candidates -= candidate
                } else {
                    val otherTiles = hands.toTileCounts()
                    val mentsuList1 = fuuroList.map { it.mentsu }
                    calculatedMachi.forEach { machiTile ->
                        val tile = machiTile.mahjong4jTile
                        val mj4jHands = Hands(otherTiles, tile, mentsuList1)
                        val shuntsuList = mj4jHands.mentsuCompSet.flatMap { comp -> comp.shuntsuList }
                        shuntsuList.forEach { shuntsu ->
                            val middleTile = shuntsu.tile
                            val previousTile = MahjongTile.entries[middleTile.code].previousTile.mahjong4jTile
                            val nextTile = MahjongTile.entries[middleTile.code].nextTile.mahjong4jTile
                            val shuntsuTiles = listOf(previousTile, middleTile, nextTile)
                            if (candidate.mahjong4jTile in shuntsuTiles) {
                                candidates -= candidate
                            }
                        }
                    }
                }
            }
            return candidates
        }

    private val tilesCanKakan: MutableSet<Pair<TileInstance, ClaimTarget>>
        get() = mutableSetOf<Pair<TileInstance, ClaimTarget>>().apply {
            fuuroList.filter { it.mentsu is Kotsu }.forEach { fuuro ->
                val tile = hands.find { it.mahjong4jTile.code == fuuro.claimTile.mahjong4jTile.code }
                if (tile != null) {
                    add(tile to fuuro.claimTarget)
                }
            }
        }

    fun availableChiiPairs(tile: TileInstance): List<Pair<MahjongTile, MahjongTile>> = tilePairsForChii(tile)

    private fun tilePairsForChii(tile: TileInstance): List<Pair<MahjongTile, MahjongTile>> {
        val mj4jTile = tile.mahjong4jTile
        if (mj4jTile.number == 0) return emptyList()
        val mjTile = tile.mahjongTile
        val next = hands.find { it.mahjongTile == mjTile.nextTile }?.mahjongTile
        val nextNext = hands.find { it.mahjongTile == mjTile.nextTile.nextTile }?.mahjongTile
        val previous = hands.find { it.mahjongTile == mjTile.previousTile }?.mahjongTile
        val previousPrevious = hands.find { it.mahjongTile == mjTile.previousTile.previousTile }?.mahjongTile
        val pairs = mutableListOf<Pair<MahjongTile, MahjongTile>>()
        if (mj4jTile.number < 8 && next != null && nextNext != null) pairs += next to nextNext
        if (mj4jTile.number in 2..8 && previous != null && next != null) pairs += previous to next
        if (mj4jTile.number > 2 && previous != null && previousPrevious != null) pairs += previous to previousPrevious

        val sameTypeRedFiveTile = hands.firstOrNull { it.mahjongTile.isRed && it.mahjong4jTile.type == mj4jTile.type }
        val canChiiWithRedFive = (mj4jTile.number in 3..4 || mj4jTile.number in 6..7) && sameTypeRedFiveTile != null
        if (canChiiWithRedFive) {
            val redFiveTile = sameTypeRedFiveTile!!
            val redFiveTileCode = redFiveTile.mahjong4jTile.code
            val targetCode = mj4jTile.code
            val gap = redFiveTileCode - targetCode
            if (gap.absoluteValue == 1) {
                val firstTile = MahjongTile.entries[minOf(redFiveTileCode, targetCode)].previousTile
                val lastTile = MahjongTile.entries[maxOf(redFiveTileCode, targetCode)].nextTile
                val allTileInHands = hands.any { it.mahjongTile == firstTile } && hands.any { it.mahjongTile == lastTile }
                if (allTileInHands) {
                    pairs += firstTile to lastTile
                }
            } else {
                val midTileCode = (redFiveTileCode + targetCode) / 2
                val midTile = MahjongTile.entries[midTileCode]
                val midTileInHands = hands.any { it.mahjongTile == midTile }
                if (midTileInHands) {
                    pairs += if (gap > 0) {
                        redFiveTile.mahjongTile to midTile
                    } else {
                        midTile to redFiveTile.mahjongTile
                    }
                }
            }
        }
        return pairs.distinct()
    }

    fun tilePairForPon(tile: TileInstance): Pair<MahjongTile, MahjongTile> {
        val tiles = tilesForPon(tile)
        return tiles[0].mahjongTile to tiles[1].mahjongTile
    }

    val tilePairsForRiichi: List<Pair<MahjongTile, List<MahjongTile>>>
        get() = buildList {
            if (hands.size != 14) return@buildList
            val results = buildList {
                hands.forEach { tile ->
                    val nowHands = hands.toMutableList().also { it -= tile }.toMahjongTileList()
                    val nowMachi = calculateMachi(hands = nowHands)
                    if (nowMachi.isNotEmpty()) {
                        add(tile.mahjongTile to nowMachi)
                    }
                }
            }
            addAll(results.distinct())
        }

    private fun sameTilesInHands(tile: TileInstance): MutableList<TileInstance> =
        hands.filter { it.mahjong4jTile == tile.mahjong4jTile }.toMutableList()

    val isTenpai: Boolean
        get() = machi.isNotEmpty()

    private val machi: List<MahjongTile>
        get() = calculateMachi()

    private fun calculateMachi(
        hands: List<MahjongTile> = this.hands.toMahjongTileList(),
        fuuroList: List<Fuuro> = this.fuuroList
    ): List<MahjongTile> = MahjongTile.entries.filter { tile ->
        val tileInHandsCount = hands.count { it.mahjong4jTile == tile.mahjong4jTile }
        val tileInFuuroCount = fuuroList.sumOf { fuuro -> fuuro.tileInstances.count { it.mahjong4jTile == tile.mahjong4jTile } }
        val allTileHere = (tileInHandsCount + tileInFuuroCount) == 4
        if (allTileHere) return@filter false
        val nowHands = hands.toTileCounts().apply { this[tile.mahjong4jTile.code]++ }
        val mentsuList = fuuroList.map { it.mentsu }
        tilesWinnable(nowHands, mentsuList, tile.mahjong4jTile)
    }

    fun calculateMachiAndHan(
        hands: List<MahjongTile> = this.hands.toMahjongTileList(),
        fuuroList: List<Fuuro> = this.fuuroList,
        rule: MahjongRule,
        generalSituation: GeneralSituation,
        personalSituation: PersonalSituation
    ): Map<MahjongTile, Int> {
        val allMachi = calculateMachi(hands, fuuroList)
        return allMachi.associateWith { machiTile ->
            val yakuSettlement = calculateYakuSettlement(
                winningTile = machiTile,
                isWinningTileInHands = false,
                hands = hands,
                fuuroList = fuuroList,
                rule = rule,
                generalSituation = generalSituation,
                personalSituation = personalSituation,
                doraIndicators = emptyList(),
                uraDoraIndicators = emptyList()
            )
            if (yakuSettlement.yakuList.isNotEmpty() || yakuSettlement.yakumanList.isNotEmpty()) -1 else yakuSettlement.han
        }
    }

    fun isFuriten(tile: TileInstance, discards: List<TileInstance>): Boolean =
        isFuriten(tile.mahjong4jTile, discards.map { it.mahjong4jTile })

    fun isFuriten(
        tile: Tile,
        discards: List<Tile>,
        machi: List<Tile> = this.machi.map { it.mahjong4jTile }
    ): Boolean {
        val discardedTiles = discardedTiles.map { it.mahjong4jTile }
        if (tile in discardedTiles) return true
        if (discardedTiles.isNotEmpty()) {
            val lastDiscard = discardedTiles.last()
            val sameTurnStartIndex = discards.indexOf(lastDiscard)
            for (index in sameTurnStartIndex until discards.lastIndex) {
                if (discards[index] in machi) return true
            }
        }
        val riichiSengenTile = riichiSengenTile?.mahjong4jTile ?: return false
        if (riichi || doubleRiichi) {
            val riichiStartIndex = discards.indexOf(riichiSengenTile)
            for (index in riichiStartIndex until discards.lastIndex) {
                if (discards[index] in machi) return true
            }
        }
        return false
    }

    fun isIppatsu(players: List<RiichiPlayerState>, discards: List<TileInstance>): Boolean {
        if (riichi) {
            val riichiSengenIndex = discards.indexOf(riichiSengenTile!!)
            if (discards.lastIndex - riichiSengenIndex > 4) return false
            val someoneCalls = discards.slice(riichiSengenIndex..discards.lastIndex).any { tile ->
                players.any { player -> player.fuuroList.any { fuuro -> tile in fuuro.tileInstances } }
            }
            return !someoneCalls
        }
        return false
    }

    fun isKokushimuso(tile: Tile): Boolean {
        val otherTiles = hands.toTileCounts()
        val mentsuList = fuuroList.toMentsuList()
        val mj4jHands = Hands(otherTiles, tile, mentsuList)
        return mj4jHands.isKokushimuso
    }

    fun canWin(
        winningTile: MahjongTile,
        isWinningTileInHands: Boolean,
        hands: List<MahjongTile> = this.hands.toMahjongTileList(),
        fuuroList: List<Fuuro> = this.fuuroList,
        rule: MahjongRule,
        generalSituation: GeneralSituation,
        personalSituation: PersonalSituation
    ): Boolean {
        val yakuSettlement = calculateYakuSettlement(
            winningTile = winningTile,
            isWinningTileInHands = isWinningTileInHands,
            hands = hands,
            fuuroList = fuuroList,
            rule = rule,
            generalSituation = generalSituation,
            personalSituation = personalSituation
        )
        return yakuSettlement.yakumanList.isNotEmpty() ||
            yakuSettlement.doubleYakumanList.isNotEmpty() ||
            yakuSettlement.han >= rule.minimumHan.han
    }

    private fun tilesWinnable(hands: IntArray, mentsuList: List<Mentsu>, lastTile: Tile): Boolean =
        Hands(hands, lastTile, mentsuList).canWin

    private fun calculateYakuSettlement(
        winningTile: MahjongTile,
        isWinningTileInHands: Boolean,
        hands: List<MahjongTile>,
        fuuroList: List<Fuuro>,
        rule: MahjongRule,
        generalSituation: GeneralSituation,
        personalSituation: PersonalSituation,
        doraIndicators: List<MahjongTile> = listOf(),
        uraDoraIndicators: List<MahjongTile> = listOf()
    ): YakuSettlement {
        val handsIntArray = hands.toTileCounts().also { if (!isWinningTileInHands) it[winningTile.mahjong4jTile.code]++ }
        val mentsuList = fuuroList.toMentsuList()
        val mj4jHands = Hands(handsIntArray, winningTile.mahjong4jTile, mentsuList)
        if (!mj4jHands.canWin) return YakuSettlement.NO_YAKU
        val mj4jPlayer = Player(mj4jHands, generalSituation, personalSituation).apply { calculate() }
        var finalHan = 0
        var finalFu = 0
        var finalRedFiveCount = 0
        var finalNormalYakuList = mutableListOf<NormalYaku>()
        val finalYakumanList = mj4jPlayer.yakumanList.toMutableList()
        val finalDoubleYakumanList = mutableListOf<DoubleYakuman>()
        if (finalYakumanList.isNotEmpty()) {
            if (!rule.localYaku && Yakuman.RENHO in finalYakumanList) {
                finalYakumanList -= Yakuman.RENHO
            }
            val handsWithoutWinningTile = hands.toMutableList().also { if (isWinningTileInHands) it -= winningTile }
            val machiBeforeWin = calculateMachi(handsWithoutWinningTile, fuuroList)
            when {
                Yakuman.DAISUSHI in finalYakumanList -> {
                    finalYakumanList -= Yakuman.DAISUSHI
                    finalDoubleYakumanList += DoubleYakuman.DAISUSHI
                }
                Yakuman.KOKUSHIMUSO in finalYakumanList && machiBeforeWin.size == 13 -> {
                    finalYakumanList -= Yakuman.KOKUSHIMUSO
                    finalDoubleYakumanList += DoubleYakuman.KOKUSHIMUSO_JUSANMENMACHI
                }
                Yakuman.CHURENPOHTO in finalYakumanList && machiBeforeWin.size == 9 -> {
                    finalYakumanList -= Yakuman.CHURENPOHTO
                    finalDoubleYakumanList += DoubleYakuman.JUNSEI_CHURENPOHTO
                }
                Yakuman.SUANKO in finalYakumanList && machiBeforeWin.size == 1 -> {
                    finalYakumanList -= Yakuman.SUANKO
                    finalDoubleYakumanList += DoubleYakuman.SUANKO_TANKI
                }
            }
        } else {
            var finalComp: MentsuComp =
                mj4jHands.mentsuCompSet.firstOrNull() ?: throw IllegalStateException("Winning hand missing mentsu composition")
            mj4jHands.mentsuCompSet.forEach { comp ->
                val yakuStock = mutableListOf<NormalYaku>()
                val resolverSet = Mahjong4jYakuConfig.getNormalYakuResolverSet(comp, generalSituation, personalSituation)
                resolverSet.filter { it.isMatch }.forEach { yakuStock += it.normalYaku }
                if (!rule.openTanyao && mj4jHands.isOpen && NormalYaku.TANYAO in yakuStock) {
                    yakuStock -= NormalYaku.TANYAO
                }
                val hanSum = if (mj4jHands.isOpen) yakuStock.sumOf { it.kuisagari } else yakuStock.sumOf { it.han }
                if (hanSum > finalHan) {
                    finalHan = hanSum
                    finalNormalYakuList = yakuStock
                    finalComp = comp
                }
            }
            if (finalHan >= rule.minimumHan.han) {
                val handsComp = mj4jHands.handsComp
                val isRiichi = NormalYaku.REACH in finalNormalYakuList
                val doraAmount = generalSituation.dora.sumOf { handsComp[it.code] }
                repeat(doraAmount) {
                    finalNormalYakuList += NormalYaku.DORA
                    finalHan += NormalYaku.DORA.han
                }
                if (isRiichi) {
                    val uraDoraAmount = generalSituation.uradora.sumOf { handsComp[it.code] }
                    repeat(uraDoraAmount) {
                        finalNormalYakuList += NormalYaku.URADORA
                        finalHan += NormalYaku.URADORA.han
                    }
                }
                if (rule.redFive != MahjongRule.RedFive.NONE) {
                    val handsRedFiveCount = this.hands.count { it.mahjongTile.isRed }
                    val fuuroListRedFiveCount = fuuroList.sumOf { fuuro -> fuuro.tileInstances.count { it.mahjongTile.isRed } }
                    finalRedFiveCount = handsRedFiveCount + fuuroListRedFiveCount
                    finalHan += finalRedFiveCount
                }
            }
            finalFu = when {
                finalNormalYakuList.isEmpty() -> 0
                NormalYaku.PINFU in finalNormalYakuList && NormalYaku.TSUMO in finalNormalYakuList -> 20
                NormalYaku.CHITOITSU in finalNormalYakuList -> 25
                else -> {
                    var tmpFu = 20
                    tmpFu += when {
                        personalSituation.isTsumo -> 2
                        !mj4jHands.isOpen -> 10
                        else -> 0
                    }
                    tmpFu += finalComp.allMentsu.sumOf { it.fu }
                    tmpFu += if (finalComp.isKanchan(mj4jHands.last) || finalComp.isPenchan(mj4jHands.last) || finalComp.isTanki(mj4jHands.last)) 2 else 0
                    val jantoTile = finalComp.janto.tile
                    if (jantoTile == generalSituation.bakaze) tmpFu += 2
                    if (jantoTile == personalSituation.jikaze) tmpFu += 2
                    if (jantoTile.type == TileType.SANGEN) tmpFu += 2
                    tmpFu
                }
            }
        }
        val fuuroListForSettlement = fuuroList.map { fuuro ->
            (fuuro.mentsu is Kantsu && !fuuro.mentsu.isOpen) to fuuro.tileInstances.toMahjongTileList()
        }
        val score = if (finalYakumanList.isNotEmpty() || finalDoubleYakumanList.isNotEmpty()) {
            val yakumanScore = finalYakumanList.size * 32000
            val doubleYakumanScore = finalDoubleYakumanList.size * 64000
            val scoreSum = yakumanScore + doubleYakumanScore
            val isParent = personalSituation.jikaze == Wind.EAST.tile
            if (isParent) (scoreSum * 1.5).toInt() else scoreSum
        } else {
            val isParent = personalSituation.jikaze == Wind.EAST.tile
            Score.calculateScore(isParent, finalHan, finalFu).ron
        }
        return YakuSettlement(
            displayName = displayName,
            uuid = uuid,
            yakuList = finalNormalYakuList,
            yakumanList = finalYakumanList,
            doubleYakumanList = finalDoubleYakumanList,
            redFiveCount = finalRedFiveCount,
            riichi = riichi || doubleRiichi,
            winningTile = winningTile,
            hands = this.hands.toMahjongTileList(),
            fuuroList = fuuroListForSettlement,
            doraIndicators = doraIndicators,
            uraDoraIndicators = uraDoraIndicators,
            fu = finalFu,
            han = finalHan,
            score = score
        )
    }

    fun calcYakuSettlementForWin(
        winningTile: MahjongTile,
        isWinningTileInHands: Boolean,
        rule: MahjongRule,
        generalSituation: GeneralSituation,
        personalSituation: PersonalSituation,
        doraIndicators: List<MahjongTile>,
        uraDoraIndicators: List<MahjongTile>
    ): YakuSettlement = calculateYakuSettlement(
        winningTile = winningTile,
        isWinningTileInHands = isWinningTileInHands,
        hands = hands.toMahjongTileList(),
        fuuroList = fuuroList,
        rule = rule,
        generalSituation = generalSituation,
        personalSituation = personalSituation,
        doraIndicators = doraIndicators,
        uraDoraIndicators = uraDoraIndicators
    )

    fun drawTile(tile: TileInstance) {
        hands += tile
    }

    fun declareRiichi(riichiSengenTile: TileInstance, isFirstRound: Boolean) {
        this.riichiSengenTile = riichiSengenTile
        if (isFirstRound) doubleRiichi = true else riichi = true
    }

    fun discardTile(tile: MahjongTile): TileInstance? =
        hands.findLast { it.mahjongTile == tile }?.also {
            hands -= it
            discardedTiles += it
            discardedTilesForDisplay += it
        }

    @JvmName("toTileCountsInstances")
    private fun List<TileInstance>.toTileCounts(): IntArray =
        IntArray(Tile.values().size) { code -> count { it.mahjong4jTile.code == code } }

    @JvmName("toTileCountsTiles")
    private fun List<MahjongTile>.toTileCounts(): IntArray =
        IntArray(Tile.values().size) { code -> count { it.mahjong4jTile.code == code } }

    private fun List<Fuuro>.toMentsuList(): List<Mentsu> = map { it.mentsu }
}
