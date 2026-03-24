package com.teddy.stormwand.enchantment;

import com.teddy.stormwand.StormWandMod;
import com.teddy.stormwand.compat.CuriosCompat;
import com.teddy.stormwand.item.StormWandItem;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = StormWandMod.MOD_ID)
public final class InvincibleTradeEvents {
    private InvincibleTradeEvents() {
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide) {
            return;
        }

        if (!hasInvincibleTradeWand(player)) {
            return;
        }

        if (!consumeTotem(player)) {
            return;
        }

        event.setCanceled(true);
        player.setHealth(1.0F);
        player.removeAllEffects();
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
        player.level().broadcastEntityEvent(player, (byte) 35);
        CriteriaTriggers.USED_TOTEM.trigger(player, new ItemStack(Items.TOTEM_OF_UNDYING));
    }

    private static boolean hasInvincibleTradeWand(ServerPlayer player) {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.getItem() instanceof StormWandItem && EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.INVINCIBLE_TRADE.get(), stack) > 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean consumeTotem(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(Items.TOTEM_OF_UNDYING)) {
                stack.shrink(1);
                return true;
            }
        }

        return CuriosCompat.tryConsumeTotem(player);
    }
}