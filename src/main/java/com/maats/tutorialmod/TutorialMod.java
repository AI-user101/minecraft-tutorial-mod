package com.maats.tutorialmod;

import com.maats.tutorialmod.net.TutorialProgressPayload;
import com.maats.tutorialmod.net.TutorialStepPayload;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.serialization.Codec;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.level.gamerules.GameRuleType;
import net.minecraft.world.level.gamerules.GameRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TutorialMod implements ModInitializer {

    public static final String MODID = "tutorialmod";
    public static final Logger LOGGER = LoggerFactory.getLogger("Tutorial Mod");

    /**
     * Gamerule: of de speler bij elke stap automatisch de benodigde materialen krijgt.
     * Standaard aan: /gamerule tutorial_give_materials false om uit te zetten.
     */
    public static final GameRule<Boolean> RULE_GIVE_MATERIALS = new GameRule<>(
            GameRuleCategory.MISC,
            GameRuleType.BOOL,
            BoolArgumentType.bool(),
            (visitor, rule) -> visitor.visitBoolean(rule),
            Codec.BOOL,
            value -> value ? 1 : 0,
            Boolean.TRUE,
            FeatureFlagSet.of());

    @Override
    public void onInitialize() {
        try {
            Registry.register(BuiltInRegistries.GAME_RULE,
                    Identifier.fromNamespaceAndPath(MODID, "tutorial_give_materials"),
                    RULE_GIVE_MATERIALS);
        } catch (IllegalStateException e) {
            LOGGER.warn("Gamerule kon niet worden geregistreerd: {}", e.getMessage());
        }

        PayloadTypeRegistry.playS2C().register(TutorialStepPayload.TYPE, TutorialStepPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(TutorialProgressPayload.TYPE, TutorialProgressPayload.CODEC);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                TutorialCommands.register(dispatcher));
        ServerTickEvents.END_SERVER_TICK.register(TutorialManager::serverTick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                TutorialManager.onDisconnect(server, handler.getPlayer()));

        LOGGER.info("Tutorial Mod geinitialiseerd.");
    }
}
