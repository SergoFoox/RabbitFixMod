package com.sergofoox.mixin;

import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.Predicate;

@Mixin(Rabbit.class)
public class RabbitTemptGoalFixMixin {

    @ModifyArg(
            method = "registerGoals",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ai/goal/TemptGoal;<init>(Lnet/minecraft/world/entity/PathfinderMob;DLjava/util/function/Predicate;Z)V"
            ),
            index = 2
    )
    private Predicate<ItemStack> includeCarrotOnAStick(Predicate<ItemStack> rabbitFood) {
        return rabbitFood.or(stack -> stack.is(Items.CARROT_ON_A_STICK));
    }
}
