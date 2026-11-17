package com.minetenant.minetenantserver.link;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.minetenant.minetenantserver.network.LinkResultCode;

class LinkRequestProcessorTest {

    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    /** 渡された値を記録する偽service。 */
    private static class RecordingService implements LinkService {
        UUID lastUuid;
        String lastCode;
        int calls;
        LinkResultCode result = LinkResultCode.SUCCESS;

        @Override
        public LinkResultCode link(UUID playerUuid, String code) {
            this.calls++;
            this.lastUuid = playerUuid;
            this.lastCode = code;
            return this.result;
        }
    }

    @Test
    void passesPlayerUuidAndCodeToService() {
        RecordingService service = new RecordingService();
        LinkResultCode result = new LinkRequestProcessor(service).process(PLAYER, "ABC123");

        assertEquals(LinkResultCode.SUCCESS, result);
        assertEquals(PLAYER, service.lastUuid);
        assertEquals("ABC123", service.lastCode);
    }

    @Test
    void returnsServiceResult() {
        RecordingService service = new RecordingService();
        service.result = LinkResultCode.CODE_REJECTED;

        assertEquals(LinkResultCode.CODE_REJECTED, new LinkRequestProcessor(service).process(PLAYER, "ABC123"));
    }

    @Test
    void malformedCodeIsRejectedWithoutCallingService() {
        RecordingService service = new RecordingService();
        LinkRequestProcessor processor = new LinkRequestProcessor(service);

        assertEquals(LinkResultCode.CODE_REJECTED, processor.process(PLAYER, null));
        assertEquals(LinkResultCode.CODE_REJECTED, processor.process(PLAYER, ""));
        assertEquals(LinkResultCode.CODE_REJECTED, processor.process(PLAYER, "AB C"));
        assertEquals(LinkResultCode.CODE_REJECTED, processor.process(PLAYER, "AB\nC"));
        assertEquals(LinkResultCode.CODE_REJECTED,
                processor.process(PLAYER, "A".repeat(LinkRequestProcessor.MAX_CODE_LENGTH + 1)));
        assertEquals(0, service.calls);
        assertNull(service.lastUuid);
    }

    @Test
    void maxLengthCodeIsAccepted() {
        RecordingService service = new RecordingService();

        assertEquals(LinkResultCode.SUCCESS,
                new LinkRequestProcessor(service).process(PLAYER, "A".repeat(LinkRequestProcessor.MAX_CODE_LENGTH)));
    }

    @Test
    void nullUuidIsRejected() {
        RecordingService service = new RecordingService();

        assertEquals(LinkResultCode.CODE_REJECTED, new LinkRequestProcessor(service).process(null, "ABC123"));
        assertEquals(0, service.calls);
    }

    @Test
    void serviceExceptionBecomesUnknown() {
        LinkService throwing = (uuid, code) -> {
            throw new IllegalStateException("boom");
        };

        assertEquals(LinkResultCode.UNKNOWN, new LinkRequestProcessor(throwing).process(PLAYER, "ABC123"));
    }

    @Test
    void nullServiceResultBecomesUnknown() {
        assertEquals(LinkResultCode.UNKNOWN,
                new LinkRequestProcessor((uuid, code) -> null).process(PLAYER, "ABC123"));
    }
}
