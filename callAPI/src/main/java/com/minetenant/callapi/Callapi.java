package com.minetenant.callapi;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import com.minetenant.callapi.command.ApiCommand;
import com.minetenant.callapi.config.ApiConfig;

public class Callapi implements ModInitializer {

    @Override
    public void onInitialize() {
        ApiConfig.load();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            ApiCommand.register(dispatcher);
        });
    }
}
