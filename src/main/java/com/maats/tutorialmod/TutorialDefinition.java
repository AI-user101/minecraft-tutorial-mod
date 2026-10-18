package com.maats.tutorialmod;

import java.util.Map;
import java.util.TreeMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Definitie van een tutorial: naam, bouwgebied en de bouwstappen.
 */
public class TutorialDefinition {

    public final String name;
    /** Inclusieve hoekpunten van het bouwgebied (waarbinnen gebouwd en vastgelegd wordt). */
    public final BlockPos min;
    public final BlockPos max;
    public final TreeMap<Integer, TutorialStep> steps = new TreeMap<>();

    public TutorialDefinition(String name, BlockPos min, BlockPos max) {
        this.name = name;
        this.min = min;
        this.max = max;
    }

    public int stepCount() {
        return steps.size();
    }

    /** Volgnummer (1-based) van de volgende nog niet bestaande stap. */
    public int nextStepNumber() {
        return steps.isEmpty() ? 1 : steps.lastKey() + 1;
    }

    public CompoundTag save(CompoundTag tag) {
        tag.putLong("min", min.asLong());
        tag.putLong("max", max.asLong());
        CompoundTag stepsTag = new CompoundTag();
        for (Map.Entry<Integer, TutorialStep> e : steps.entrySet()) {
            stepsTag.put(String.valueOf(e.getKey()), e.getValue().save(new CompoundTag()));
        }
        tag.put("steps", stepsTag);
        return tag;
    }

    public static TutorialDefinition load(String name, CompoundTag tag) {
        BlockPos min = BlockPos.of(tag.getLongOr("min", 0L));
        BlockPos max = BlockPos.of(tag.getLongOr("max", 0L));
        TutorialDefinition def = new TutorialDefinition(name, min, max);
        CompoundTag stepsTag = tag.getCompoundOrEmpty("steps");
        for (String key : stepsTag.keySet()) {
            try {
                int num = Integer.parseInt(key);
                def.steps.put(num, TutorialStep.load(stepsTag.getCompoundOrEmpty(key)));
            } catch (NumberFormatException ignored) {
            }
        }
        return def;
    }

    /**
     * Leg alle niet-lucht blokken in het bouwgebied vast als verwachte blokken.
     */
    public TutorialStep capture(ServerLevel level, String stepName) {
        TutorialStep step = new TutorialStep(stepName);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockState state = level.getBlockState(pos);
            if (!state.isAir()) {
                step.blocks.put(pos.immutable(), NbtUtils.writeBlockState(state));
            }
        }
        return step;
    }

    public HolderGetter<Block> blockLookup(ServerLevel level) {
        return level.registryAccess().lookupOrThrow(Registries.BLOCK);
    }

    public String describe() {
        return "bouwgebied " + min.getX() + "," + min.getY() + "," + min.getZ()
                + " tot " + max.getX() + "," + max.getY() + "," + max.getZ()
                + ", " + stepCount() + " stappen";
    }
}
