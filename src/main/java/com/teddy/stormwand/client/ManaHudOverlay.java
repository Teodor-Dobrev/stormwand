package com.teddy.stormwand.client;

import com.teddy.stormwand.mana.ClientManaState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public final class ManaHudOverlay {
    private static final int BAR_WIDTH = 74;
    private static final int BAR_HEIGHT = 3;
    private static final long TEXT_FADE_WINDOW_MS = 2200L;

    public static final IGuiOverlay INSTANCE = ManaHudOverlay::render;

    private ManaHudOverlay() {
    }

    private static void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft = gui.getMinecraft();
        if (minecraft.player == null || minecraft.options.hideGui || minecraft.player.isSpectator()) {
            return;
        }

        int maxMana = ClientManaState.getMaxMana();
        if (maxMana <= 0) {
            return;
        }

        int currentMana = Mth.clamp(ClientManaState.getCurrentMana(), 0, maxMana);
        float fullRatio = currentMana / (float) maxMana;
        boolean isFull = currentMana >= maxMana;
        long timeSinceChange = System.currentTimeMillis() - ClientManaState.getLastManaChangeMillis();
        boolean showText = !isFull || timeSinceChange < TEXT_FADE_WINDOW_MS;

        int x = screenWidth - BAR_WIDTH - 10;
        int y = screenHeight - 26;
        if (minecraft.gameMode != null && minecraft.gameMode.hasExperience()) {
            y -= 6;
        }

        int backgroundColor = isFull ? 0x221A2438 : 0x661A2438;
        int fillColor = isFull ? 0x663EA5FF : 0xCC4DB4FF;

        guiGraphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, backgroundColor);
        int fillWidth = Math.max(0, Mth.ceil(fullRatio * BAR_WIDTH));
        if (fillWidth > 0) {
            guiGraphics.fill(x, y, x + fillWidth, y + BAR_HEIGHT, fillColor);
        }

        if (showText) {
            String label = currentMana + "/" + maxMana;
            int textColor = isFull ? 0x77D8F5FF : 0xFFD8F5FF;
            guiGraphics.drawString(minecraft.font, label, x + BAR_WIDTH - minecraft.font.width(label), y - 9, textColor, false);
        }
    }
}