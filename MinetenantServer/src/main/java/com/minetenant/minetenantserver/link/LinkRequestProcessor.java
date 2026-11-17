package com.minetenant.minetenantserver.link;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minetenant.minetenantserver.network.LinkResultCode;

/**
 * 受け取った連携コードを検証して LinkService に渡す。Minecraftのクラスには依存しない。
 * 不正な入力や例外でも落とさず、必ず結果を返す。
 */
public class LinkRequestProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger("minetenantserver");

    /** Client側の上限と同じ値。packetの復号側でも制限しているが、ここでも守る。 */
    public static final int MAX_CODE_LENGTH = 64;

    private final LinkService service;

    public LinkRequestProcessor(LinkService service) {
        this.service = service;
    }

    /**
     * @param playerUuid Serverが接続中のplayerから取得したUUID
     * @param code       Clientから届いた連携コード
     */
    public LinkResultCode process(UUID playerUuid, String code) {
        if (playerUuid == null || !isValidFormat(code)) {
            return LinkResultCode.CODE_REJECTED;
        }

        try {
            LinkResultCode result = this.service.link(playerUuid, code);
            return result == null ? LinkResultCode.UNKNOWN : result;
        } catch (RuntimeException e) {
            LOGGER.error("Link service failed", e);
            return LinkResultCode.UNKNOWN;
        }
    }

    private static boolean isValidFormat(String code) {
        if (code == null || code.isEmpty() || code.length() > MAX_CODE_LENGTH) {
            return false;
        }

        return code.chars().noneMatch(c -> Character.isWhitespace(c) || Character.isISOControl(c));
    }
}
