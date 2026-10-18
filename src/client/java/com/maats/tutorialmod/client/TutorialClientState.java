package com.maats.tutorialmod.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.maats.tutorialmod.TutorialStep;
import com.maats.tutorialmod.net.TutorialProgressPayload;
import com.maats.tutorialmod.net.TutorialStepPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;

/**
 * Clientstatus: huidige stap, voortgang en de ghost blocks
 * (voorbeeldblokken via client-only item display entities).
 */
public final class TutorialClientState {

    /** Schaalgroei van item displays: een blok-item is standaard 1/4 blok groot. */
    private static final float GHOST_SCALE = 4.0f;
    private static final float WRONG_SCALE = 4.08f;
    /** Veiligheidslimiet op het aantal gelijktijdige ghosts. */
    private static final int MAX_GHOSTS = 4000;

    private static String tutorialName = "";
    private static int stepIndex = -1;
    private static int stepCount = 0;
    private static String stepName = "";
    private static int placed = 0;
    private static int total = 0;

    private static final List<TutorialStepPayload.Entry> entries = new ArrayList<>();
    private static final Set<BlockPos> wrong = new HashSet<>();

    private static final List<Integer> ghostIds = new ArrayList<>();
    private static final List<Integer> wrongIds = new ArrayList<>();
    private static int nextGhostId = 1_950_000_000;

    private TutorialClientState() {
    }

    public static boolean isActive() {
        return stepIndex >= 0;
    }

    public static String tutorialName() {
        return tutorialName;
    }

    public static int stepNumber() {
        return stepIndex + 1;
    }

    public static int stepCount() {
        return stepCount;
    }

    public static String stepName() {
        return stepName;
    }

    public static int placed() {
        return placed;
    }

    public static int total() {
        return total;
    }

    public static int wrongCount() {
        return wrong.size();
    }

    public static void clear() {
        tutorialName = "";
        stepIndex = -1;
        stepCount = 0;
        stepName = "";
        placed = 0;
        total = 0;
        entries.clear();
        wrong.clear();
        removeAllEntities();
    }

    public static void onStep(TutorialStepPayload payload) {
        clearGhosts();
        clearWrong();

        tutorialName = payload.tutorialName();
        stepIndex = payload.stepIndex();
        stepCount = payload.stepCount();
        stepName = payload.stepName();
        placed = 0;
        total = payload.entries().size();
        wrong.clear();

        if (stepIndex < 0) {
            stepIndex = -1;
            entries.clear();
            return;
        }

        entries.clear();
        entries.addAll(payload.entries());
        spawnGhosts();
    }

    public static void onProgress(TutorialProgressPayload payload) {
        if (!isActive()) {
            return;
        }
        placed = payload.placed();
        total = payload.total();

        Set<BlockPos> newWrong = new HashSet<>(payload.wrong());
        if (!newWrong.equals(wrong)) {
            wrong.clear();
            wrong.addAll(newWrong);
            clearWrong();
            for (BlockPos pos : wrong) {
                spawnDisplay(pos, Items.RED_STAINED_GLASS, WRONG_SCALE, wrongIds);
            }
        }

        // Ghosts herberekenen: geplaatste blokken hoeven niet langer te zweven.
        spawnGhosts();
    }

    private static void spawnGhosts() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }

        int spawned = 0;
        List<Integer> newIds = new ArrayList<>();
        for (TutorialStepPayload.Entry e : entries) {
            if (spawned >= MAX_GHOSTS) {
                break;
            }
            BlockState expected = TutorialStep.readState(
                    level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BLOCK),
                    e.stateTag());
            BlockState world = level.getBlockState(e.pos());
            if (world == expected || wrong.contains(e.pos())) {
                continue; // al correct geplaatst, of er staat al iets fouts
            }
            Item item = expected.getBlock().asItem();
            if (item == Items.AIR) {
                continue;
            }
            int id = spawnDisplay(e.pos(), item, GHOST_SCALE, null);
            if (id != -1) {
                newIds.add(id);
                spawned++;
            }
        }
        clearGhosts();
        ghostIds.addAll(newIds);
    }

    private static void clearGhosts() {
        removeEntities(ghostIds);
    }

    private static void clearWrong() {
        removeEntities(wrongIds);
    }

    private static void removeAllEntities() {
        clearGhosts();
        clearWrong();
    }

    /**
     * Spawnt een client-only item display op de positie van een blok.
     * Deze entiteiten bestaan alleen bij de client en worden niet opgeslagen.
     */
    private static int spawnDisplay(BlockPos pos, Item item, float scale, List<Integer> extraIds) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return -1;
        }
        Display.ItemDisplay display = new Display.ItemDisplay(EntityType.ITEM_DISPLAY, level);

        CompoundTag tag = new CompoundTag();
        tag.putString("id", "minecraft:item_display");

        ListTag posTag = new ListTag();
        posTag.add(DoubleTag.valueOf(pos.getX() + 0.5));
        posTag.add(DoubleTag.valueOf(pos.getY() + 0.5));
        posTag.add(DoubleTag.valueOf(pos.getZ() + 0.5));
        tag.put("Pos", posTag);

        CompoundTag itemTag = new CompoundTag();
        itemTag.putString("id", BuiltInRegistries.ITEM.getKey(item).toString());
        itemTag.putByte("count", (byte) 1);
        tag.put("item", itemTag);
        tag.putString("item_display", "none");

        CompoundTag transformation = new CompoundTag();
        transformation.put("translation", vec3(0f, 0f, 0f));
        transformation.put("scale", vec3(scale, scale, scale));
        transformation.put("left_rotation", quat());
        transformation.put("right_rotation", quat());
        tag.put("transformation", transformation);

        CompoundTag brightness = new CompoundTag();
        brightness.putByte("sky", (byte) 15);
        brightness.putByte("block", (byte) 15);
        tag.put("brightness", brightness);

        tag.putFloat("view_range", 1.5f);
        tag.putFloat("shadow_radius", 0.0f);
        tag.putFloat("shadow_strength", 0.0f);

        display.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
        display.setId(nextGhostId++);
        display.setPosRaw(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);

        level.addEntity(display);
        if (extraIds != null) {
            extraIds.add(display.getId());
        }
        return display.getId();
    }

    private static ListTag vec3(float x, float y, float z) {
        ListTag list = new ListTag();
        list.add(FloatTag.valueOf(x));
        list.add(FloatTag.valueOf(y));
        list.add(FloatTag.valueOf(z));
        return list;
    }

    private static ListTag quat() {
        ListTag list = new ListTag();
        list.add(FloatTag.valueOf(0f));
        list.add(FloatTag.valueOf(0f));
        list.add(FloatTag.valueOf(0f));
        list.add(FloatTag.valueOf(1f));
        return list;
    }

    private static void removeEntities(List<Integer> ids) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        for (int id : ids) {
            if (level != null) {
                level.removeEntity(id, Entity.RemovalReason.DISCARDED);
            }
        }
        ids.clear();
    }
}
