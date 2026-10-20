package fonnymunkey.gibbed.mixin.azurelib;

import com.mojang.blaze3d.vertex.PoseStack;
import fonnymunkey.gibbed.compat.azurelib.AzureLibCompat;
import mod.azure.azurelib.model.AzBone;
import mod.azure.azurelib.util.client.RenderUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderUtils.class, remap = false)
public abstract class RenderUtilsMixin {

	@Inject(
			method = "translateMatrixToBone",
			at = @At("HEAD")
	)
	private static void gibbed_azurelibRenderUtils_translateMatrixToBone(PoseStack poseStack, AzBone bone, CallbackInfo ci) {
		AzureLibCompat.captureBone(poseStack, bone);
	}
}