package com.teddy.stormwand.network;

import com.teddy.stormwand.mana.ClientManaState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncManaS2CPacket {
    private final int currentMana;
    private final int maxMana;

    public SyncManaS2CPacket(int currentMana, int maxMana) {
        this.currentMana = currentMana;
        this.maxMana = maxMana;
    }

    public static void encode(SyncManaS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.currentMana);
        buffer.writeInt(packet.maxMana);
    }

    public static SyncManaS2CPacket decode(FriendlyByteBuf buffer) {
        return new SyncManaS2CPacket(buffer.readInt(), buffer.readInt());
    }

    public static void handle(SyncManaS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ClientManaState.setMana(packet.currentMana, packet.maxMana));
        context.setPacketHandled(true);
    }
}
