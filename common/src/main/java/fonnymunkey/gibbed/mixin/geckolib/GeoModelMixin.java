package fonnymunkey.gibbed.mixin.geckolib;

import fonnymunkey.gibbed.compat.geckolib.GeckoLibCompat;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;

@Mixin(value = GeoModel.class, remap = false)
public abstract class GeoModelMixin {

	@Inject(
			method = "getBakedModel",
			at = @At("RETURN")
	)
	private void gibbed_geckolibGeoModel_getBakedModel(ResourceLocation location, CallbackInfoReturnable<BakedGeoModel> cir) {
		GeckoLibCompat.captureModel(cir.getReturnValue());
	}
}