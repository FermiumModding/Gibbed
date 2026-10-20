package fonnymunkey.gibbed.mixin.azurelib;

import fonnymunkey.gibbed.compat.azurelib.AzureLibCompat;
import mod.azure.azurelib.model.AzBakedModel;
import mod.azure.azurelib.render.AzProvider;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AzProvider.class, remap = false)
public abstract class AzProviderMixin {

	@Inject(
			method = "provideBakedModel",
			at = @At("RETURN")
	)
	private void gibbed_azurelibAzProvider_provideBakedModel(Entity entity, Object animatable, CallbackInfoReturnable<AzBakedModel> cir) {
		AzureLibCompat.captureModel(cir.getReturnValue());
	}
}