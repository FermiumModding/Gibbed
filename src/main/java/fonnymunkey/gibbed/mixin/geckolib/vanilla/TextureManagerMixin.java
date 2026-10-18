package fonnymunkey.gibbed.mixin.geckolib.vanilla;

import fonnymunkey.gibbed.compat.geckolib.GeckolibCompat;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextureManager.class)
public abstract class TextureManagerMixin {
	
	@Inject(
			method = "bindTexture",
			at = @At("HEAD")
	)
	private void gibbed_geckolibTextureManager_bindTexture(ResourceLocation resource, CallbackInfo ci) {
		//Geckolib bypasses Render::bindTexture so duplicate here for gecko only handling
		GeckolibCompat.captureTexture(resource);
	}
}