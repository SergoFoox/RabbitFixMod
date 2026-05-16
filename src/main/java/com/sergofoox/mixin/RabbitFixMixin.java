package com.sergofoox.mixin;

import net.minecraft.client.model.animal.rabbit.RabbitModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.RabbitRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RabbitModel.class)
public class RabbitFixMixin {

    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);
    private static final float ADULT_BODY_PITCH = (float) (Math.PI / 8.0);
    private static final long IDLE_HEAD_TILT_LENGTH_MS = 4000L;
    private static final float IDLE_HEAD_TILT_FADE_OUT_MS = 300.0F;

    @Final
    @Shadow private ModelPart head;

    @Inject(
            method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/RabbitRenderState;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/animation/KeyframeAnimation;apply(Lnet/minecraft/world/entity/AnimationState;F)V",
                    ordinal = 0
            )
    )

    private void fixAdultRabbitLookPitch(RabbitRenderState state, CallbackInfo ci) {
        if (!state.isBaby) {
            this.head.xRot += ADULT_BODY_PITCH;
        }
    }

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/RabbitRenderState;)V", at = @At("TAIL"))
    private void fadeIdleHeadTiltBackToLookDirection(RabbitRenderState state, CallbackInfo ci) {
        if (!state.idleHeadTiltAnimationState.isStarted()) {
            return;
        }

        long elapsedMillis = state.idleHeadTiltAnimationState.getTimeInMillis(state.ageInTicks);
        if (elapsedMillis < IDLE_HEAD_TILT_LENGTH_MS) {
            return;
        }

        float fade = Math.min((elapsedMillis - IDLE_HEAD_TILT_LENGTH_MS) / IDLE_HEAD_TILT_FADE_OUT_MS, 1.0F);
        this.head.yRot = lerp(fade, this.head.yRot, state.yRot * DEG_TO_RAD);
        this.head.xRot = lerp(fade, this.head.xRot, state.xRot * DEG_TO_RAD + (state.isBaby ? 0.0F : ADULT_BODY_PITCH));
    }

    private static float lerp(float delta, float start, float end) {
        return start + delta * (end - start);
    }
}
