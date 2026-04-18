package com.teddy.stormwand.spell;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;

public class StupifySpell implements WandSpell {
    private static final int[] MANA_COST = {18, 24, 30};
    private static final int[] COOLDOWN = {80, 110, 140};
    private static final double[] RANGE = {56.0D, 84.0D, 120.0D};
    private static final int[] DURATION = {140, 220, 320};

    private final ResourceLocation id;

    public StupifySpell(ResourceLocation id) {
        this.id = id;
    }

    @Override
    public ResourceLocation id() {
        return this.id;
    }

    @Override
    public Component displayName() {
        return Component.translatable("spell.stormwand.stupify");
    }

    @Override
    public SpellCastResult cast(SpellCastContext context, int spellLevel) {
        LivingEntity target = context.directHitTarget();
        if (target == null || !target.isAlive()) {
            return SpellCastResult.NO_TARGET;
        }

        ServerPlayer player = context.player();
        ServerLevel level = player.serverLevel();
        int durationTicks = getDurationTicks(spellLevel);
        StupifyControlEvents.applyStupify(target, level.getGameTime() + durationTicks);

        level.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY() + target.getBbHeight() * 0.6D, target.getZ(), 28, 0.28D, 0.4D, 0.28D, 0.02D);
        level.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + target.getBbHeight() * 0.6D, target.getZ(), 10, 0.2D, 0.25D, 0.2D, 0.01D);
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.4F, 1.35F);
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.35F, 1.6F);
        return SpellCastResult.SUCCESS;
    }

    @Override
    public int getBaseManaCost(int spellLevel) {
        return valueForLevel(MANA_COST, spellLevel);
    }

    @Override
    public int getBaseCooldownTicks(int spellLevel) {
        return valueForLevel(COOLDOWN, spellLevel);
    }

    @Override
    public double getCastRange(int spellLevel) {
        return valueForLevel(RANGE, spellLevel);
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }

    @Override
    public List<Component> getTooltipLines(int spellLevel) {
        return List.of(
                Component.translatable("tooltip.stormwand.stupify.duration", formatSeconds(getDurationTicks(spellLevel) / 20.0D)),
                Component.translatable("tooltip.stormwand.stupify.range", formatNumber(getCastRange(spellLevel)))
        );
    }

    private int getDurationTicks(int spellLevel) {
        return valueForLevel(DURATION, spellLevel);
    }

    private int valueForLevel(int[] values, int spellLevel) {
        return values[Math.max(0, Math.min(values.length - 1, spellLevel - 1))];
    }

    private double valueForLevel(double[] values, int spellLevel) {
        return values[Math.max(0, Math.min(values.length - 1, spellLevel - 1))];
    }

    private String formatSeconds(double seconds) {
        return String.format(Locale.ROOT, "%.1f", seconds);
    }

    private String formatNumber(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
