package com.teddy.stormwand;

import com.teddy.stormwand.config.StormWandConfig;
import com.teddy.stormwand.enchantment.ModEnchantments;
import com.teddy.stormwand.entity.ModEntities;
import com.teddy.stormwand.item.ModItems;
import com.teddy.stormwand.network.StormWandNetwork;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(StormWandMod.MOD_ID)
public class StormWandMod {
    public static final String MOD_ID = "stormwand";

    public StormWandMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        ModItems.register(modEventBus);
        ModEnchantments.register(modEventBus);
        ModEntities.register(modEventBus);
        modEventBus.addListener(this::addCreativeTabItems);

        context.registerConfig(ModConfig.Type.COMMON, StormWandConfig.SPEC);
        StormWandNetwork.register();
    }

    private void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        if (CreativeModeTabs.COMBAT.equals(event.getTabKey())) {
            ModItems.getCreativeWands().forEach(wand -> event.accept(wand.get()));
            return;
        }

        if (CreativeModeTabs.TOOLS_AND_UTILITIES.equals(event.getTabKey())) {
            ModItems.getCreativeSpellBooks().forEach(event::accept);
            return;
        }

        if (CreativeModeTabs.SEARCH.equals(event.getTabKey())) {
            ModItems.getCreativeWands().forEach(wand -> event.accept(wand.get()));
            ModItems.getCreativeSpellBooks().forEach(event::accept);
        }
    }
}
