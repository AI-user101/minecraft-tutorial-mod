package com.maats.tutorialmod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;

/**
 * Alle /tutorial commando's.
 */
public class TutorialCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tutorial")

                .then(Commands.literal("start")
                        .then(Commands.argument("naam", com.mojang.brigadier.arguments.StringArgumentType.word())
                                .executes(ctx -> start(ctx))))

                .then(Commands.literal("make")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("step")
                                .then(Commands.argument("nummer", IntegerArgumentType.integer(1))
                                        .then(Commands.argument("naam",
                                                com.mojang.brigadier.arguments.StringArgumentType.word())
                                                .executes(ctx -> makeStep(ctx)))))
                        .then(Commands.argument("naam",
                                com.mojang.brigadier.arguments.StringArgumentType.word())
                                .then(Commands.argument("x", IntegerArgumentType.integer())
                                .then(Commands.argument("z", IntegerArgumentType.integer())
                                        .then(Commands.argument("grondDiepte", IntegerArgumentType.integer(1))
                                                .then(Commands.argument("luchtHoogte", IntegerArgumentType.integer(1, 256))
                                                        .executes(ctx -> makeTutorial(ctx))))))))

                .then(Commands.literal("edit")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("step", IntegerArgumentType.integer(1))
                                .then(Commands.argument("naam",
                                        com.mojang.brigadier.arguments.StringArgumentType.word())
                                        .executes(ctx -> editStep(ctx)))))

                .then(Commands.literal("remove")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("step").executes(ctx -> removeStep(ctx))))

                .then(Commands.literal("skip")
                        .then(Commands.argument("step", IntegerArgumentType.integer(1))
                                .executes(ctx -> skip(ctx))))

                .then(Commands.literal("stop").executes(ctx -> stop(ctx)))
                .then(Commands.literal("list").executes(ctx -> list(ctx))));
    }

    // ---------------------------------------------------------------- start

    private static int start(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack src = ctx.getSource();
        ServerPlayer player = src.getPlayerOrException();
        String name = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "naam");
        TutorialDefinition def = TutorialManager.get(src.getServer()).data().tutorials.get(name);
        if (def == null) {
            src.sendFailure(Component.literal("Tutorial '" + name + "' bestaat niet. Gebruik /tutorial list."));
            return 0;
        }
        if (def.steps.isEmpty()) {
            src.sendFailure(Component.literal("Tutorial '" + name + "' heeft nog geen stappen."));
            return 0;
        }
        TutorialManager.get(src.getServer()).startSession(player, def);
        return 1;
    }

    // ---------------------------------------------------------- make (gebied)

    private static int makeTutorial(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack src = ctx.getSource();
        ServerPlayer player = src.getPlayerOrException();
        ServerLevel level = src.getLevel();

        String name = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "naam");
        int x = IntegerArgumentType.getInteger(ctx, "x");
        int z = IntegerArgumentType.getInteger(ctx, "z");
        int groundDepth = IntegerArgumentType.getInteger(ctx, "grondDiepte");
        int airHeight = IntegerArgumentType.getInteger(ctx, "luchtHoogte");

        TutorialManager manager = TutorialManager.get(src.getServer());
        TutorialSaveData data = manager.data();

        if (data.tutorials.containsKey(name)) {
            src.sendFailure(Component.literal("Tutorial '" + name + "' bestaat al. Kies een andere naam."));
            return 0;
        }

        // Gras zit op het blok waar de speler op staat.
        int grassY = player.blockPosition().getY() - 1;
        int minY = Math.max(level.getMinY(), grassY + 1 - groundDepth);
        int maxY = Math.min(level.getMinY() + level.getHeight(), grassY + airHeight);

        // 16x16 platform, (x, z) is het midden.
        for (int dx = -8; dx < 8; dx++) {
            for (int dz = -8; dz < 8; dz++) {
                for (int i = 0; i < groundDepth; i++) {
                    int y = grassY - i;
                    if (y < level.getMinY() || y >= level.getMinY() + level.getHeight()) {
                        continue;
                    }
                    BlockPos pos = new BlockPos(x + dx, y, z + dz);
                    var state = (i == 0 ? Blocks.GRASS_BLOCK : Blocks.DIRT).defaultBlockState();
                    level.setBlock(pos, state, 3);
                }
                // Lucht erboven
                for (int y = grassY + 1; y <= grassY + airHeight; y++) {
                    if (y >= level.getMinY() + level.getHeight()) {
                        break;
                    }
                    BlockPos pos = new BlockPos(x + dx, y, z + dz);
                    if (!level.getBlockState(pos).isAir()) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }

        BlockPos min = new BlockPos(x - 8, grassY + 1, z - 8);
        BlockPos max = new BlockPos(x + 7, grassY + airHeight, z + 7);
        TutorialDefinition def = new TutorialDefinition(name, min, max);
        data.tutorials.put(name, def);
        data.setDirty();
        manager.setLastCreated(player.getUUID(), name);

        src.sendSuccess(() -> Component.literal("Tutorial '" + name + "' gemaakt met bouwplatform: "
                + def.describe()), false);
        src.sendSuccess(() -> Component.literal(
                "Bouw nu iets in het gebied en leg het vast met /tutorial make step <nummer> <naam>."), false);
        return 1;
    }

    // ------------------------------------------------------------ make step

    private static int makeStep(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack src = ctx.getSource();
        ServerPlayer player = src.getPlayerOrException();
        TutorialManager manager = TutorialManager.get(src.getServer());
        TutorialDefinition def = manager.lastCreatedTutorial(player, "make step");
        if (def == null) {
            src.sendFailure(Component.literal(
                    "Geen tutorial bekend. Maak eerst een tutorial met /tutorial make."));
            return 0;
        }
        int number = IntegerArgumentType.getInteger(ctx, "nummer");
        String name = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "naam");
        if (number != def.nextStepNumber()) {
            src.sendFailure(Component.literal("Stap " + number + " kan niet worden toegevoegd. "
                    + "Verwacht wordt stap " + def.nextStepNumber() + ". Bestaande stappen pas je aan met /tutorial edit."));
            return 0;
        }
        TutorialStep step = def.capture(player.level(), name);
        if (step.blocks.isEmpty()) {
            src.sendFailure(Component.literal(
                    "Het bouwgebied is leeg. Bouw eerst iets en leg daarna de stap vast."));
            return 0;
        }
        def.steps.put(number, step);
        manager.data().setDirty();
        src.sendSuccess(() -> Component.literal("Stap " + number + " ('" + name + "') vastgelegd met "
                + step.blocks.size() + " blokken. Volgende: /tutorial make step " + (number + 1) + " <naam>."), false);
        return 1;
    }

    // ------------------------------------------------------------------ edit

    private static int editStep(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack src = ctx.getSource();
        ServerPlayer player = src.getPlayerOrException();
        TutorialManager manager = TutorialManager.get(src.getServer());
        TutorialDefinition def = manager.lastCreatedTutorial(player, "edit");
        if (def == null) {
            src.sendFailure(Component.literal(
                    "Geen tutorial bekend. Maak eerst een tutorial met /tutorial make."));
            return 0;
        }
        int number = IntegerArgumentType.getInteger(ctx, "step");
        String name = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "naam");
        TutorialStep existing = def.steps.get(number);
        if (existing == null) {
            src.sendFailure(Component.literal("Stap " + number + " bestaat niet in tutorial '" + def.name + "'."));
            return 0;
        }
        TutorialStep step = def.capture(player.level(), name);
        if (step.blocks.isEmpty()) {
            src.sendFailure(Component.literal("Het bouwgebied is leeg; stap niet aangepast."));
            return 0;
        }
        def.steps.put(number, step);
        manager.data().setDirty();
        src.sendSuccess(() -> Component.literal("Stap " + number + " opnieuw vastgelegd als '" + name
                + "' met " + step.blocks.size() + " blokken."), false);
        return 1;
    }

    // ---------------------------------------------------------------- remove

    private static int removeStep(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack src = ctx.getSource();
        ServerPlayer player = src.getPlayerOrException();
        TutorialManager manager = TutorialManager.get(src.getServer());
        TutorialDefinition def = manager.lastCreatedTutorial(player, "remove step");
        if (def == null) {
            src.sendFailure(Component.literal("Geen tutorial bekend."));
            return 0;
        }
        if (def.steps.isEmpty()) {
            src.sendFailure(Component.literal("Tutorial '" + def.name + "' heeft geen stappen."));
            return 0;
        }
        int last = def.steps.lastKey();
        def.steps.remove(last);
        manager.data().setDirty();
        src.sendSuccess(() -> Component.literal("Stap " + last + " verwijderd uit tutorial '" + def.name + "'."), false);
        return 1;
    }

    // ------------------------------------------------------------------ skip

    private static int skip(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack src = ctx.getSource();
        ServerPlayer player = src.getPlayerOrException();
        TutorialSession session = TutorialManager.get(src.getServer()).session(player);
        if (session == null) {
            src.sendFailure(Component.literal("Je volgt momenteel geen tutorial. Start er een met /tutorial start <naam>."));
            return 0;
        }
        int step = IntegerArgumentType.getInteger(ctx, "step");
        if (step > session.def.stepCount()) {
            src.sendFailure(Component.literal("Deze tutorial heeft maar " + session.def.stepCount() + " stappen."));
            return 0;
        }
        session.skip(step);
        return 1;
    }

    // ------------------------------------------------------------------ stop

    private static int stop(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack src = ctx.getSource();
        ServerPlayer player = src.getPlayerOrException();
        TutorialSession session = TutorialManager.get(src.getServer()).session(player);
        if (session == null) {
            src.sendFailure(Component.literal("Je volgt momenteel geen tutorial."));
            return 0;
        }
        TutorialManager.get(src.getServer()).stopSession(player, false);
        return 1;
    }

    // ------------------------------------------------------------------ list

    private static int list(CommandContext<CommandSourceStack> ctx) {
        var tutorials = TutorialManager.get(ctx.getSource().getServer()).data().tutorials;
        if (tutorials.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("Er zijn nog geen tutorials."));
            return 0;
        }
        ctx.getSource().sendSystemMessage(Component.literal("Tutorials:"));
        for (TutorialDefinition def : tutorials.values()) {
            ctx.getSource().sendSystemMessage(Component.literal(" - " + def.name + " (" + def.describe() + ")"));
        }
        return tutorials.size();
    }
}
