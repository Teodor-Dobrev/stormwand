package com.teddy.stormwand.enchantment;

import com.teddy.stormwand.StormWandMod;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEnchantments {
    private static final DeferredRegister<Enchantment> ENCHANTMENTS = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, StormWandMod.MOD_ID);

    public static final RegistryObject<Enchantment> INVINCIBLE_TRADE = ENCHANTMENTS.register("invincible_trade", InvincibleTradeEnchantment::new);

    private ModEnchantments() {
    }

    public static void register(IEventBus eventBus) {
        ENCHANTMENTS.register(eventBus);
    }
}