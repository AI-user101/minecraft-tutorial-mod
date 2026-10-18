package com.maats.tutorialmod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Wereldopslag: alle tutorialdefinities en de voortgang per speler.
 */
public class TutorialSaveData extends SavedData {

    public static final String DATA_NAME = "tutorialmod_tutorials";

    public static final SavedDataType<TutorialSaveData> TYPE = new SavedDataType<>(
            DATA_NAME,
            TutorialSaveData::new,
            CompoundTag.CODEC.xmap(TutorialSaveData::fromTag, TutorialSaveData::toTag),
            DataFixTypes.SAVED_DATA_RANDOM_SEQUENCES);

    public final Map<String, TutorialDefinition> tutorials = new HashMap<>();
    public final Map<UUID, PlayerProgress> progress = new HashMap<>();

    public static class PlayerProgress {
        public String tutorial;
        public int step; // 0-based index van de huidige stap

        public PlayerProgress(String tutorial, int step) {
            this.tutorial = tutorial;
            this.step = step;
        }
    }

    public TutorialSaveData() {
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        CompoundTag tuts = new CompoundTag();
        for (Map.Entry<String, TutorialDefinition> e : tutorials.entrySet()) {
            tuts.put(e.getKey(), e.getValue().save(new CompoundTag()));
        }
        tag.put("tutorials", tuts);

        CompoundTag prog = new CompoundTag();
        for (Map.Entry<UUID, PlayerProgress> e : progress.entrySet()) {
            CompoundTag p = new CompoundTag();
            p.putString("tutorial", e.getValue().tutorial);
            p.putInt("step", e.getValue().step);
            prog.put(e.getKey().toString(), p);
        }
        tag.put("progress", prog);
        return tag;
    }

    public static TutorialSaveData fromTag(CompoundTag tag) {
        TutorialSaveData data = new TutorialSaveData();
        CompoundTag tuts = tag.getCompoundOrEmpty("tutorials");
        for (String key : tuts.keySet()) {
            data.tutorials.put(key, TutorialDefinition.load(key, tuts.getCompoundOrEmpty(key)));
        }
        CompoundTag prog = tag.getCompoundOrEmpty("progress");
        for (String key : prog.keySet()) {
            CompoundTag p = prog.getCompoundOrEmpty(key);
            try {
                data.progress.put(UUID.fromString(key),
                        new PlayerProgress(p.getStringOr("tutorial", ""), p.getIntOr("step", 0)));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return data;
    }

    public void saveProgress(UUID player, String tutorial, int step) {
        progress.put(player, new PlayerProgress(tutorial, step));
        setDirty();
    }

    public int getProgress(UUID player, String tutorial) {
        PlayerProgress p = progress.get(player);
        if (p != null && p.tutorial.equals(tutorial)) {
            return p.step;
        }
        return 0;
    }

    public static TutorialSaveData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }
}
