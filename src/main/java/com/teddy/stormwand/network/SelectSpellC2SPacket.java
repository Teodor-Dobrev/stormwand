package com.teddy.stormwand.network;

import com.teddy.stormwand.item.StormWandItem;
import com.teddy.stormwand.spell.WandSpellData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SelectSpellC2SPacket {
    private final InteractionHand hand;
    private final ResourceLocation spellId;

    public SelectSpellC2SPacket(InteractionHand hand, ResourceLocation spellId) {
        this.hand = hand;
        this.spellId = spellId;
    }

    public static void encode(SelectSpellC2SPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.hand);
        buffer.writeResourceLocation(packet.spellId);
    }

    public static SelectSpellC2SPacket decode(FriendlyByteBuf buffer) {
        return new SelectSpellC2SPacket(buffer.readEnum(InteractionHand.class), buffer.readResourceLocation());
    }

    public static void handle(SelectSpellC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            ItemStack stack = player.getItemInHand(packet.hand);
            if (!(stack.getItem() instanceof StormWandItem)) {
                return;
            }

            WandSpellData.ensureDefaults(stack);
            WandSpellData.setSelectedSpell(stack, packet.spellId);
        });
        context.setPacketHandled(true);
    }
}