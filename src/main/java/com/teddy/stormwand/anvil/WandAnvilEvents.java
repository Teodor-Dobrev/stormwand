package com.teddy.stormwand.anvil;

import com.teddy.stormwand.StormWandMod;
import com.teddy.stormwand.item.ModItems;
import com.teddy.stormwand.item.SpellBookItem;
import com.teddy.stormwand.item.StormWandItem;
import com.teddy.stormwand.spell.ModSpells;
import com.teddy.stormwand.spell.WandSpell;
import com.teddy.stormwand.spell.WandSpellData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

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

        if (left.getItem() instanceof StormWandItem && right.getItem() instanceof StormWandItem) {
            mergeWands(event, left, right);
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

    private static void mergeWands(AnvilUpdateEvent event, ItemStack left, ItemStack right) {
        ItemStack result = left.copy();
        ItemStack rightCopy = right.copy();
        WandSpellData.ensureDefaults(result);
        WandSpellData.ensureDefaults(rightCopy);

        int operations = 0;
        if (result.isDamageableItem() && rightCopy.isDamageableItem()) {
            int mergedDamage = mergeDamageValue(left, right);
            if (mergedDamage != result.getDamageValue()) {
                result.setDamageValue(mergedDamage);
                operations++;
            }
        }

        operations += mergeEnchantments(result, right);
        operations += mergeSpells(result, rightCopy);
        if (operations <= 0) {
            return;
        }

        result.setRepairCost(Math.max(left.getBaseRepairCost(), right.getBaseRepairCost()));
        event.setOutput(result);
        event.setCost(Math.max(1, operations + 1));
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
        result.setRepairCost(wand.getBaseRepairCost());
        event.setOutput(result);
        event.setCost(Math.max(1, shardsNeeded));
        event.setMaterialCost(shardsNeeded);
    }

    private static int mergeDamageValue(ItemStack left, ItemStack right) {
        if (!left.isDamageableItem() || !right.isDamageableItem()) {
            return left.getDamageValue();
        }

        int leftMax = left.getMaxDamage();
        int rightMax = right.getMaxDamage();
        if (leftMax <= 0 || rightMax <= 0) {
            return left.getDamageValue();
        }

        int leftRemaining = leftMax - left.getDamageValue();
        int rightRemainingScaled = (int) Math.round(((double) (rightMax - right.getDamageValue()) / (double) rightMax) * leftMax);
        int bonus = Math.max(1, (int) (leftMax * 0.12F));
        int mergedRemaining = Math.min(leftMax, leftRemaining + rightRemainingScaled + bonus);
        return Math.max(0, leftMax - mergedRemaining);
    }

    private static int mergeEnchantments(ItemStack target, ItemStack addition) {
        Map<Enchantment, Integer> merged = new HashMap<>(EnchantmentHelper.getEnchantments(target));
        Map<Enchantment, Integer> incoming = EnchantmentHelper.getEnchantments(addition);
        int changes = 0;

        for (Map.Entry<Enchantment, Integer> entry : incoming.entrySet()) {
            Enchantment enchantment = entry.getKey();
            int currentLevel = merged.getOrDefault(enchantment, 0);
            int incomingLevel = entry.getValue();
            int mergedLevel;

            if (currentLevel == incomingLevel) {
                mergedLevel = Math.min(enchantment.getMaxLevel(), currentLevel + 1);
            } else {
                mergedLevel = Math.min(enchantment.getMaxLevel(), Math.max(currentLevel, incomingLevel));
            }

            if (mergedLevel > currentLevel) {
                merged.put(enchantment, mergedLevel);
                changes++;
            }
        }

        if (changes > 0) {
            EnchantmentHelper.setEnchantments(merged, target);
        }

        return changes;
    }

    private static int mergeSpells(ItemStack target, ItemStack addition) {
        int changes = 0;

        for (WandSpell spell : ModSpells.all()) {
            ResourceLocation spellId = spell.id();
            int currentLevel = WandSpellData.getSpellLevel(target, spellId);
            int incomingLevel = WandSpellData.getSpellLevel(addition, spellId);
            int mergedLevel = getMergedSpellLevel(currentLevel, incomingLevel, spell.getMaxLevel());
            if (mergedLevel > currentLevel) {
                WandSpellData.setSpellLevel(target, spellId, mergedLevel);
                changes++;
            }
        }

        return changes;
    }

    private static int getMergedSpellLevel(int currentLevel, int incomingLevel, int maxLevel) {
        if (currentLevel <= 0 && incomingLevel <= 0) {
            return 0;
        }

        if (currentLevel <= 0) {
            return Math.min(incomingLevel, maxLevel);
        }

        if (incomingLevel <= 0) {
            return Math.min(currentLevel, maxLevel);
        }

        if (currentLevel == incomingLevel) {
            return Math.min(maxLevel, currentLevel + 1);
        }

        return Math.min(maxLevel, Math.max(currentLevel, incomingLevel));
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
