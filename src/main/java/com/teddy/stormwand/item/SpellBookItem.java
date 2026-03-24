package com.teddy.stormwand.item;

import com.teddy.stormwand.spell.ModSpells;
import com.teddy.stormwand.spell.WandSpell;
import com.teddy.stormwand.util.RomanNumerals;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SpellBookItem extends Item {
    private static final String SPELL_ID_TAG = "SpellId";
    private static final String SPELL_LEVEL_TAG = "SpellLevel";

    public SpellBookItem(Properties properties) {
        super(properties);
    }

    public static ItemStack createFor(WandSpell spell, int level, Item item) {
        ItemStack stack = new ItemStack(item);
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(SPELL_ID_TAG, spell.id().toString());
        tag.putInt(SPELL_LEVEL_TAG, Math.max(1, Math.min(spell.getMaxLevel(), level)));
        return stack;
    }

    public static ResourceLocation getSpellId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(SPELL_ID_TAG)) {
            ResourceLocation id = ResourceLocation.tryParse(tag.getString(SPELL_ID_TAG));
            if (id != null) {
                return id;
            }
        }
        return ModSpells.getDefaultSelectedSpell().id();
    }

    public static int getSpellLevel(ItemStack stack) {
        WandSpell spell = getSpell(stack);
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(SPELL_LEVEL_TAG)) {
            return Math.max(1, Math.min(spell.getMaxLevel(), tag.getInt(SPELL_LEVEL_TAG)));
        }
        return 1;
    }

    public static WandSpell getSpell(ItemStack stack) {
        return ModSpells.getOrDefault(getSpellId(stack));
    }

    @Override
    public Component getName(ItemStack stack) {
        WandSpell spell = getSpell(stack);
        int level = getSpellLevel(stack);
        return Component.translatable("item.stormwand.spell_book.named", spell.displayName(), RomanNumerals.toRoman(level));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        WandSpell spell = getSpell(stack);
        int spellLevel = getSpellLevel(stack);

        tooltip.add(Component.translatable("tooltip.stormwand.spell_book.teaches", spell.displayName(), RomanNumerals.toRoman(spellLevel)).withStyle(ChatFormatting.GRAY));
        for (Component line : spell.getTooltipLines(spellLevel)) {
            tooltip.add(line.copy().withStyle(ChatFormatting.AQUA));
        }
        tooltip.add(Component.translatable("tooltip.stormwand.spell_book.combine").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.stormwand.spell_book.apply").withStyle(ChatFormatting.DARK_GRAY));
    }
}