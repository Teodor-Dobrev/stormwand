package com.teddy.stormwand.client;

import com.teddy.stormwand.spell.WandSpell;
import com.teddy.stormwand.spell.WandSpellData;
import com.teddy.stormwand.util.RomanNumerals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.List;

public final class SpellSelectorOverlay {
    public static final IGuiOverlay INSTANCE = SpellSelectorOverlay::render;

    private SpellSelectorOverlay() {
    }

    private static void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft = gui.getMinecraft();
        if (!SpellSelectorClientHandler.isSelectorActive(minecraft) || minecraft.player == null) {
            return;
        }

        SpellSelectorClientHandler.HeldWand heldWand = SpellSelectorClientHandler.getHeldWand(minecraft.player);
        if (heldWand == null) {
            return;
        }

        List<WandSpellData.SpellLevelEntry> spells = WandSpellData.getInstalledSpells(heldWand.stack());
        if (spells.isEmpty()) {
            return;
        }

        WandSpell selectedSpell = WandSpellData.getSelectedSpell(heldWand.stack());
        int selectedLevel = WandSpellData.getSpellLevel(heldWand.stack(), selectedSpell.id());
        List<Component> detailLines = selectedSpell.getTooltipLines(selectedLevel);

        int width = 154;
        int headerHeight = 16;
        int rowHeight = 14;
        int detailHeight = detailLines.size() * 10;
        int height = headerHeight + spells.size() * rowHeight + detailHeight + 16;
        int x = screenWidth - width - 12;
        int y = Mth.clamp((screenHeight - height) / 2, 10, screenHeight - height - 10);

        guiGraphics.fill(x, y, x + width, y + height, 0xB0141C2E);
        guiGraphics.fill(x, y, x + width, y + 1, 0xCC4DB4FF);
        guiGraphics.drawString(minecraft.font, Component.translatable("overlay.stormwand.spell_selector"), x + 8, y + 5, 0xFFDFF6FF, false);

        int rowY = y + headerHeight;
        for (WandSpellData.SpellLevelEntry entry : spells) {
            boolean selected = entry.spell().id().equals(selectedSpell.id());
            int rowColor = selected ? 0xAA1F3F66 : 0x66203044;
            guiGraphics.fill(x + 6, rowY, x + width - 6, rowY + 12, rowColor);
            guiGraphics.drawString(
                    minecraft.font,
                    Component.literal(entry.spell().displayName().getString() + " " + RomanNumerals.toRoman(entry.level())),
                    x + 10,
                    rowY + 2,
                    selected ? 0xFFFFFFFF : 0xFFBFD9FF,
                    false
            );
            rowY += rowHeight;
        }

        guiGraphics.drawString(minecraft.font, Component.translatable("overlay.stormwand.selected_spell", selectedSpell.displayName(), RomanNumerals.toRoman(selectedLevel)), x + 8, rowY + 2, 0xFF8FE9FF, false);
        int detailY = rowY + 14;
        for (Component line : detailLines) {
            guiGraphics.drawString(minecraft.font, line, x + 8, detailY, 0xFFCBDCFF, false);
            detailY += 10;
        }

        guiGraphics.drawString(
                minecraft.font,
                Component.translatable("overlay.stormwand.selector_hint"),
                x + 8,
                y + height - 10,
                0xAA9FB7D8,
                false
        );
    }
}