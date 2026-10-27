package com.minetenant.callapi.command;

import com.minetenant.callapi.service.ApiService;
import com.minetenant.callapi.config.ApiConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.network.message.MessageType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger("callapi");

    private static Throwable rootCause(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("callapi")
//                        .suggests((context, builder) ->
//                                CommandSource.suggestMatching(new String[]{"https://api.example.com/data"}, builder))

                        .executes(commandContext ->  {
                            ServerCommandSource source = commandContext.getSource();
                            MinecraftServer server = source.getServer();

                            source.sendMessage(Text.literal("Calling API..."));

                            // 結果は実行したsourceだけへ返す(他プレイヤーへbroadcastしない)
                            ApiService.fetchApiDataAsync(ApiConfig.getApiUrl())
                                    .thenAccept(response -> {
                                        server.execute(() -> {
                                            source.sendFeedback(() -> Text.literal("Successfully fetched API data: " + response), false);
                                        });
                                    })
                                    .exceptionally(throwable -> {
                                        // 画面にはエラー詳細・URLを出さず、ログにも例外の種類だけを残す
                                        LOGGER.warn("Failed to fetch API data: {}", rootCause(throwable).getClass().getSimpleName());
                                        server.execute(() -> {
                                            source.sendError(Text.literal("Failed to fetch API data"));
                                        });
                                        return null;
                                    });
                            return 1;
                        })
        );
    }
}
