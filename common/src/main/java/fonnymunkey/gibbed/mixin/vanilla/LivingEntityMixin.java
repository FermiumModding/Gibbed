package fonnymunkey.gibbed.mixin.vanilla;

import fonnymunkey.gibbed.client.EventHandlerClient;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

	@Shadow
	public int deathTime;

	@Inject(
			method = "baseTick",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;tickDeath()V")
	)
	private void gibbed_vanillaLivingEntity_baseTick_tickDeath(CallbackInfo ci) {
		LivingEntity entity = (LivingEntity)(Object)this;
		if(this.deathTime == 0 && entity.level().isClientSide()) {
			EventHandlerClient.onClientDeath(entity);
		}
	}
}