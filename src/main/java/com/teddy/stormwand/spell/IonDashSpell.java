package com.teddy.stormwand.spell;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;

public class IonDashSpell implements WandSpell {
    private static final int[] MANA_COST = {10, 12, 14, 17, 21};
    private static final int[] COOLDOWN = {36, 30, 24, 20, 16};
    private static final double[] DASH_DISTANCE = {8.0D, 10.0D, 12.0D, 14.0D, 16.0D};
    private static final int[] PHASE_BLOCKS = {1, 2, 3, 4, 5};

    private final ResourceLocation id;

    public IonDashSpell(ResourceLocation id) {
        this.id = id;
    }

    @Override
    public ResourceLocation id() {
        return this.id;
    }

    @Override
    public Component displayName() {
        return Component.translatable("spell.stormwand.ion_dash");
    }

    @Override
    public SpellCastResult cast(SpellCastContext context, int spellLevel) {
        return SpellCastResult.NO_TARGET;
    }

    @Override
    public SpellCastResult castFromUse(ServerPlayer player, net.minecraft.world.item.ItemStack wandStack, int spellLevel) {
        Vec3 start = player.position();
        Vec3 direction = player.getLookAngle().normalize();
        Vec3 destination = findDestination(player, direction, getDashDistance(spellLevel), getPhaseBlocks(spellLevel));
        if (destination == null || destination.distanceToSqr(start) < 0.09D) {
            return SpellCastResult.NO_TARGET;
        }

        player.serverLevel().sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1.0D, player.getZ(), 16, 0.28D, 0.25D, 0.28D, 0.06D);
        player.teleportTo(destination.x, destination.y, destination.z);
        player.fallDistance = 0.0F;
        player.serverLevel().sendParticles(ParticleTypes.ELECTRIC_SPARK, destination.x, destination.y + 1.0D, destination.z, 24, 0.32D, 0.3D, 0.32D, 0.08D);
        player.level().playSound(null, start.x, start.y, start.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.45F, 1.35F);
        player.level().playSound(null, destination.x, destination.y, destination.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.55F, 1.65F);
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
        return getDashDistance(spellLevel);
    }

    @Override
    public int getMaxLevel() {
        return 5;
    }

    @Override
    public List<Component> getTooltipLines(int spellLevel) {
        return List.of(
                Component.translatable("tooltip.stormwand.ion_dash.distance", format(getDashDistance(spellLevel))),
                Component.translatable("tooltip.stormwand.ion_dash.phase", getPhaseBlocks(spellLevel))
        );
    }

    private Vec3 findDestination(ServerPlayer player, Vec3 direction, double maxDistance, int maxPhaseBlocks) {
        Vec3 eye = player.getEyePosition();
        Vec3 lastSafeFeet = null;
        int solidRun = 0;

        for (double step = 0.35D; step <= maxDistance; step += 0.25D) {
            Vec3 probe = eye.add(direction.scale(step));
            BlockPos probePos = BlockPos.containing(probe);
            boolean solid = !player.level().getBlockState(probePos).getCollisionShape(player.level(), probePos).isEmpty();
            if (solid) {
                solidRun++;
                if (solidRun > maxPhaseBlocks + 1) {
                    break;
                }
            } else {
                solidRun = 0;
            }

            Vec3 feet = probe.subtract(0.0D, player.getEyeHeight(), 0.0D);
            if (isSafeFeetPosition(player, feet)) {
                lastSafeFeet = feet;
            }
        }

        return lastSafeFeet;
    }

    private boolean isSafeFeetPosition(ServerPlayer player, Vec3 feetPosition) {
        AABB movedBounds = player.getBoundingBox().move(feetPosition.subtract(player.position()));
        return player.level().noCollision(player, movedBounds);
    }

    private int getPhaseBlocks(int spellLevel) {
        return valueForLevel(PHASE_BLOCKS, spellLevel);
    }

    private double getDashDistance(int spellLevel) {
        return valueForLevel(DASH_DISTANCE, spellLevel);
    }

    private int valueForLevel(int[] values, int spellLevel) {
        return values[Math.max(0, Math.min(values.length - 1, spellLevel - 1))];
    }

    private double valueForLevel(double[] values, int spellLevel) {
        return values[Math.max(0, Math.min(values.length - 1, spellLevel - 1))];
    }

    private String format(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
