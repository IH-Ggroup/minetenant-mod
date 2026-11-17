package com.minetenant.minetenantclient.link;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.minetenant.minetenantclient.network.LinkResultCode;

class LinkPacketClientTest {

    /** 送信内容を記録する偽Transport。 */
    private static class FakeTransport implements LinkPacketClient.Transport {
        boolean canSend = true;
        boolean throwOnSend;
        final List<Long> ids = new ArrayList<>();
        final List<String> codes = new ArrayList<>();

        @Override
        public boolean canSend() {
            return this.canSend;
        }

        @Override
        public void send(long requestId, String code) {
            if (this.throwOnSend) {
                throw new IllegalStateException("send failed");
            }
            this.ids.add(requestId);
            this.codes.add(code);
        }
    }

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

    private FakeTransport transport;
    private ManualScheduler scheduler;
    private LinkPacketClient client;
    private final List<LinkSubmitter.Outcome> results = new ArrayList<>();

    @BeforeEach
    void setUp() {
        transport = new FakeTransport();
        scheduler = new ManualScheduler();
        client = new LinkPacketClient(transport, scheduler);
        results.clear();
    }

    @Test
    void sendsCodeWithRequestId() {
        client.submit("ABC123", results::add);

        assertEquals(List.of("ABC123"), transport.codes);
        assertEquals(1, transport.ids.size());
        assertTrue(results.isEmpty());
    }

    @Test
    void successResponseDeliversSuccess() {
        client.submit("ABC123", results::add);
        client.onResponse(transport.ids.get(0), LinkResultCode.SUCCESS);

        assertTrue(results.get(0).isSuccess());
    }

    @Test
    void rejectedAndUnknownResponsesMapToErrors() {
        client.submit("A", results::add);
        client.submit("B", results::add);
        client.onResponse(transport.ids.get(0), LinkResultCode.CODE_REJECTED);
        client.onResponse(transport.ids.get(1), LinkResultCode.UNKNOWN);

        assertEquals(LinkError.CODE_REJECTED, results.get(0).error());
        assertEquals(LinkError.UNKNOWN, results.get(1).error());
    }

    @Test
    void requestIdsAreUnique() {
        client.submit("A", results::add);
        client.submit("B", results::add);

        assertFalse(transport.ids.get(0).equals(transport.ids.get(1)));
    }

    @Test
    void responseIsMatchedByRequestId() {
        client.submit("A", results::add);
        client.submit("B", results::add);
        // 後のrequestの結果が先に届いても、対応するcallbackだけが呼ばれる
        client.onResponse(transport.ids.get(1), LinkResultCode.CODE_REJECTED);

        assertEquals(1, results.size());
        assertEquals(LinkError.CODE_REJECTED, results.get(0).error());
    }

    @Test
    void unknownRequestIdIsIgnored() {
        client.submit("A", results::add);
        client.onResponse(9999L, LinkResultCode.SUCCESS);

        assertTrue(results.isEmpty());
    }

    @Test
    void duplicateResponseIsIgnored() {
        client.submit("A", results::add);
        long id = transport.ids.get(0);
        client.onResponse(id, LinkResultCode.SUCCESS);
        client.onResponse(id, LinkResultCode.CODE_REJECTED);

        assertEquals(1, results.size());
        assertTrue(results.get(0).isSuccess());
    }

    @Test
    void timeoutDeliversUnknown() {
        client.submit("A", results::add);
        assertEquals(LinkPacketClient.TIMEOUT_TICKS, scheduler.lastDelayTicks);

        scheduler.runAll();

        assertEquals(LinkError.UNKNOWN, results.get(0).error());
    }

    @Test
    void timeoutAfterResponseDoesNothing() {
        client.submit("A", results::add);
        client.onResponse(transport.ids.get(0), LinkResultCode.SUCCESS);
        scheduler.runAll();

        assertEquals(1, results.size());
        assertTrue(results.get(0).isSuccess());
    }

    @Test
    void cannotSendReturnsUnknownWithoutSending() {
        transport.canSend = false;
        client.submit("A", results::add);

        assertTrue(transport.ids.isEmpty());
        assertEquals(LinkError.UNKNOWN, results.get(0).error());
    }

    @Test
    void sendFailureReturnsUnknown() {
        transport.throwOnSend = true;
        client.submit("A", results::add);

        assertEquals(LinkError.UNKNOWN, results.get(0).error());
        assertTrue(scheduler.tasks.isEmpty());
    }

    @Test
    void resetDropsPendingWithoutCallback() {
        client.submit("A", results::add);
        long id = transport.ids.get(0);
        client.reset();
        client.onResponse(id, LinkResultCode.SUCCESS);
        scheduler.runAll();

        assertTrue(results.isEmpty());
    }

    @Test
    void worksWithLinkUiState() {
        LinkUiState state = new LinkUiState(new DefaultLinkCodeValidator(), client);
        state.submit("FAIL-1");
        assertEquals(LinkUiState.Phase.LOADING, state.phase());

        client.onResponse(transport.ids.get(0), LinkResultCode.CODE_REJECTED);

        assertEquals(LinkUiState.Phase.ERROR, state.phase());
        assertEquals(LinkError.CODE_REJECTED, state.error());
    }
}
