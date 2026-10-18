package com.maats.tutorialmod;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.nbt.CompoundTag;

/**
 * Geeft de speler de materialen die nog nodig zijn voor de huidige stap.
 * In-/uitschakelbaar via de gamerule tutorial_give_materials.
 */
public final class MaterialsGiver {

    private MaterialsGiver() {
    }

    public static void give(ServerPlayer player, TutorialStep step, HolderGetter<Block> lookup, ServerLevel level) {
        Map<Item, Integer> needed = new LinkedHashMap<>();
        for (Map.Entry<BlockPos, CompoundTag> e : step.blocks.entrySet()) {
            BlockState expected = TutorialStep.readState(lookup, e.getValue());
            BlockState world = level.getBlockState(e.getKey());
            if (world != expected) {
                Item item = expected.getBlock().asItem();
                if (item != Items.AIR) {
                    needed.merge(item, 1, Integer::sum);
                }
            }
        }
        if (needed.isEmpty()) {
            return;
        }
        StringBuilder summary = new StringBuilder();
        for (Map.Entry<Item, Integer> e : needed.entrySet()) {
            giveItem(player, e.getKey(), e.getValue());
            if (summary.length() > 0) {
                summary.append(", ");
            }
            summary.append(e.getValue()).append("x ")
                    .append(BuiltInRegistries.ITEM.getKey(e.getKey()).getPath());
        }
        player.sendSystemMessage(Component.literal("Materialen voor deze stap: " + summary));
    }

    private static void giveItem(ServerPlayer player, Item item, int count) {
        int left = count;
        while (left > 0) {
            int size = Math.min(left, item.getDefaultMaxStackSize());
            ItemStack stack = new ItemStack(item, size);
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false, false);
            }
            left -= size;
        }
    }
}
