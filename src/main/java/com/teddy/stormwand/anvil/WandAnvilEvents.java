package com.teddy.stormwand.anvil;

import com.teddy.stormwand.StormWandMod;
import com.teddy.stormwand.item.ModItems;
import com.teddy.stormwand.item.SpellBookItem;
import com.teddy.stormwand.item.StormWandItem;
import com.teddy.stormwand.spell.WandSpell;
import com.teddy.stormwand.spell.WandSpellData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = StormWandMod.MOD_ID)
public final class WandAnvilEvents {
    private WandAnvilEvents() {
    }

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();

        if (left.getItem() == ModItems.SPELL_BOOK.get() && right.getItem() == ModItems.SPELL_BOOK.get()) {
            mergeSpellBooks(event, left, right);
            return;
        }

        if (left.getItem() instanceof StormWandItem && right.getItem() == ModItems.SPELL_BOOK.get()) {
            applySpellBookToWand(event, left, right);
            return;
        }

        if (left.getItem() instanceof StormWandItem && right.is(Items.AMETHYST_SHARD)) {
            repairWand(event, left, right);
        }
    }

    private static void mergeSpellBooks(AnvilUpdateEvent event, ItemStack left, ItemStack right) {
        ResourceLocation leftSpellId = SpellBookItem.getSpellId(left);
        ResourceLocation rightSpellId = SpellBookItem.getSpellId(right);
        int leftLevel = SpellBookItem.getSpellLevel(left);
        int rightLevel = SpellBookItem.getSpellLevel(right);
        WandSpell spell = SpellBookItem.getSpell(left);

        if (!leftSpellId.equals(rightSpellId) || leftLevel != rightLevel || leftLevel >= spell.getMaxLevel()) {
            return;
        }

        ItemStack result = SpellBookItem.createFor(spell, leftLevel + 1, ModItems.SPELL_BOOK.get());
        event.setOutput(result);
        event.setCost(1);
        event.setMaterialCost(1);
    }

    private static void applySpellBookToWand(AnvilUpdateEvent event, ItemStack wand, ItemStack spellBook) {
        ItemStack result = wand.copy();
        WandSpellData.ensureDefaults(result);

        WandSpell spell = SpellBookItem.getSpell(spellBook);
        ResourceLocation spellId = spell.id();
        int bookLevel = SpellBookItem.getSpellLevel(spellBook);
        int currentLevel = WandSpellData.getSpellLevel(result, spellId);
        int upgradedLevel = getUpgradedSpellLevel(currentLevel, bookLevel, spell.getMaxLevel());
        if (upgradedLevel <= 0) {
            return;
        }

        WandSpellData.setSpellLevel(result, spellId, upgradedLevel);
        if (!WandSpellData.hasSpell(result, WandSpellData.getSelectedSpellId(result))) {
            WandSpellData.setSelectedSpell(result, spellId);
        }
        result.setRepairCost(wand.getBaseRepairCost());
        event.setOutput(result);
        event.setCost(1);
        event.setMaterialCost(1);
    }

    private static void repairWand(AnvilUpdateEvent event, ItemStack wand, ItemStack shards) {
        if (!wand.isDamageableItem() || wand.getDamageValue() <= 0) {
            return;
        }

        int repairPerShard = Math.max(64, wand.getMaxDamage() / 4);
        int shardsNeeded = Math.min(shards.getCount(), (int) Math.ceil(wand.getDamageValue() / (double) repairPerShard));
        if (shardsNeeded <= 0) {
            return;
        }

        ItemStack result = wand.copy();
        StormWandItem.repairWand(result, repairPerShard * shardsNeeded);
        event.setOutput(result);
        event.setCost(Math.max(1, shardsNeeded));
        event.setMaterialCost(shardsNeeded);
    }

    private static int getUpgradedSpellLevel(int currentLevel, int bookLevel, int maxLevel) {
        if (currentLevel <= 0) {
            return Math.min(bookLevel, maxLevel);
        }

        if (currentLevel == bookLevel && currentLevel < maxLevel) {
            return currentLevel + 1;
        }

        if (bookLevel > currentLevel) {
            return Math.min(bookLevel, maxLevel);
        }

        return -1;
    }
}