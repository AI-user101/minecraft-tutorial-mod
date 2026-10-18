package com.maats.tutorialmod.client;

import com.maats.tutorialmod.net.TutorialProgressPayload;
import com.maats.tutorialmod.net.TutorialStepPayload;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public class TutorialModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(TutorialStepPayload.TYPE, (payload, context) ->
                context.client().execute(() -> TutorialClientState.onStep(payload)));
        ClientPlayNetworking.registerGlobalReceiver(TutorialProgressPayload.TYPE, (payload, context) ->
                context.client().execute(() -> TutorialClientState.onProgress(payload)));

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> TutorialClientState.clear());

        HudRenderCallback.EVENT.register((gui, tickCounter) -> TutorialHud.render(gui));
    }
}
