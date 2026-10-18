package com.maats.tutorialmod;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Een bouwstap: alle verwachte blokken (positie -> blockstate in NBT-vorm).
 */
public class TutorialStep {

    public final String name;
    public final Map<BlockPos, CompoundTag> blocks = new HashMap<>();

    public TutorialStep(String name) {
        this.name = name;
    }

    public CompoundTag save(CompoundTag tag) {
        tag.putString("name", name);
        CompoundTag blocksTag = new CompoundTag();
        for (Map.Entry<BlockPos, CompoundTag> e : blocks.entrySet()) {
            blocksTag.put(e.getKey().asLong() + "", e.getValue().copy());
        }
        tag.put("blocks", blocksTag);
        return tag;
    }

    public static TutorialStep load(CompoundTag tag) {
        TutorialStep step = new TutorialStep(tag.getStringOr("name", "Stap"));
        CompoundTag blocksTag = tag.getCompoundOrEmpty("blocks");
        for (String key : blocksTag.keySet()) {
            try {
                long packed = Long.parseLong(key);
                step.blocks.put(BlockPos.of(packed), blocksTag.getCompoundOrEmpty(key).copy());
            } catch (NumberFormatException ignored) {
            }
        }
        return step;
    }

    public static BlockState readState(HolderGetter<Block> lookup, CompoundTag tag) {
        BlockState state = NbtUtils.readBlockState(lookup, tag);
        return state == null ? Blocks.AIR.defaultBlockState() : state;
    }
}
