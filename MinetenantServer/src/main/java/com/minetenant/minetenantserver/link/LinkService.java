package com.minetenant.minetenantserver.link;

import java.util.UUID;

import com.minetenant.minetenantserver.network.LinkResultCode;

/**
 * 連携コードの照合。今はfixture実装だけ。後でHonoへのPOSTに差し替える。
 * playerUuid は Server が接続中のplayerから取得した値で、Clientの申告ではない。
 */
public interface LinkService {

    LinkResultCode link(UUID playerUuid, String code);
}
