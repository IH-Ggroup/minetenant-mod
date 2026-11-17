package com.minetenant.minetenantclient.link;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.minetenant.minetenantclient.link.LinkUiState.Phase;

class LinkUiStateTest {

    /** 送信内容と結果callbackを記録するだけの偽submitter。 */
    private static class FakeSubmitter implements LinkSubmitter {
        final List<String> codes = new ArrayList<>();
        final List<Consumer<LinkSubmitter.Outcome>> callbacks = new ArrayList<>();

        @Override
        public void submit(String code, Consumer<LinkSubmitter.Outcome> onResult) {
            codes.add(code);
            callbacks.add(onResult);
        }

        void complete(int index, LinkSubmitter.Outcome outcome) {
            callbacks.get(index).accept(outcome);
        }
    }

    private FakeSubmitter submitter;
    private LinkUiState state;

    @BeforeEach
    void setUp() {
        submitter = new FakeSubmitter();
        state = new LinkUiState(new DefaultLinkCodeValidator(), submitter);
    }

    @Test
    void startsIdle() {
        assertEquals(Phase.IDLE, state.phase());
        assertNull(state.error());
    }

    @Test
    void emptyIsRejectedBeforeSending() {
        assertFalse(state.submit(""));
        assertEquals(Phase.ERROR, state.phase());
        assertEquals(LinkError.EMPTY, state.error());
        assertTrue(submitter.codes.isEmpty());
    }

    @Test
    void invalidFormatIsRejectedBeforeSending() {
        assertFalse(state.submit("AB CD"));
        assertEquals(Phase.ERROR, state.phase());
        assertEquals(LinkError.INVALID_FORMAT, state.error());
        assertTrue(submitter.codes.isEmpty());
    }

    @Test
    void validCodeGoesLoadingAndSendsTrimmedCode() {
        assertTrue(state.submit("  ABC123 "));
        assertEquals(Phase.LOADING, state.phase());
        assertEquals(List.of("ABC123"), submitter.codes);
    }

    @Test
    void secondSubmitWhileLoadingIsIgnored() {
        state.submit("ABC123");
        assertFalse(state.submit("ABC123"));
        assertFalse(state.submit(""));
        assertEquals(Phase.LOADING, state.phase());
        assertEquals(1, submitter.codes.size());
    }

    @Test
    void successOutcomeMovesToSuccess() {
        state.submit("ABC123");
        submitter.complete(0, LinkSubmitter.Outcome.success());
        assertEquals(Phase.SUCCESS, state.phase());
        assertNull(state.error());
    }

    @Test
    void failureOutcomeMovesToErrorWithReason() {
        state.submit("ABC123");
        submitter.complete(0, LinkSubmitter.Outcome.failure(LinkError.CODE_REJECTED));
        assertEquals(Phase.ERROR, state.phase());
        assertEquals(LinkError.CODE_REJECTED, state.error());
    }

    @Test
    void canResubmitAfterErrorAndAfterSuccess() {
        state.submit("ABC123");
        submitter.complete(0, LinkSubmitter.Outcome.failure(LinkError.CODE_REJECTED));
        assertTrue(state.submit("ABC124"));
        assertEquals(Phase.LOADING, state.phase());
        assertNull(state.error());

        submitter.complete(1, LinkSubmitter.Outcome.success());
        assertTrue(state.submit("ABC125"));
        assertEquals(3, submitter.codes.size());
    }

    @Test
    void validationErrorIsClearedByNextValidSubmit() {
        state.submit("");
        assertTrue(state.submit("ABC123"));
        assertNull(state.error());
    }

    @Test
    void duplicateCallbackIsIgnored() {
        state.submit("ABC123");
        submitter.complete(0, LinkSubmitter.Outcome.success());
        submitter.complete(0, LinkSubmitter.Outcome.failure(LinkError.CODE_REJECTED));
        assertEquals(Phase.SUCCESS, state.phase());
    }

    @Test
    void synchronousCallbackStillEndsInResultState() {
        LinkUiState syncState = new LinkUiState(new DefaultLinkCodeValidator(),
                (code, onResult) -> onResult.accept(LinkSubmitter.Outcome.success()));
        syncState.submit("ABC123");
        assertEquals(Phase.SUCCESS, syncState.phase());
    }

    @Test
    void resetReturnsToIdleAndDropsLateCallback() {
        state.submit("ABC123");
        state.reset();
        assertEquals(Phase.IDLE, state.phase());

        submitter.complete(0, LinkSubmitter.Outcome.success());
        assertEquals(Phase.IDLE, state.phase());
    }
}
