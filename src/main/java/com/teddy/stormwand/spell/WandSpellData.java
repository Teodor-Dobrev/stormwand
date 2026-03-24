package com.teddy.stormwand.spell;

import com.teddy.stormwand.item.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class WandSpellData {
    private static final String SPELLS_TAG = "StormSpells";
    private static final String SELECTED_SPELL_TAG = "SelectedSpell";
    private static final String COOLDOWNS_TAG = "SpellCooldowns";

    private WandSpellData() {
    }

    public static void ensureDefaults(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(SPELLS_TAG, CompoundTag.TAG_COMPOUND)) {
            initializeStormLanceOnly(stack, 1);
        }
    }

    public static ItemStack createStormLanceWand(Item item, int level) {
        ItemStack stack = new ItemStack(item);
        initializeStormLanceOnly(stack, level);
        return stack;
    }

    public static void initializeStormLanceOnly(ItemStack stack, int level) {
        CompoundTag tag = stack.getOrCreateTag();
        CompoundTag spellsTag = new CompoundTag();
        spellsTag.putInt(ModSpells.STORM_LANCE.id().toString(), clampLevel(ModSpells.STORM_LANCE.id(), level));
        tag.put(SPELLS_TAG, spellsTag);
        tag.putString(SELECTED_SPELL_TAG, ModSpells.STORM_LANCE.id().toString());
        tag.put(COOLDOWNS_TAG, new CompoundTag());
    }

    public static List<SpellLevelEntry> getInstalledSpells(ItemStack stack) {
        List<SpellLevelEntry> spells = new ArrayList<>();
        for (WandSpell spell : ModSpells.all()) {
            int level = getSpellLevel(stack, spell.id());
            if (level > 0) {
                spells.add(new SpellLevelEntry(spell, level));
            }
        }
        return spells;
    }

    public static int getSpellLevel(ItemStack stack, ResourceLocation spellId) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(SPELLS_TAG, CompoundTag.TAG_COMPOUND)) {
            return 0;
        }

        CompoundTag spellsTag = tag.getCompound(SPELLS_TAG);
        if (!spellsTag.contains(spellId.toString())) {
            return 0;
        }

        return clampLevel(spellId, spellsTag.getInt(spellId.toString()));
    }

    public static boolean hasSpell(ItemStack stack, ResourceLocation spellId) {
        return getSpellLevel(stack, spellId) > 0;
    }

    public static WandSpell getSelectedSpell(ItemStack stack) {
        return ModSpells.getOrDefault(getSelectedSpellId(stack));
    }

    public static ResourceLocation getSelectedSpellId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(SELECTED_SPELL_TAG)) {
            ResourceLocation parsed = ResourceLocation.tryParse(tag.getString(SELECTED_SPELL_TAG));
            if (parsed != null && hasSpell(stack, parsed)) {
                return parsed;
            }
        }

        return hasSpell(stack, ModSpells.STORM_LANCE.id())
                ? ModSpells.STORM_LANCE.id()
                : ModSpells.getDefaultSelectedSpell().id();
    }

    public static void setSelectedSpell(ItemStack stack, ResourceLocation spellId) {
        ensureDefaults(stack);
        if (!hasSpell(stack, spellId)) {
            return;
        }
        stack.getOrCreateTag().putString(SELECTED_SPELL_TAG, spellId.toString());
    }

    public static void setSpellLevel(ItemStack stack, ResourceLocation spellId, int level) {
        CompoundTag tag = stack.getOrCreateTag();
        CompoundTag spellsTag = tag.contains(SPELLS_TAG, CompoundTag.TAG_COMPOUND)
                ? tag.getCompound(SPELLS_TAG)
                : new CompoundTag();
        spellsTag.putInt(spellId.toString(), clampLevel(spellId, level));
        tag.put(SPELLS_TAG, spellsTag);

        if (!tag.contains(SELECTED_SPELL_TAG) || !hasSpell(stack, getSelectedSpellId(stack))) {
            tag.putString(SELECTED_SPELL_TAG, spellId.toString());
        }
    }

    public static int getRemainingCooldownTicks(ItemStack stack, ResourceLocation spellId, long gameTime) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(COOLDOWNS_TAG, CompoundTag.TAG_COMPOUND)) {
            return 0;
        }

        CompoundTag cooldowns = tag.getCompound(COOLDOWNS_TAG);
        long remaining = cooldowns.getLong(spellId.toString()) - gameTime;
        return (int) Math.max(0L, remaining);
    }

    public static void setCooldown(ItemStack stack, ResourceLocation spellId, long nextCastTick) {
        CompoundTag tag = stack.getOrCreateTag();
        CompoundTag cooldowns = tag.contains(COOLDOWNS_TAG, CompoundTag.TAG_COMPOUND)
                ? tag.getCompound(COOLDOWNS_TAG)
                : new CompoundTag();
        cooldowns.putLong(spellId.toString(), nextCastTick);
        tag.put(COOLDOWNS_TAG, cooldowns);
    }

    private static int clampLevel(ResourceLocation spellId, int level) {
        int max = ModSpells.byId(spellId).map(WandSpell::getMaxLevel).orElse(1);
        return Math.max(1, Math.min(max, level));
    }

    public record SpellLevelEntry(WandSpell spell, int level) {
    }
}