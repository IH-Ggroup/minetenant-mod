package com.minetenant.minetenantclient.link;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FixtureLinkSubmitterTest {

    /** 予約された処理を、テストが好きなタイミングで実行する偽Scheduler。 */
    private static class ManualScheduler implements Scheduler {
        final List<Runnable> tasks = new ArrayList<>();
        int lastDelayTicks = -1;

        @Override
        public void runLater(int delayTicks, Runnable task) {
            this.lastDelayTicks = delayTicks;
            this.tasks.add(task);
        }

        void runAll() {
            List<Runnable> copy = new ArrayList<>(this.tasks);
            this.tasks.clear();
            copy.forEach(Runnable::run);
        }
    }

    private ManualScheduler scheduler;
    private FixtureLinkSubmitter submitter;
    private final List<LinkSubmitter.Outcome> results = new ArrayList<>();

    @BeforeEach
    void setUp() {
        scheduler = new ManualScheduler();
        submitter = new FixtureLinkSubmitter(scheduler);
        results.clear();
    }

    @Test
    void resultIsNotDeliveredUntilScheduledTaskRuns() {
        submitter.submit("ABC123", results::add);
        assertTrue(results.isEmpty());
        assertEquals(FixtureLinkSubmitter.DELAY_TICKS, scheduler.lastDelayTicks);

        scheduler.runAll();
        assertEquals(1, results.size());
    }

    @Test
    void ordinaryCodeSucceeds() {
        submitter.submit("ABC123", results::add);
        scheduler.runAll();
        assertTrue(results.get(0).isSuccess());
        assertNull(results.get(0).error());
    }

    @Test
    void failPrefixIsRejected() {
        submitter.submit("FAIL-1", results::add);
        scheduler.runAll();
        assertEquals(LinkError.CODE_REJECTED, results.get(0).error());
    }

    @Test
    void errorPrefixIsUnknown() {
        submitter.submit("ERROR-1", results::add);
        scheduler.runAll();
        assertEquals(LinkError.UNKNOWN, results.get(0).error());
    }

    @Test
    void prefixMatchIgnoresCase() {
        submitter.submit("fail-1", results::add);
        scheduler.runAll();
        assertEquals(LinkError.CODE_REJECTED, results.get(0).error());
    }

    @Test
    void worksWithLinkUiState() {
        LinkUiState state = new LinkUiState(new DefaultLinkCodeValidator(), submitter);
        state.submit("FAIL-1");
        assertEquals(LinkUiState.Phase.LOADING, state.phase());

        scheduler.runAll();
        assertEquals(LinkUiState.Phase.ERROR, state.phase());
        assertEquals(LinkError.CODE_REJECTED, state.error());
    }
}
