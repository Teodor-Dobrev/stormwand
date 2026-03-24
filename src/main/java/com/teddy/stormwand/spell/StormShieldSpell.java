package com.teddy.stormwand.spell;

import com.teddy.stormwand.mana.ManaHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class StormShieldSpell implements WandSpell {
    private static final int DURATION_TICKS = 20 * 60 * 10;

    private final ResourceLocation id;

    public StormShieldSpell(ResourceLocation id) {
        this.id = id;
    }

    @Override
    public ResourceLocation id() {
        return this.id;
    }

    @Override
    public Component displayName() {
        return Component.translatable("spell.stormwand.storm_shield");
    }

    @Override
    public SpellCastResult cast(SpellCastContext context, int spellLevel) {
        return SpellCastResult.NO_TARGET;
    }

    @Override
    public SpellCastResult castFromUse(ServerPlayer player, ItemStack wandStack, int spellLevel) {
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, DURATION_TICKS, spellLevel >= 4 ? 1 : 0));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, DURATION_TICKS, spellLevel - 1));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, DURATION_TICKS, Math.min(2, (spellLevel - 1) / 2)));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, DURATION_TICKS, 0));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, DURATION_TICKS, Math.max(0, spellLevel - 3)));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.9F, 0.8F);
        return SpellCastResult.SUCCESS;
    }

    @Override
    public int getBaseManaCost(int spellLevel) {
        return 0;
    }

    @Override
    public int getManaCost(ServerPlayer player, ItemStack wandStack, int spellLevel) {
        return player == null ? 0 : ManaHelper.getMaxMana(player);
    }

    @Override
    public int getBaseCooldownTicks(int spellLevel) {
        return 20 * 60 * 5;
    }

    @Override
    public double getCastRange(int spellLevel) {
        return 0.0D;
    }

    @Override
    public int getMaxLevel() {
        return 5;
    }

    @Override
    public boolean requiresFullMana() {
        return true;
    }

    @Override
    public boolean ignoresManaDiscount() {
        return true;
    }

    @Override
    public boolean ignoresCooldownReduction() {
        return true;
    }

    @Override
    public List<Component> getTooltipLines(int spellLevel) {
        return List.of(
                Component.translatable("tooltip.stormwand.storm_shield.duration", 10),
                Component.translatable("tooltip.stormwand.storm_shield.effects", spellLevel)
        );
    }
}