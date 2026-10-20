package fonnymunkey.gibbed.mixin.geckolib;

import com.mojang.blaze3d.vertex.PoseStack;
import fonnymunkey.gibbed.compat.geckolib.GeckoLibCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.util.RenderUtil;

@Mixin(value = RenderUtil.class, remap = false)
public abstract class RenderUtilMixin {

	@Inject(
			method = "translateMatrixToBone",
			at = @At("HEAD")
	)
	private static void gibbed_geckolibRenderUtil_translateMatrixToBone(PoseStack poseStack, GeoBone bone, CallbackInfo ci) {
		GeckoLibCompat.captureBone(poseStack, bone);
	}
}