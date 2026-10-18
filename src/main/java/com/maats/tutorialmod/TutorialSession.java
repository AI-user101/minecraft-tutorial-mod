package com.maats.tutorialmod;

import java.util.ArrayList;
import java.util.List;

import com.maats.tutorialmod.net.TutorialProgressPayload;
import com.maats.tutorialmod.net.TutorialStepPayload;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Sessie van een speler die een tutorial volgt (serverzijde).
 * Controleert elke halve seconde of de stap correct is gebouwd.
 */
public class TutorialSession {

    private final TutorialManager manager;
    public final ServerPlayer player;
    public final TutorialDefinition def;

    private int stepIndex = 0;
    private int lastPlaced = -1;
    private List<BlockPos> lastWrong = List.of();

    public TutorialSession(TutorialManager manager, ServerPlayer player, TutorialDefinition def) {
        this.manager = manager;
        this.player = player;
        this.def = def;
        int saved = manager.data().getProgress(player.getUUID(), def.name);
        if (saved >= 0 && saved < def.steps.size()) {
            this.stepIndex = saved;
        }
    }

    public int stepIndex() {
        return stepIndex;
    }

    public TutorialStep currentStep() {
        return def.steps.get(stepNumber());
    }

    /** Volgnummer (1-based) van de huidige stap. */
    public int stepNumber() {
        return stepIndex + 1;
    }

    private ServerLevel level() {
        return player.level();
    }

    public void start() {
        player.sendSystemMessage(Component.literal("Tutorial gestart: " + def.name
                + " (stap " + stepNumber() + "/" + def.stepCount() + ")"));
        sendStep(true);
    }

    public void skip(int targetStepNumber) {
        stepIndex = Math.clamp(targetStepNumber - 1, 0, def.steps.size() - 1);
        manager.data().saveProgress(player.getUUID(), def.name, stepIndex);
        player.sendSystemMessage(Component.literal("Doorgespoeld naar stap " + stepNumber() + ": "
                + currentStep().name));
        sendStep(true);
    }

    public void sendStep(boolean giveMaterials) {
        TutorialStep step = currentStep();
        List<TutorialStepPayload.Entry> entries = new ArrayList<>();
        for (java.util.Map.Entry<BlockPos, CompoundTag> e : step.blocks.entrySet()) {
            entries.add(new TutorialStepPayload.Entry(e.getKey(), e.getValue()));
        }
        ServerPlayNetworking.send(player, new TutorialStepPayload(
                def.name, stepIndex, def.stepCount(), step.name, entries));

        if (giveMaterials && level().getGameRules().get(TutorialMod.RULE_GIVE_MATERIALS)) {
            MaterialsGiver.give(player, step, def.blockLookup(level()), level());
        }
    }

    public void sendClear(boolean completed) {
        ServerPlayNetworking.send(player, new TutorialStepPayload(def.name, -1, 0, "", List.of()));
        if (completed) {
            player.sendSystemMessage(Component.literal("Tutorial voltooid: " + def.name
                    + ". Goed gedaan!"));
        } else {
            player.sendSystemMessage(Component.literal("Tutorial gestopt."));
        }
    }

    public void tick() {
        if (player.isRemoved()) {
            manager.removeSession(player);
            return;
        }
        TutorialStep step = currentStep();
        if (step == null || step.blocks.isEmpty()) {
            finish();
            return;
        }
        ServerLevel level = level();

        int ok = 0;
        List<BlockPos> wrong = new ArrayList<>();
        for (java.util.Map.Entry<BlockPos, CompoundTag> e : step.blocks.entrySet()) {
            BlockState expected = TutorialStep.readState(def.blockLookup(level), e.getValue());
            BlockState world = level.getBlockState(e.getKey());
            if (world == expected) {
                ok++;
            } else if (!world.isAir()) {
                wrong.add(e.getKey());
            }
        }
        int total = step.blocks.size();
        if (ok == total) {
            stepDone();
            return;
        }
        if (ok != lastPlaced || !wrong.equals(lastWrong)) {
            lastPlaced = ok;
            lastWrong = wrong;
            ServerPlayNetworking.send(player,
                    new TutorialProgressPayload(stepIndex, ok, total, wrong));
        }
    }

    private void stepDone() {
        player.sendSystemMessage(Component.literal("Stap " + stepNumber() + " voltooid: "
                + currentStep().name));
        if (stepIndex + 1 < def.steps.size()) {
            stepIndex++;
            manager.data().saveProgress(player.getUUID(), def.name, stepIndex);
            sendStep(true);
        } else {
            finish();
        }
    }

    private void finish() {
        manager.data().saveProgress(player.getUUID(), def.name, Math.max(def.steps.size() - 1, 0));
        manager.removeSession(player);
        sendClear(true);
    }
}
