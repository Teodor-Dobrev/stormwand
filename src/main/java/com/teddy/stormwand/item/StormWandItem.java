package com.teddy.stormwand.item;

import com.teddy.stormwand.mana.ManaHelper;
import com.teddy.stormwand.mana.PlayerMana;
import com.teddy.stormwand.mana.PlayerManaProvider;
import com.teddy.stormwand.network.StormWandNetwork;
import com.teddy.stormwand.spell.SpellCastResult;
import com.teddy.stormwand.spell.WandSpell;
import com.teddy.stormwand.spell.WandSpellData;
import com.teddy.stormwand.util.RomanNumerals;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.DigDurabilityEnchantment;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public class StormWandItem extends Item {
    private static final Set<Enchantment> SUPPORTED_WEAPON_ENCHANTMENTS = Set.of(
            Enchantments.SHARPNESS,
            Enchantments.SMITE,
            Enchantments.BANE_OF_ARTHROPODS,
            Enchantments.FIRE_ASPECT,
            Enchantments.MOB_LOOTING
    );

    private final WandTier tier;

    public StormWandItem(WandTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public WandTier getTier() {
        return this.tier;
    }

    @Override
    public int getEnchantmentValue() {
        return 18;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return super.canApplyAtEnchantingTable(stack, enchantment)
                || SUPPORTED_WEAPON_ENCHANTMENTS.contains(enchantment)
;
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repairCandidate) {
        return repairCandidate.is(Items.AMETHYST_SHARD) || super.isValidRepairItem(toRepair, repairCandidate);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        WandSpellData.ensureDefaults(stack);

        if (isBroken(stack)) {
            player.displayClientMessage(Component.translatable("message.stormwand.wand_broken"), true);
            return InteractionResultHolder.fail(stack);
        }

        WandSpell selectedSpell = WandSpellData.getSelectedSpell(stack);
        int spellLevel = WandSpellData.getSpellLevel(stack, selectedSpell.id());
        boolean creativeMode = player.getAbilities().instabuild;

        int remainingCooldown = WandSpellData.getRemainingCooldownTicks(stack, selectedSpell.id(), level.getGameTime());
        if (!creativeMode && remainingCooldown > 0) {
            player.displayClientMessage(Component.translatable("message.stormwand.spell_on_cooldown", formatSeconds(remainingCooldown)), true);
            return InteractionResultHolder.fail(stack);
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        int manaCost = getManaCost(serverPlayer, stack, selectedSpell, spellLevel);
        if (!creativeMode) {
            PlayerMana mana = PlayerManaProvider.get(serverPlayer).orElse(null);
            if (mana == null) {
                player.displayClientMessage(Component.translatable("message.stormwand.mana_unavailable"), true);
                return InteractionResultHolder.fail(stack);
            }

            mana.setMaxMana(ManaHelper.getMaxMana(serverPlayer));
            if (selectedSpell.requiresFullMana() && mana.getCurrentMana() < mana.getMaxMana()) {
                player.displayClientMessage(Component.translatable("message.stormwand.requires_full_mana"), true);
                return InteractionResultHolder.fail(stack);
            }

            if (mana.getCurrentMana() < manaCost) {
                player.displayClientMessage(Component.translatable("message.stormwand.not_enough_mana"), true);
                return InteractionResultHolder.fail(stack);
            }

            if (manaCost > 0) {
                mana.consume(manaCost);
                StormWandNetwork.syncMana(serverPlayer, mana);
            }
        }

        SpellCastResult castResult = selectedSpell.castFromUse(serverPlayer, stack, spellLevel);
        if (castResult != SpellCastResult.SUCCESS) {
            return InteractionResultHolder.fail(stack);
        }

        if (!creativeMode) {
            int cooldownTicks = getCooldownTicks(serverPlayer, stack, selectedSpell, spellLevel);
            if (cooldownTicks > 0) {
                WandSpellData.setCooldown(stack, selectedSpell.id(), serverPlayer.level().getGameTime() + cooldownTicks);
            }

            if (!this.tier.isEternal() && !selectedSpell.handlesOwnDurabilityCost()) {
                damageWand(stack, serverPlayer, selectedSpell.getDurabilityCost(serverPlayer, stack, spellLevel));
            }
        }

        player.swing(hand, true);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        WandSpellData.ensureDefaults(stack);
        WandSpell selectedSpell = WandSpellData.getSelectedSpell(stack);
        int spellLevel = WandSpellData.getSpellLevel(stack, selectedSpell.id());

        tooltip.add(Component.translatable("tooltip.stormwand.wand_tier", Component.translatable(this.getDescriptionId())).withStyle(ChatFormatting.GRAY));
        if (isBroken(stack)) {
            tooltip.add(Component.translatable("tooltip.stormwand.broken").withStyle(ChatFormatting.RED));
        }
        tooltip.add(Component.translatable("tooltip.stormwand.selected_spell", selectedSpell.displayName(), RomanNumerals.toRoman(spellLevel)).withStyle(ChatFormatting.AQUA));
        for (Component line : selectedSpell.getTooltipLines(spellLevel)) {
            tooltip.add(line.copy().withStyle(ChatFormatting.BLUE));
        }
        if (selectedSpell.requiresFullMana()) {
            tooltip.add(Component.translatable("tooltip.stormwand.mana_cost.full").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.stormwand.mana_cost", getManaCost(null, stack, selectedSpell, spellLevel)).withStyle(ChatFormatting.GRAY));
        }
        int cooldownTicks = getCooldownTicks(null, stack, selectedSpell, spellLevel);
        if (cooldownTicks > 0) {
            tooltip.add(Component.translatable("tooltip.stormwand.cooldown", formatSeconds(cooldownTicks)).withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.stormwand.cooldown.none").withStyle(ChatFormatting.GRAY));
        }
        if (stack.isDamageableItem()) {
            int durabilityLeft = stack.getMaxDamage() - stack.getDamageValue();
            tooltip.add(Component.translatable("tooltip.stormwand.durability", durabilityLeft, stack.getMaxDamage() - 1).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.stormwand.repair_hint").withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.add(Component.translatable("tooltip.stormwand.mana_discount", (int) Math.round(this.tier.getManaDiscount() * 100.0D)).withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(Component.translatable("tooltip.stormwand.cooldown_reduction", (int) Math.round(this.tier.getCooldownReduction() * 100.0D)).withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(Component.translatable("tooltip.stormwand.merge_hint").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.stormwand.tier_upgrade_hint").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.stormwand.selector_hint").withStyle(ChatFormatting.DARK_GRAY));
        for (WandSpellData.SpellLevelEntry entry : WandSpellData.getInstalledSpells(stack)) {
            tooltip.add(Component.translatable("tooltip.stormwand.installed_spell", entry.spell().displayName(), RomanNumerals.toRoman(entry.level())).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    public int getManaCost(@Nullable ServerPlayer player, ItemStack stack, WandSpell spell, int spellLevel) {
        int baseCost = spell.getManaCost(player, stack, spellLevel);
        if (baseCost <= 0) {
            return 0;
        }
        if (spell.ignoresManaDiscount()) {
            return baseCost;
        }
        return Math.max(1, (int) Math.round(baseCost * (1.0D - this.tier.getManaDiscount())));
    }

    public int getCooldownTicks(@Nullable ServerPlayer player, ItemStack stack, WandSpell spell, int spellLevel) {
        int baseCooldown = spell.getBaseCooldownTicks(spellLevel);
        if (baseCooldown <= 0) {
            return 0;
        }
        if (spell.ignoresCooldownReduction()) {
            return baseCooldown;
        }
        return Math.max(0, (int) Math.round(baseCooldown * (1.0D - this.tier.getCooldownReduction())));
    }

    public static boolean isBroken(ItemStack stack) {
        return stack.isDamageableItem() && stack.getDamageValue() >= stack.getMaxDamage() - 1;
    }

    public static void damageWand(ItemStack stack, ServerPlayer player, int attemptedDamage) {
        if (!(stack.getItem() instanceof StormWandItem wandItem) || wandItem.tier.isEternal() || attemptedDamage <= 0 || !stack.isDamageableItem()) {
            return;
        }

        RandomSource random = player.getRandom();
        int unbreakingLevel = stack.getEnchantmentLevel(Enchantments.UNBREAKING);

        for (int point = 0; point < attemptedDamage; point++) {
            if (isBroken(stack)) {
                break;
            }

            if (unbreakingLevel > 0 && DigDurabilityEnchantment.shouldIgnoreDurabilityDrop(stack, unbreakingLevel, random)) {
                continue;
            }

            stack.setDamageValue(Math.min(stack.getMaxDamage() - 1, stack.getDamageValue() + 1));
        }

        if (isBroken(stack)) {
            player.displayClientMessage(Component.translatable("message.stormwand.wand_broken"), true);
        }
    }

    public static void repairWand(ItemStack stack, int repairAmount) {
        if (!stack.isDamageableItem() || repairAmount <= 0) {
            return;
        }

        stack.setDamageValue(Math.max(0, stack.getDamageValue() - repairAmount));
    }

    private String formatSeconds(int cooldownTicks) {
        return String.format(java.util.Locale.ROOT, "%.2fs", cooldownTicks / 20.0D);
    }
}
