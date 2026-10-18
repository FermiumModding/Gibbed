package fonnymunkey.gibbed.mixin.azurelib;

import fonnymunkey.gibbed.compat.azurelib.AzureLibCompat;
import mod.azure.azurelib.model.AzBone;
import mod.azure.azurelib.render.AzModelRenderer;
import mod.azure.azurelib.render.AzRendererPipelineContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AzModelRenderer.class)
public abstract class AzModelRendererMixin {
	
	@Inject(
			method = "render(Lmod/azure/azurelib/render/AzRendererPipelineContext;Z)V",
			at = @At("HEAD"),
			remap = false
	)
	private void gibbed_azurelibAzModelRenderer_renderHead(AzRendererPipelineContext<?,?> context, boolean isReRender, CallbackInfo ci) {
		AzureLibCompat.beginModelPass(context, isReRender);
	}
	
	@Inject(
			method = "render(Lmod/azure/azurelib/render/AzRendererPipelineContext;Z)V",
			at = @At("RETURN"),
			remap = false
	)
	private void gibbed_azurelibAzModelRenderer_renderReturn(AzRendererPipelineContext<?,?> context, boolean isReRender, CallbackInfo ci) {
		AzureLibCompat.endModelPass(context);
	}
	
	@Inject(
			method = "renderCubesOfBone(Lmod/azure/azurelib/render/AzRendererPipelineContext;Lmod/azure/azurelib/model/AzBone;)V",
			at = @At("HEAD"),
			remap = false
	)
	private void gibbed_azurelibAzModelRenderer_renderCubesOfBone(AzRendererPipelineContext<?,?> context, AzBone bone, CallbackInfo ci) {
		AzureLibCompat.captureBone(context, bone);
	}
}
