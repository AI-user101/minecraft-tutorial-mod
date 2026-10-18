package com.maats.tutorialmod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Voortgangsweergave: infoblok linksboven en voortgangsbalk boven de hotbar.
 */
public final class TutorialHud {

    private TutorialHud() {
    }

    public static void render(GuiGraphics gui) {
        if (!TutorialClientState.isActive()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        // Info linksboven
        gui.drawString(font, "Tutorial: " + TutorialClientState.tutorialName(), 4, 4, 0xFFFFFF, true);
        gui.drawString(font, "Stap " + TutorialClientState.stepNumber() + "/" + TutorialClientState.stepCount()
                + ": " + TutorialClientState.stepName(), 4, 14, 0xFFFFAA, true);
        int wrong = TutorialClientState.wrongCount();
        String progress = TutorialClientState.placed() + "/" + TutorialClientState.total() + " geplaatst";
        if (wrong > 0) {
            progress += " | " + wrong + " fout" + (wrong > 1 ? "e blokken" : " blok");
        }
        gui.drawString(font, progress, 4, 24, 0x55FF55, true);

        // Voortgangsbalk boven de hotbar (net als de xp-balk)
        int w = gui.guiWidth();
        int h = gui.guiHeight();
        int barW = 182;
        int x = (w - barW) / 2;
        int y = h - 32;
        gui.fill(x, y, x + barW, y + 5, 0xC0000000);
        float frac = TutorialClientState.total() == 0
                ? 0f
                : TutorialClientState.placed() / (float) TutorialClientState.total();
        int fill = (int) ((barW - 2) * Math.min(frac, 1f));
        int color = wrong > 0 ? 0xFFD9534F : 0xFF44AA44;
        if (fill > 0) {
            gui.fill(x + 1, y + 1, x + 1 + fill, y + 4, color);
        }
    }
}
