package fonnymunkey.gibbed.mixin.geckolib;

import fonnymunkey.gibbed.compat.geckolib.GeckolibCompat;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import software.bernie.geckolib3.geo.render.built.GeoModel;
import software.bernie.geckolib3.model.AnimatedGeoModel;

@Mixin(AnimatedGeoModel.class)
public abstract class AnimatedGeoModelMixin {
	
	@Inject(
			method = "getModel(Lnet/minecraft/util/ResourceLocation;)Lsoftware/bernie/geckolib3/geo/render/built/GeoModel;",
			at = @At("RETURN"),
			remap = false
	)
	private void gibbed_geckolibAnimatedGeoModel_getModel(ResourceLocation location, CallbackInfoReturnable<GeoModel> cir) {
		GeckolibCompat.captureModel(cir.getReturnValue());
	}
}
