package com.teddy.stormwand.spell;

import com.teddy.stormwand.item.StormWandItem;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.List;

public class FisheyFishingSpell implements WandSpell {
    private final ResourceLocation id;

    public FisheyFishingSpell(ResourceLocation id) {
        this.id = id;
    }

    @Override
    public ResourceLocation id() {
        return this.id;
    }

    @Override
    public Component displayName() {
        return Component.translatable("spell.stormwand.fishey_fishing");
    }

    @Override
    public SpellCastResult cast(SpellCastContext context, int spellLevel) {
        return SpellCastResult.NO_TARGET;
    }

    @Override
    public SpellCastResult castFromUse(ServerPlayer player, ItemStack wandStack, int spellLevel) {
        LootParams params = new LootParams.Builder(player.serverLevel())
                .withParameter(LootContextParams.ORIGIN, player.position())
                .withParameter(LootContextParams.TOOL, new ItemStack(Items.FISHING_ROD))
                .withOptionalParameter(LootContextParams.THIS_ENTITY, player)
                .create(LootContextParamSets.FISHING);

        LootTable lootTable = player.serverLevel().getServer().getLootData().getLootTable(BuiltInLootTables.FISHING);
        List<ItemStack> loot = lootTable.getRandomItems(params);
        for (ItemStack lootStack : loot) {
            if (!player.addItem(lootStack.copy())) {
                player.drop(lootStack.copy(), false);
            }
        }

        int durabilityCost = 10 + player.getRandom().nextInt(41);
        StormWandItem.damageWand(wandStack, player, durabilityCost);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.PLAYERS, 0.8F, 0.9F);
        return SpellCastResult.SUCCESS;
    }

    @Override
    public int getBaseManaCost(int spellLevel) {
        return 0;
    }

    @Override
    public int getBaseCooldownTicks(int spellLevel) {
        return 20;
    }

    @Override
    public double getCastRange(int spellLevel) {
        return 0.0D;
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }

    @Override
    public boolean handlesOwnDurabilityCost() {
        return true;
    }

    @Override
    public List<Component> getTooltipLines(int spellLevel) {
        return List.of(
                Component.translatable("tooltip.stormwand.fishey_fishing.loot"),
                Component.translatable("tooltip.stormwand.fishey_fishing.durability")
        );
    }
}