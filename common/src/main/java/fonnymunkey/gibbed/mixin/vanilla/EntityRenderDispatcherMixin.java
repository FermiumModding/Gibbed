package fonnymunkey.gibbed.mixin.vanilla;

import com.mojang.blaze3d.vertex.PoseStack;
import fonnymunkey.gibbed.client.RenderCaptureHandler;
import fonnymunkey.gibbed.client.gib.handler.GibModelHandlers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

	@Shadow
	public abstract <T extends Entity> EntityRenderer<? super T> getRenderer(T entity);

	@Inject(
			method = "render",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V")
	)
	private void gibbed_vanillaEntityRenderDispatcher_render_pre(Entity entity, double x, double y, double z, float rotationYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
		if(entity instanceof LivingEntity living && !living.isAlive()) {
			RenderCaptureHandler.beginRenderStateCapture(living);
			RenderCaptureHandler.beginBaseTextureCapture();
			GibModelHandlers.beginRenderStateCapture(living, this.getRenderer(entity));
		}
	}

	@Inject(
			method = "render",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", shift = At.Shift.AFTER)
	)
	private void gibbed_vanillaEntityRenderDispatcher_render_post(Entity entity, double x, double y, double z, float rotationYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
		if(entity instanceof LivingEntity living && !living.isAlive()) {
			GibModelHandlers.endRenderStateCapture(living, this.getRenderer(entity));
			RenderCaptureHandler.endBaseTextureCapture();
			RenderCaptureHandler.endRenderStateCapture();
		}
	}
}