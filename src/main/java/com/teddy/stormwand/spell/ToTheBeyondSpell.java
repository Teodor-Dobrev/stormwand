package com.teddy.stormwand.spell;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;

public class ToTheBeyondSpell implements WandSpell {
    private final ResourceLocation id;

    public ToTheBeyondSpell(ResourceLocation id) {
        this.id = id;
    }

    @Override
    public ResourceLocation id() {
        return this.id;
    }

    @Override
    public Component displayName() {
        return Component.translatable("spell.stormwand.to_the_beyond");
    }

    @Override
    public SpellCastResult cast(SpellCastContext context, int spellLevel) {
        return SpellCastResult.NO_TARGET;
    }

    @Override
    public SpellCastResult castFromUse(ServerPlayer player, ItemStack wandStack, int spellLevel) {
        if (player.isFallFlying()) {
            ItemStack rocketStack = new ItemStack(Items.FIREWORK_ROCKET);
            CompoundTag fireworks = rocketStack.getOrCreateTagElement("Fireworks");
            fireworks.putByte("Flight", (byte) 3);
            FireworkRocketEntity rocket = new FireworkRocketEntity(player.level(), rocketStack, player);
            player.level().addFreshEntity(rocket);
        } else {
            Vec3 look = player.getLookAngle().normalize();
            double strength = 1.25D + (spellLevel - 1) * 0.35D;
            if (player.isInWater()) {
                strength += 0.25D;
            }
            player.setDeltaMovement(player.getDeltaMovement().add(look.scale(strength)));
            player.hasImpulse = true;
            player.hurtMarked = true;
        }

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 0.7F, 1.0F + spellLevel * 0.04F);
        return SpellCastResult.SUCCESS;
    }

    @Override
    public int getBaseManaCost(int spellLevel) {
        return 18;
    }

    @Override
    public int getBaseCooldownTicks(int spellLevel) {
        return 16;
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
    public List<Component> getTooltipLines(int spellLevel) {
        return List.of(
                Component.translatable("tooltip.stormwand.to_the_beyond.power", format(1.25D + (spellLevel - 1) * 0.35D)),
                Component.translatable("tooltip.stormwand.to_the_beyond.elytra")
        );
    }

    private String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}