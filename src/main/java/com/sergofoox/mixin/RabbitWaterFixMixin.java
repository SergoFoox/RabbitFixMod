package com.sergofoox.mixin;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Rabbit.class)
public abstract class RabbitWaterFixMixin extends Animal {

    private static final long IDLE_HEAD_TILT_LENGTH_MS = 4000L;
    private static final long IDLE_HEAD_TILT_FADE_OUT_MS = 300L;

    protected RabbitWaterFixMixin(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "setupAnimationStates", at = @At("TAIL"))
    private void stopFinishedIdleHeadTiltAnimation(CallbackInfo ci) {
        Rabbit rabbit = (Rabbit) (Object) this;

        if (this.isInWater() || isIdleHeadTiltFinished(rabbit)) {
            rabbit.idleHeadTiltAnimationState.stop();
        }
    }

    private boolean isIdleHeadTiltFinished(Rabbit rabbit) {
        return rabbit.idleHeadTiltAnimationState.isStarted()
                && rabbit.idleHeadTiltAnimationState.getTimeInMillis(this.tickCount)
                >= IDLE_HEAD_TILT_LENGTH_MS + IDLE_HEAD_TILT_FADE_OUT_MS;
    }
}
