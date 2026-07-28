package com.minetenant.callapi.command;

import com.minetenant.callapi.service.ApiService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
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

                            source.sendMessage(Text.literal("Calling API..."));

                            ApiService.fetchApiDataAsync("http://localhost:8787/api/hello")
                                    .thenAccept(response -> {
                                        source.getServer().execute(() -> {
                                            source.sendMessage(Text.literal("API Response: " + response));
                                        });
                                    })
                                    .exceptionally(throwable -> {
                                        source.getServer().execute(() -> {
                                            source.sendMessage(Text.literal("Error calling API: " + throwable.getMessage()));
                                        });
                                        return null;
                                    });
                            return 1;
                        })
        );
    }
}
