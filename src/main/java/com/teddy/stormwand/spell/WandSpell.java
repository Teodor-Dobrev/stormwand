package com.teddy.stormwand.spell;

import com.teddy.stormwand.entity.StormBoltProjectile;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface WandSpell {
    int MAX_LEVEL = 10;
    int DEFAULT_LEVEL = 5;

    ResourceLocation id();

    Component displayName();

    SpellCastResult cast(SpellCastContext context, int spellLevel);

    default SpellCastResult castFromUse(ServerPlayer player, ItemStack wandStack, int spellLevel) {
        StormBoltProjectile projectile = new StormBoltProjectile(player.level(), player, wandStack, id(), spellLevel);
        projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 2.45F, 0.0F);
        player.level().addFreshEntity(projectile);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 0.35F, 1.55F);
        return SpellCastResult.SUCCESS;
    }

    int getBaseManaCost(int spellLevel);

    int getBaseCooldownTicks(int spellLevel);

    double getCastRange(int spellLevel);

    List<Component> getTooltipLines(int spellLevel);

    default int getMaxLevel() {
        return MAX_LEVEL;
    }

    default int getManaCost(ServerPlayer player, ItemStack wandStack, int spellLevel) {
        return getBaseManaCost(spellLevel);
    }

    default int getDurabilityCost(ServerPlayer player, ItemStack wandStack, int spellLevel) {
        return 1;
    }

    default boolean handlesOwnDurabilityCost() {
        return false;
    }

    default boolean requiresFullMana() {
        return false;
    }

    default boolean ignoresManaDiscount() {
        return false;
    }

    default boolean ignoresCooldownReduction() {
        return false;
    }

    default boolean resolvesAtMaxRange() {
        return true;
    }
}