package com.teddy.stormwand.spell;

import com.teddy.stormwand.item.StormWandItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class RussianRouletteSpell implements WandSpell {
    private static final List<EntityType<? extends Mob>> GOOD_MOBS = List.of(
            EntityType.COW, EntityType.SHEEP, EntityType.PIG, EntityType.CHICKEN, EntityType.RABBIT,
            EntityType.HORSE, EntityType.DONKEY, EntityType.MULE, EntityType.LLAMA, EntityType.TRADER_LLAMA,
            EntityType.FOX, EntityType.WOLF, EntityType.CAT, EntityType.OCELOT, EntityType.PANDA,
            EntityType.POLAR_BEAR, EntityType.TURTLE, EntityType.DOLPHIN, EntityType.BEE, EntityType.GOAT,
            EntityType.CAMEL, EntityType.SNIFFER, EntityType.FROG, EntityType.AXOLOTL, EntityType.MOOSHROOM,
            EntityType.STRIDER, EntityType.PARROT
    );
    private static final List<EntityType<? extends Mob>> BAD_MOBS = List.of(
            EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER, EntityType.SPIDER, EntityType.CAVE_SPIDER,
            EntityType.ENDERMAN, EntityType.BLAZE, EntityType.WITCH, EntityType.HUSK, EntityType.DROWNED,
            EntityType.PILLAGER, EntityType.VINDICATOR, EntityType.EVOKER, EntityType.RAVAGER, EntityType.PHANTOM,
            EntityType.SLIME, EntityType.MAGMA_CUBE, EntityType.PIGLIN, EntityType.ZOMBIFIED_PIGLIN,
            EntityType.HOGLIN, EntityType.ZOGLIN, EntityType.GUARDIAN, EntityType.ELDER_GUARDIAN,
            EntityType.SHULKER, EntityType.SILVERFISH, EntityType.ENDERMITE, EntityType.WITHER_SKELETON,
            EntityType.VEX, EntityType.GHAST
    );

    private final ResourceLocation id;

    public RussianRouletteSpell(ResourceLocation id) {
        this.id = id;
    }

    @Override
    public ResourceLocation id() {
        return this.id;
    }

    @Override
    public Component displayName() {
        return Component.translatable("spell.stormwand.russian_roulette");
    }

    @Override
    public SpellCastResult cast(SpellCastContext context, int spellLevel) {
        return SpellCastResult.NO_TARGET;
    }

    @Override
    public SpellCastResult castFromUse(ServerPlayer player, ItemStack wandStack, int spellLevel) {
        int count = 1 + player.getRandom().nextInt(5);
        ServerLevel level = player.serverLevel();
        Vec3 center = player.position().add(player.getLookAngle().scale(3.0D));

        for (int index = 0; index < count; index++) {
            EntityType<? extends Mob> type = chooseMob(player, spellLevel);
            Mob mob = type.create(level);
            if (mob == null) {
                continue;
            }

            BlockPos spawnPos = BlockPos.containing(center.add(player.getRandom().nextDouble() * 4.0D - 2.0D, 0.0D, player.getRandom().nextDouble() * 4.0D - 2.0D));
            BlockPos topPos = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawnPos);
            mob.moveTo(topPos.getX() + 0.5D, topPos.getY(), topPos.getZ() + 0.5D, player.getRandom().nextFloat() * 360.0F, 0.0F);
            SpawnPlacements.checkSpawnRules(type, level, MobSpawnType.MOB_SUMMONED, topPos, level.random);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(topPos), MobSpawnType.MOB_SUMMONED, null, null);
            level.addFreshEntity(mob);
        }

        StormWandItem.damageWand(wandStack, player, count * 10);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.9F, 0.8F + spellLevel * 0.05F);
        return SpellCastResult.SUCCESS;
    }

    @Override
    public int getBaseManaCost(int spellLevel) {
        return 15;
    }

    @Override
    public int getBaseCooldownTicks(int spellLevel) {
        return 60;
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
    public boolean handlesOwnDurabilityCost() {
        return true;
    }

    @Override
    public List<Component> getTooltipLines(int spellLevel) {
        return List.of(
                Component.translatable("tooltip.stormwand.russian_roulette.count"),
                Component.translatable("tooltip.stormwand.russian_roulette.good_chance", getGoodChancePercent(spellLevel))
        );
    }

    private EntityType<? extends Mob> chooseMob(ServerPlayer player, int spellLevel) {
        if (player.getRandom().nextDouble() < getGoodChance(spellLevel)) {
            return GOOD_MOBS.get(player.getRandom().nextInt(GOOD_MOBS.size()));
        }

        if (player.getRandom().nextDouble() < getWardenChance(spellLevel)) {
            return EntityType.WARDEN;
        }

        return BAD_MOBS.get(player.getRandom().nextInt(BAD_MOBS.size()));
    }

    private double getGoodChance(int spellLevel) {
        return switch (spellLevel) {
            case 1 -> 0.40D;
            case 2 -> 0.60D;
            case 3 -> 0.80D;
            case 4 -> 0.92D;
            default -> 0.99D;
        };
    }

    private double getWardenChance(int spellLevel) {
        return switch (spellLevel) {
            case 1 -> 0.030D;
            case 2 -> 0.020D;
            case 3 -> 0.015D;
            case 4 -> 0.010D;
            default -> 0.005D;
        };
    }

    private int getGoodChancePercent(int spellLevel) {
        return (int) Math.round(getGoodChance(spellLevel) * 100.0D);
    }
}