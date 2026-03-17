package doublemoon.mahjongcraft.paper.table;

import java.util.Objects;

final class SettlementFeedbackGate {
    private SettlementFeedbackGate() {
    }

    static boolean isNewSettlement(String settlementFingerprint, String lastSettlementFingerprint) {
        return !Objects.toString(settlementFingerprint, "").isBlank()
            && !Objects.equals(settlementFingerprint, lastSettlementFingerprint);
    }
}
