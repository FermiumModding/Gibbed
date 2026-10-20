package fonnymunkey.gibbed.compat.geckolib;

import fonnymunkey.gibbed.client.gib.AbstractGib;
import fonnymunkey.gibbed.compat.BoneGibGenerator;
import fonnymunkey.gibbed.client.gib.handler.IGibModelHandler;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;

import java.util.*;

public class GeckoLibGibModelHandler implements IGibModelHandler {

	@Override
	public boolean canHandle(EntityRenderer<?> renderer) {
		return renderer instanceof GeoRenderer<?>;
	}

	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	public Object getGibCacheKey(LivingEntity entity, EntityRenderer<?> renderer) {
		BakedGeoModel model = GeckoLibCompat.CAPTURE.getCapturedModel(entity);
		if(model != null) return model;
		if(renderer instanceof GeoEntityRenderer geoRenderer && entity instanceof GeoAnimatable animatable) {
			try {
				GeoModel geoModel = geoRenderer.getGeoModel();
				return geoModel.getBakedModel(geoModel.getModelResource(animatable));
			}
			catch(Exception ignored) { }
		}
		return null;
	}

	@Override
	public List<AbstractGib> generateGibs(Object cacheKey, LivingEntity entity, EntityRenderer<?> renderer, float maxBranchSizeRatio, float maxBranchesRatio) {
		if(!(cacheKey instanceof BakedGeoModel model)) return Collections.emptyList();
		return BoneGibGenerator.generateGibs(model.topLevelBones(), GeoBone::getChildBones, GeoGib::new, maxBranchSizeRatio, maxBranchesRatio);
	}

	@Override
	public void beginRenderStateCapture(LivingEntity entity, EntityRenderer<?> renderer) {
		GeckoLibCompat.CAPTURE.beginRenderStateCapture(entity);
	}

	@Override
	public void endRenderStateCapture(LivingEntity entity, EntityRenderer<?> renderer) {
		GeckoLibCompat.CAPTURE.endRenderStateCapture();
	}

	@Override
	public void clearCaptures() {
		GeckoLibCompat.CAPTURE.clearCaptures();
	}
}