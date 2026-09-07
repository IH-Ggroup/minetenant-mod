package com.minetenant.callapi;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import com.minetenant.callapi.command.ApiCommand;

public class Callapi implements ModInitializer {

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            ApiCommand.register(dispatcher);
        });
    }
}
