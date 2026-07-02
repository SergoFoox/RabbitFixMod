package com.sergofoox.mixin;

import com.sergofoox.goal.RabbitCropRaidGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Rabbit.class)
public class RabbitCropRaidGoalMixin {

    @Redirect(
            method = "registerGoals",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ai/goal/GoalSelector;addGoal(ILnet/minecraft/world/entity/ai/goal/Goal;)V",
                    ordinal = 8
            )
    )
    private void replaceBrokenCropGoal(GoalSelector selector, int priority, Goal ignored) {
        selector.addGoal(priority, new RabbitCropRaidGoal((Rabbit) (Object) this));
    }
}
