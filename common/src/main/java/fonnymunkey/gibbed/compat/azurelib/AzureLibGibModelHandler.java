package fonnymunkey.gibbed.compat.azurelib;

import fonnymunkey.gibbed.client.gib.AbstractGib;
import fonnymunkey.gibbed.compat.BoneGibGenerator;
import fonnymunkey.gibbed.client.gib.handler.IGibModelHandler;
import mod.azure.azurelib.common.model.AzBakedModel;
import mod.azure.azurelib.common.model.AzBone;
import mod.azure.azurelib.common.render.entity.AzEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.LivingEntity;

import java.util.Collections;
import java.util.List;

public class AzureLibGibModelHandler implements IGibModelHandler {

	@Override
	public boolean canHandle(EntityRenderer<?> renderer) {
		return renderer instanceof AzEntityRenderer<?>;
	}

	@Override
	public Object getGibCacheKey(LivingEntity entity, EntityRenderer<?> renderer) {
		return AzureLibCompat.CAPTURE.getCapturedModel(entity);
	}

	@Override
	public List<AbstractGib> generateGibs(Object cacheKey, LivingEntity entity, EntityRenderer<?> renderer, float maxBranchSizeRatio, float maxBranchesRatio) {
		if(!(cacheKey instanceof AzBakedModel model)) return Collections.emptyList();
		return BoneGibGenerator.generateGibs(model.getTopLevelBones(), AzBone::getChildBones, AzGib::new, maxBranchSizeRatio, maxBranchesRatio);
	}

	@Override
	public void beginRenderStateCapture(LivingEntity entity, EntityRenderer<?> renderer) {
		AzureLibCompat.CAPTURE.beginRenderStateCapture(entity);
	}

	@Override
	public void endRenderStateCapture(LivingEntity entity, EntityRenderer<?> renderer) {
		AzureLibCompat.CAPTURE.endRenderStateCapture();
	}

	@Override
	public void clearCaptures() {
		AzureLibCompat.CAPTURE.clearCaptures();
	}
}