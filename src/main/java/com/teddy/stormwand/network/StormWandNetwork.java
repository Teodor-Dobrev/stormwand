package com.teddy.stormwand.network;

import com.teddy.stormwand.StormWandMod;
import com.teddy.stormwand.mana.PlayerMana;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public final class StormWandNetwork {
    private static final String PROTOCOL_VERSION = "2";
    private static int nextPacketId;
    private static boolean registered;

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(StormWandMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private StormWandNetwork() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        CHANNEL.registerMessage(
                nextPacketId++,
                SyncManaS2CPacket.class,
                SyncManaS2CPacket::encode,
                SyncManaS2CPacket::decode,
                SyncManaS2CPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        CHANNEL.registerMessage(
                nextPacketId++,
                SelectSpellC2SPacket.class,
                SelectSpellC2SPacket::encode,
                SelectSpellC2SPacket::decode,
                SelectSpellC2SPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );

        registered = true;
    }

    public static void syncMana(ServerPlayer player, PlayerMana mana) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncManaS2CPacket(mana.getCurrentMana(), mana.getMaxMana()));
    }
}