package com.minetenant.callapi.command;

import com.minetenant.callapi.service.ApiService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.network.message.MessageType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

public class ApiCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("callapi")
//                        .suggests((context, builder) ->
//                                CommandSource.suggestMatching(new String[]{"https://api.example.com/data"}, builder))

                        .executes(commandContext ->  {
                            ServerCommandSource source = commandContext.getSource();
                            MinecraftServer server = source.getServer();

                            source.sendMessage(Text.literal("Calling API..."));

                            ApiService.fetchApiDataAsync("http://localhost:8787/api/hello")
                                    .thenAccept(response -> {
                                        server.execute(() -> {
                                            Text message = Text.literal("Successfully fetched API data: " + response);
                                            server.getPlayerManager().broadcast(message, false);
                                        });
                                    })
                                    .exceptionally(throwable -> {
                                        server.execute(() -> {
                                            Text errorMessage = Text.literal("Failed to fetch API data: " + throwable.getMessage());
                                            server.getPlayerManager().broadcast(errorMessage, false);
                                        });
                                        return null;
                                    });
                            return 1;
                        })
        );
    }
}
