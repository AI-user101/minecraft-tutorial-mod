package com.maats.tutorialmod;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Beheert alle tutorialsessies (serverzijde), gekoppeld aan de spelers.
 */
public class TutorialManager {

    private static final Map<MinecraftServer, TutorialManager> INSTANCES = new WeakHashMap<>();

    private final MinecraftServer server;
    private final Map<UUID, TutorialSession> sessions = new ConcurrentHashMap<>();
    /** Laatst aangemaakte tutorial per maker (voor make step / edit / remove zonder tutorialnaam). */
    private final Map<UUID, String> lastCreated = new ConcurrentHashMap<>();

    private TutorialManager(MinecraftServer server) {
        this.server = server;
    }

    public static TutorialManager get(MinecraftServer server) {
        return INSTANCES.computeIfAbsent(server, TutorialManager::new);
    }

    public MinecraftServer server() {
        return server;
    }

    public TutorialSaveData data() {
        return TutorialSaveData.get(server);
    }

    public TutorialSession session(ServerPlayer player) {
        return sessions.get(player.getUUID());
    }

    public void setLastCreated(UUID player, String tutorialName) {
        lastCreated.put(player, tutorialName);
    }

    public String lastCreated(UUID player) {
        return lastCreated.get(player);
    }

    public TutorialDefinition lastCreatedTutorial(ServerPlayer player, String commandName) {
        String name = lastCreated.get(player.getUUID());
        if (name != null) {
            TutorialDefinition def = data().tutorials.get(name);
            if (def != null) {
                return def;
            }
        }
        if (data().tutorials.size() == 1) {
            return data().tutorials.values().iterator().next();
        }
        return null;
    }

    public TutorialSession startSession(ServerPlayer player, TutorialDefinition def) {
        TutorialSession session = new TutorialSession(this, player, def);
        sessions.put(player.getUUID(), session);
        session.start();
        return session;
    }

    public void stopSession(ServerPlayer player, boolean completed) {
        TutorialSession session = sessions.remove(player.getUUID());
        if (session != null) {
            session.sendClear(completed);
        }
    }

    public void removeSession(ServerPlayer player) {
        sessions.remove(player.getUUID());
    }

    public static void serverTick(MinecraftServer server) {
        if (server.getTickCount() % 10 == 0) {
            TutorialManager manager = get(server);
            for (TutorialSession session : manager.sessions.values()) {
                session.tick();
            }
        }
    }

    public static void onDisconnect(MinecraftServer server, ServerPlayer player) {
        get(server).sessions.remove(player.getUUID());
    }
}
