package fonnymunkey.gibbed.mixin.uranus;

import com.iafenvoy.uranus.client.model.AdvancedModelBox;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fonnymunkey.gibbed.client.RenderCaptureHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AdvancedModelBox.class, remap = false)
public abstract class AdvancedModelBoxMixin {

	@Inject(
			method = "render",
			at = @At("HEAD")
	)
	private void gibbed_uranusAdvancedModelBox_render(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, CallbackInfo ci) {
		if(((AdvancedModelBox)(Object)this).showModel && RenderCaptureHandler.isCapturing()) {
			RenderCaptureHandler.captureRenderer(this, 0.0625F);
		}
	}
}