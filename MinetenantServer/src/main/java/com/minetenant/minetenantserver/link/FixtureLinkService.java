package com.minetenant.minetenantserver.link;

import java.util.Locale;
import java.util.UUID;

import com.minetenant.minetenantserver.network.LinkResultCode;

/**
 * 画面確認用の仮の照合先。通信も保存もしない。
 * <ul>
 *   <li>FAIL で始まる: コード拒否</li>
 *   <li>ERROR で始まる: 不明なエラー</li>
 *   <li>それ以外: 成功</li>
 * </ul>
 * Honoへの接続ができたら LinkService の実装ごと差し替える。
 */
public class FixtureLinkService implements LinkService {

    @Override
    public LinkResultCode link(UUID playerUuid, String code) {
        String upper = code.toUpperCase(Locale.ROOT);

        if (upper.startsWith("FAIL")) {
            return LinkResultCode.CODE_REJECTED;
        }

        if (upper.startsWith("ERROR")) {
            return LinkResultCode.UNKNOWN;
        }

        return LinkResultCode.SUCCESS;
    }
}
