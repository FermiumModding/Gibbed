package fonnymunkey.gibbed.mixin.geckolib;

import com.mojang.blaze3d.vertex.PoseStack;
import fonnymunkey.gibbed.compat.geckolib.GeckoLibCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.util.RenderUtils;

@Mixin(value = RenderUtils.class, remap = false)
public abstract class RenderUtilsMixin {

	@Inject(
			method = "translateMatrixToBone",
			at = @At("HEAD")
	)
	private static void gibbed_geckolibRenderUtils_translateMatrixToBone(PoseStack poseStack, CoreGeoBone bone, CallbackInfo ci) {
		GeckoLibCompat.captureBone(poseStack, bone);
	}
}