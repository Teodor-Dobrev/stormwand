package com.teddy.stormwand.compat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Predicate;

public final class CuriosCompat {
    private CuriosCompat() {
    }

    public static boolean tryConsumeTotem(ServerPlayer player) {
        if (!ModList.get().isLoaded("curios")) {
            return false;
        }

        try {
            Class<?> curiosApiClass = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            Object helper = curiosApiClass.getMethod("getCuriosHelper").invoke(null);
            Method findFirstCurio = helper.getClass().getMethod("findFirstCurio", net.minecraft.world.entity.LivingEntity.class, Predicate.class);
            Predicate<ItemStack> predicate = stack -> stack.is(Items.TOTEM_OF_UNDYING);
            Object result = findFirstCurio.invoke(helper, player, predicate);
            if (result instanceof Optional<?> optional && optional.isPresent()) {
                Object slotResult = optional.get();
                Method stackMethod = slotResult.getClass().getMethod("stack");
                Object stackObject = stackMethod.invoke(slotResult);
                if (stackObject instanceof ItemStack stack && !stack.isEmpty()) {
                    stack.shrink(1);
                    return true;
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }

        return false;
    }
}