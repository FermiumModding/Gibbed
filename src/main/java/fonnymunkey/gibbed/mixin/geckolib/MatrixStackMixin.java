package fonnymunkey.gibbed.mixin.geckolib;

import fonnymunkey.gibbed.compat.geckolib.GeckolibCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib3.geo.render.built.GeoBone;
import software.bernie.geckolib3.util.MatrixStack;

@Mixin(MatrixStack.class)
public abstract class MatrixStackMixin {
	
	@Inject(
			method = "translate(Lsoftware/bernie/geckolib3/geo/render/built/GeoBone;)V",
			at = @At("HEAD"),
			remap = false
	)
	private void gibbed_geckolibMatrixStack_translate(GeoBone bone, CallbackInfo ci) {
		GeckolibCompat.captureBone(bone);
	}
}
