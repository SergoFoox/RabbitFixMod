package com.sergofoox.mixin;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.camel.CamelHusk;
import net.minecraft.world.entity.animal.equine.SkeletonHorse;
import net.minecraft.world.entity.animal.equine.ZombieHorse;
import net.minecraft.world.entity.animal.nautilus.ZombieNautilus;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Rabbit.class)
public abstract class RabbitMonsterAvoidanceFixMixin extends Animal {

    protected RabbitMonsterAvoidanceFixMixin(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void avoidMissingHostileMobs(CallbackInfo ci) {
        Rabbit rabbit = (Rabbit) (Object) this;
        this.goalSelector.addGoal(4, new MissingHostileMobAvoidGoal(rabbit));
    }

    private static boolean isMissingHostileMob(LivingEntity entity) {
        return entity instanceof Enemy && !(entity instanceof Monster)
                || entity instanceof CamelHusk
                || entity instanceof SkeletonHorse
                || entity instanceof ZombieHorse
                || entity instanceof ZombieNautilus;
    }

    private static final class MissingHostileMobAvoidGoal extends AvoidEntityGoal<LivingEntity> {

        private final Rabbit rabbit;

        private MissingHostileMobAvoidGoal(Rabbit rabbit) {
            super(rabbit, LivingEntity.class, RabbitMonsterAvoidanceFixMixin::isMissingHostileMob,
                    4.0F, 2.2D, 2.2D, entity -> true);
            this.rabbit = rabbit;
        }

        @Override
        public boolean canUse() {
            return this.rabbit.getVariant() != Rabbit.Variant.EVIL && super.canUse();
        }
    }
}
