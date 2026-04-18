package com.teddy.stormwand.client;

import com.teddy.stormwand.StormWandMod;
import com.teddy.stormwand.entity.ModEntities;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = StormWandMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.EXPERIENCE_BAR.id(), "mana_bar", ManaHudOverlay.INSTANCE);
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "spell_selector", SpellSelectorOverlay.INSTANCE);
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.STORM_BOLT.get(), StormBoltRenderer::new);
        event.registerEntityRenderer(ModEntities.STORM_GRENADE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.BALL_LIGHTNING.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.ARC_MINE.get(), ThrownItemRenderer::new);
    }
}
