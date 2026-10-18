package fonnymunkey.gibbed.mixin.vanilla;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fonnymunkey.gibbed.client.RenderCaptureHandler;
import fonnymunkey.gibbed.compat.ModLoadedUtil;
import fonnymunkey.gibbed.compat.geckolib.GeckolibCompat;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RenderManager.class)
public abstract class RenderManagerMixin {
	
	@WrapOperation(
			method = "renderEntity",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/Render;doRender(Lnet/minecraft/entity/Entity;DDDFF)V")
	)
	private void gibbed_vanillaRenderManager_renderEntity_doRender(Render<Entity> instance, Entity entity, double x, double y, double z, float entityYaw, float partialTicks, Operation<Void> original) {
		if(entity instanceof EntityLivingBase && !entity.isEntityAlive()) {
			RenderCaptureHandler.beginRenderStateCapture((EntityLivingBase)entity);
			RenderCaptureHandler.beginBaseTextureCapture();
			if(ModLoadedUtil.isGeckolibLoaded() && GeckolibCompat.isRendererGeckolib(instance)) {
				GeckolibCompat.beginRenderStateCapture((EntityLivingBase)entity);
			}
		}
		original.call(instance, entity, x, y, z, entityYaw, partialTicks);
		if(entity instanceof EntityLivingBase && !entity.isEntityAlive()) {
			if(ModLoadedUtil.isGeckolibLoaded() && GeckolibCompat.isRendererGeckolib(instance)) {
				GeckolibCompat.endRenderStateCapture();
			}
			RenderCaptureHandler.endBaseTextureCapture();
			RenderCaptureHandler.endRenderStateCapture();
		}
	}
}