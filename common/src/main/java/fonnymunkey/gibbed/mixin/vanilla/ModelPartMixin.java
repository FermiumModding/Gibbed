package fonnymunkey.gibbed.mixin.vanilla;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fonnymunkey.gibbed.client.RenderCaptureHandler;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelPart.class)
public abstract class ModelPartMixin {

	@Shadow
	public boolean visible;

	@Inject(
			method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
			at = @At("HEAD")
	)
	private void gibbed_vanillaModelPart_render(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color, CallbackInfo ci) {
		if(this.visible && RenderCaptureHandler.isCapturing()) {
			RenderCaptureHandler.captureRenderer(this, 0.0625F);
		}
	}
}