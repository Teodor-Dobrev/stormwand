package com.teddy.stormwand.item;

import com.teddy.stormwand.entity.StormBoltProjectile;
import com.teddy.stormwand.mana.ManaHelper;
import com.teddy.stormwand.mana.PlayerManaProvider;
import com.teddy.stormwand.network.StormWandNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

import java.util.Set;

public class StormWandItem extends Item {
    private static final float PROJECTILE_SPEED = 2.45F;
    private static final Set<Enchantment> SUPPORTED_WEAPON_ENCHANTMENTS = Set.of(
            Enchantments.SHARPNESS,
            Enchantments.SMITE,
            Enchantments.BANE_OF_ARTHROPODS,
            Enchantments.FIRE_ASPECT,
            Enchantments.MOB_LOOTING
    );

    private final WandTier tier;

    public StormWandItem(WandTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public WandTier getTier() {
        return this.tier;
    }

    @Override
    public int getEnchantmentValue() {
        return 18;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return super.canApplyAtEnchantingTable(stack, enchantment) || SUPPORTED_WEAPON_ENCHANTMENTS.contains(enchantment);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean creativeMode = player.getAbilities().instabuild;
        WandFireMode fireMode = WandFireMode.fromStack(stack);

        if (player.isShiftKeyDown()) {
            if (!(player instanceof ServerPlayer)) {
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }

            WandFireMode nextMode = fireMode.next();
            WandFireMode.set(stack, nextMode);
            player.displayClientMessage(Component.translatable(nextMode == WandFireMode.CHAIN ? "message.stormwand.mode_chain" : "message.stormwand.mode_focused"), true);
            return InteractionResultHolder.consume(stack);
        }

        if (!creativeMode && fireMode == WandFireMode.CHAIN && player.getCooldowns().isOnCooldown(stack.getItem())) {
            return InteractionResultHolder.fail(stack);
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        if (!creativeMode) {
            boolean enoughMana = PlayerManaProvider.get(serverPlayer)
                    .map(mana -> {
                        mana.setMaxMana(ManaHelper.getMaxMana(serverPlayer));
                        if (mana.getCurrentMana() < fireMode.getManaCost()) {
                            return false;
                        }
                        mana.consume(fireMode.getManaCost());
                        StormWandNetwork.syncMana(serverPlayer, mana);
                        return true;
                    })
                    .orElse(false);

            if (!enoughMana) {
                player.displayClientMessage(Component.translatable("message.stormwand.not_enough_mana"), true);
                return InteractionResultHolder.fail(stack);
            }
        }

        StormBoltProjectile projectile = new StormBoltProjectile(level, serverPlayer, stack);
        projectile.shootFromRotation(serverPlayer, serverPlayer.getXRot(), serverPlayer.getYRot(), 0.0F, PROJECTILE_SPEED, 0.0F);
        level.addFreshEntity(projectile);

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 0.35F, 1.55F);
        player.swing(hand, true);

        if (!creativeMode && !this.tier.isEternal()) {
            stack.hurtAndBreak(1, serverPlayer, brokenPlayer -> brokenPlayer.broadcastBreakEvent(hand));
        }

        return InteractionResultHolder.consume(stack);
    }
}