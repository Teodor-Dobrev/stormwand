package com.teddy.stormwand;

import com.teddy.stormwand.config.StormWandConfig;
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
        ModEntities.register(modEventBus);
        modEventBus.addListener(this::addCreativeTabItems);

        context.registerConfig(ModConfig.Type.COMMON, StormWandConfig.SPEC);
        StormWandNetwork.register();
    }

    private void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            ModItems.getCreativeWands().forEach(wand -> event.accept(wand.get()));
        }
    }
}