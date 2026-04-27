package com.teddy.stormwand.spell;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public final class WandSpellData {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String SPELLS_TAG = "StormSpells";
    private static final String SELECTED_SPELL_TAG = "SelectedSpell";
    private static final String COOLDOWNS_TAG = "SpellCooldowns";

    private WandSpellData() {
    }

    public static void ensureDefaults(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(SPELLS_TAG, CompoundTag.TAG_COMPOUND)) {
            initializeStormLanceOnly(stack, 1);
            return;
        }
        sanitizeSpellData(stack, tag);
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
        if (ModSpells.byId(spellId).isEmpty()) {
            return 0;
        }

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

    private static void sanitizeSpellData(ItemStack stack, CompoundTag tag) {
        CompoundTag spellsTag = tag.getCompound(SPELLS_TAG);
        boolean changed = false;

        List<String> keys = new ArrayList<>(spellsTag.getAllKeys());
        for (String key : keys) {
            ResourceLocation spellId = ResourceLocation.tryParse(key);
            if (spellId == null || ModSpells.byId(spellId).isEmpty()) {
                spellsTag.remove(key);
                changed = true;
                LOGGER.warn("Removed unknown spell '{}' from wand '{}'.", key, stack.getItem());
                continue;
            }

            int rawLevel = spellsTag.getInt(key);
            int clampedLevel = clampLevel(spellId, rawLevel);
            if (rawLevel != clampedLevel) {
                spellsTag.putInt(key, clampedLevel);
                changed = true;
                LOGGER.warn("Clamped spell '{}' level from {} to {} on wand '{}'.", key, rawLevel, clampedLevel, stack.getItem());
            }
        }

        if (spellsTag.isEmpty()) {
            LOGGER.warn("Wand '{}' had no valid spells; restoring default spell.", stack.getItem());
            initializeStormLanceOnly(stack, 1);
            return;
        }

        if (!tag.contains(SELECTED_SPELL_TAG)) {
            ResourceLocation fallback = getFirstInstalledSpellId(spellsTag);
            tag.putString(SELECTED_SPELL_TAG, fallback.toString());
            changed = true;
            LOGGER.warn("Missing selected spell on wand '{}'; set to '{}'.", stack.getItem(), fallback);
        } else {
            ResourceLocation selected = ResourceLocation.tryParse(tag.getString(SELECTED_SPELL_TAG));
            if (selected == null || !spellsTag.contains(selected.toString())) {
                ResourceLocation fallback = getFirstInstalledSpellId(spellsTag);
                tag.putString(SELECTED_SPELL_TAG, fallback.toString());
                changed = true;
                LOGGER.warn("Reset invalid selected spell '{}' to '{}' on wand '{}'.", selected, fallback, stack.getItem());
            }
        }

        if (!tag.contains(COOLDOWNS_TAG, CompoundTag.TAG_COMPOUND)) {
            tag.put(COOLDOWNS_TAG, new CompoundTag());
            changed = true;
        } else {
            CompoundTag cooldowns = tag.getCompound(COOLDOWNS_TAG);
            boolean cooldownChanged = false;
            for (String key : new ArrayList<>(cooldowns.getAllKeys())) {
                if (!spellsTag.contains(key)) {
                    cooldowns.remove(key);
                    cooldownChanged = true;
                }
            }
            if (cooldownChanged) {
                tag.put(COOLDOWNS_TAG, cooldowns);
                changed = true;
            }
        }

        if (changed) {
            tag.put(SPELLS_TAG, spellsTag);
        }
    }

    private static ResourceLocation getFirstInstalledSpellId(CompoundTag spellsTag) {
        for (WandSpell spell : ModSpells.all()) {
            String key = spell.id().toString();
            if (spellsTag.contains(key) && spellsTag.getInt(key) > 0) {
                return spell.id();
            }
        }
        return ModSpells.STORM_LANCE.id();
    }

    private static int clampLevel(ResourceLocation spellId, int level) {
        int max = ModSpells.byId(spellId).map(WandSpell::getMaxLevel).orElse(0);
        if (max <= 0) {
            return 0;
        }
        return Math.max(1, Math.min(max, level));
    }

    public record SpellLevelEntry(WandSpell spell, int level) {
    }
}
