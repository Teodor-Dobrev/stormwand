package com.teddy.stormwand.client;

import com.teddy.stormwand.StormWandMod;
import com.teddy.stormwand.item.StormWandItem;
import com.teddy.stormwand.network.SelectSpellC2SPacket;
import com.teddy.stormwand.network.StormWandNetwork;
import com.teddy.stormwand.spell.WandSpellData;
import com.teddy.stormwand.util.RomanNumerals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Mod.EventBusSubscriber(modid = StormWandMod.MOD_ID, value = Dist.CLIENT)
public final class SpellSelectorClientHandler {
    private SpellSelectorClientHandler() {
    }

    public static boolean isSelectorActive(Minecraft minecraft) {
        return minecraft.player != null
                && minecraft.screen == null
                && ClientKeyMappings.SPELL_SELECTOR.isDown()
                && getHeldWand(minecraft.player) != null;
    }

    @Nullable
    public static HeldWand getHeldWand(LocalPlayer player) {
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() instanceof StormWandItem) {
            return new HeldWand(InteractionHand.MAIN_HAND, mainHand);
        }

        ItemStack offHand = player.getOffhandItem();
        if (offHand.getItem() instanceof StormWandItem) {
            return new HeldWand(InteractionHand.OFF_HAND, offHand);
        }

        return null;
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!isSelectorActive(minecraft) || minecraft.player == null) {
            return;
        }

        HeldWand heldWand = getHeldWand(minecraft.player);
        if (heldWand == null) {
            return;
        }

        List<WandSpellData.SpellLevelEntry> installedSpells = WandSpellData.getInstalledSpells(heldWand.stack());
        if (installedSpells.size() < 2) {
            return;
        }

        int currentIndex = 0;
        for (int index = 0; index < installedSpells.size(); index++) {
            if (installedSpells.get(index).spell().id().equals(WandSpellData.getSelectedSpellId(heldWand.stack()))) {
                currentIndex = index;
                break;
            }
        }

        int direction = event.getScrollDelta() > 0 ? -1 : 1;
        int nextIndex = Math.floorMod(currentIndex + direction, installedSpells.size());
        WandSpellData.SpellLevelEntry nextSpell = installedSpells.get(nextIndex);
        WandSpellData.setSelectedSpell(heldWand.stack(), nextSpell.spell().id());
        StormWandNetwork.CHANNEL.sendToServer(new SelectSpellC2SPacket(heldWand.hand(), nextSpell.spell().id()));
        minecraft.player.displayClientMessage(
                Component.translatable("message.stormwand.selected_spell", nextSpell.spell().displayName(), RomanNumerals.toRoman(nextSpell.level())),
                true
        );
        event.setCanceled(true);
    }

    public record HeldWand(InteractionHand hand, ItemStack stack) {
    }
}