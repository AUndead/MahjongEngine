package doublemoon.mahjongcraft.paper.table;

import doublemoon.mahjongcraft.paper.riichi.ReactionResponse;
import doublemoon.mahjongcraft.paper.riichi.ReactionType;
import java.util.Objects;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

final class TableStateSoundCoordinator {
    private final MahjongTableSession session;
    private String lastTurnSoundFingerprint = "";
    private String lastRiichiSoundFingerprint = "";
    private String lastResolutionSoundFingerprint = "";

    TableStateSoundCoordinator(MahjongTableSession session) {
        this.session = session;
    }

    void syncStateSounds() {
        if (this.session.engine() == null) {
            this.reset();
            return;
        }
        this.syncTurnSound();
        this.syncRiichiSound();
        this.syncResolutionSound();
    }

    void playReactionSound(ReactionResponse response) {
        if (response.getType() == ReactionType.CHII) {
            this.broadcastSound(Sound.ENTITY_PLAYER_BURP, 0.85F, 1.15F);
        }
    }

    void playRoundStartSound() {
        this.broadcastSound(Sound.BLOCK_BEACON_POWER_SELECT, 0.9F, 1.2F);
    }

    void reset() {
        this.lastTurnSoundFingerprint = "";
        this.lastRiichiSoundFingerprint = "";
        this.lastResolutionSoundFingerprint = "";
    }

    void resetForRoundStart() {
        this.lastResolutionSoundFingerprint = "";
    }

    private void syncTurnSound() {
        String turnFingerprint = this.session.engine().getStarted()
            ? this.session.engine().getCurrentPlayerIndex() + ":" + this.session.engine().getWall().size() + ":" + Objects.toString(this.session.engine().getPendingReaction(), "")
            : "";
        if (!turnFingerprint.isBlank() && !turnFingerprint.equals(this.lastTurnSoundFingerprint)) {
            this.broadcastSound(Sound.UI_BUTTON_CLICK, 0.5F, 1.6F);
        }
        this.lastTurnSoundFingerprint = turnFingerprint;
    }

    private void syncRiichiSound() {
        String riichiFingerprint = this.session.riichiFingerprintValue();
        if (!riichiFingerprint.equals(this.lastRiichiSoundFingerprint) && !this.lastRiichiSoundFingerprint.isBlank()) {
            this.broadcastSound(Sound.BLOCK_BELL_USE, 0.8F, 1.25F);
        }
        this.lastRiichiSoundFingerprint = riichiFingerprint;
    }

    private void syncResolutionSound() {
        String resolutionFingerprint = Objects.toString(this.session.engine().getLastResolution(), "");
        if (!resolutionFingerprint.isBlank() && !resolutionFingerprint.equals(this.lastResolutionSoundFingerprint)) {
            Sound sound = this.session.engine().getLastResolution().getDraw() == null
                ? Sound.UI_TOAST_CHALLENGE_COMPLETE
                : Sound.BLOCK_NOTE_BLOCK_BELL;
            this.broadcastSound(sound, 0.9F, 1.0F);
        }
        this.lastResolutionSoundFingerprint = resolutionFingerprint;
    }

    private void broadcastSound(Sound sound, float volume, float pitch) {
        for (Player viewer : this.session.viewers()) {
            viewer.playSound(viewer.getLocation(), sound, volume, pitch);
        }
    }
}
