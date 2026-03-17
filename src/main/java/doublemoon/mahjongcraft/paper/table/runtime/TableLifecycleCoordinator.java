package doublemoon.mahjongcraft.paper.table;

final class TableLifecycleCoordinator {
    private final MahjongTableSession session;

    TableLifecycleCoordinator(MahjongTableSession session) {
        this.session = session;
    }

    void shutdown() {
        this.session.cancelBotTask();
        this.session.cancelNextRoundCountdownForLifecycle();
        this.session.shutdownRenderFlow();
        this.session.clearRenderDisplays();
        this.session.clearFeedbackTracking();
        this.session.clearRoundTrackingState();
        this.session.shutdownViewerPresentation();
        this.session.clearReadyPlayersForLifecycle();
        this.session.clearLeaveQueueForLifecycle();
        this.session.clearSpectatorsForLifecycle();
        this.session.clearEngineForLifecycle();
    }

    void forceEndMatch() {
        this.session.cancelBotTask();
        this.session.cancelNextRoundCountdownForLifecycle();
        this.session.shutdownRenderFlow();
        this.session.clearFeedbackTracking();
        this.session.clearRoundTrackingState();
        this.session.invalidateRenderFingerprints();
        this.session.resetViewerPresentationForLifecycleChange();
        this.session.clearLeaveQueueForLifecycle();
        this.session.clearEngineForLifecycle();
        this.session.resetReadyStateForNextRoundForLifecycle();
        this.session.render();
    }

    void resetForServerStartup() {
        this.session.cancelBotTask();
        this.session.cancelNextRoundCountdownForLifecycle();
        this.session.shutdownRenderFlow();
        this.session.clearRenderDisplays();
        this.session.clearFeedbackTracking();
        this.session.clearRoundTrackingState();
        this.session.clearReadyPlayersForLifecycle();
        this.session.clearLeaveQueueForLifecycle();
        this.session.clearSpectatorsForLifecycle();
        this.session.clearBotNamesForLifecycle();
        this.session.clearSeatAssignmentsForLifecycle();
        this.session.shutdownViewerPresentation();
        this.session.clearEngineForLifecycle();
        this.session.resetBotCounterForLifecycle();
    }
}
