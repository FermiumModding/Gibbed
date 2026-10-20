package fonnymunkey.gibbed.compat.geckolib;

import fonnymunkey.gibbed.client.gib.AbstractGib;
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
		BakedGeoModel model = GeckoLibCompat.getCapturedModel(entity);
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
		if(!(cacheKey instanceof BakedGeoModel model) || model.topLevelBones().isEmpty()) return Collections.emptyList();

		boolean nested = false;
		for(GeoBone bone : model.topLevelBones()) {
			if(!bone.getChildBones().isEmpty()) {
				nested = true;
				break;
			}
		}

		if(maxBranchSizeRatio <= 0.0F || maxBranchesRatio <= 0.0F || !nested) return generateGibsSimple(model.topLevelBones());
		else return generateGibsBranched(model.topLevelBones(), maxBranchSizeRatio, maxBranchesRatio);
	}

	@Override
	public void beginRenderStateCapture(LivingEntity entity, EntityRenderer<?> renderer) {
		GeckoLibCompat.beginRenderStateCapture(entity);
	}

	@Override
	public void endRenderStateCapture(LivingEntity entity, EntityRenderer<?> renderer) {
		GeckoLibCompat.endRenderStateCapture();
	}

	@Override
	public void clearCaptures() {
		GeckoLibCompat.clearCaptures();
	}

	private static List<AbstractGib> generateGibsSimple(List<GeoBone> rootBones) {
		List<AbstractGib> generatedGibs = new ArrayList<>();
		Queue<GeoBone> boneQueue = new LinkedList<>(rootBones);
		while(!boneQueue.isEmpty()) {
			GeoBone bone = boneQueue.poll();
			AbstractGib gib = new GeoGib(bone, false);
			if(gib.valid()) generatedGibs.add(gib);
			boneQueue.addAll(bone.getChildBones());
		}
		return generatedGibs;
	}

	private static List<AbstractGib> generateGibsBranched(List<GeoBone> rootBones, float maxBranchSizeRatio, float maxBranchesRatio) {
		float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
		float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
		for(GeoBone bone : rootBones) {
			AbstractGib fullGib = new GeoGib(bone, true);
			if(!fullGib.valid()) continue;
			minX = Math.min(minX, fullGib.cX(1.0F) - fullGib.dX(1.0F) / 2.0F);
			minY = Math.min(minY, fullGib.cY(1.0F) - fullGib.dY(1.0F) / 2.0F);
			minZ = Math.min(minZ, fullGib.cZ(1.0F) - fullGib.dZ(1.0F) / 2.0F);
			maxX = Math.max(maxX, fullGib.cX(1.0F) + fullGib.dX(1.0F) / 2.0F);
			maxY = Math.max(maxY, fullGib.cY(1.0F) + fullGib.dY(1.0F) / 2.0F);
			maxZ = Math.max(maxZ, fullGib.cZ(1.0F) + fullGib.dZ(1.0F) / 2.0F);
		}
		if(minX == Float.MAX_VALUE) return Collections.emptyList();
		float fullArea = (maxX - minX) * (maxY - minY) * (maxZ - minZ);
		float maxBranchSize = maxBranchSizeRatio * fullArea;
		float maxBranchFill = maxBranchesRatio * fullArea;

		List<AbstractGib> generatedGibs = new ArrayList<>();
		float branchFill = 0.0F;
		Queue<GeoBone> rootQueue = new LinkedList<>(rootBones);
		while(!rootQueue.isEmpty()) {
			GeoBone bone = rootQueue.poll();
			if(!bone.getChildBones().isEmpty() && branchFill < maxBranchFill) {
				AbstractGib gibBranch = new GeoGib(bone, true);
				if(gibBranch.valid()) {
					float branchSize = gibBranch.dX(1.0F) * gibBranch.dY(1.0F) * gibBranch.dZ(1.0F);
					if(branchSize <= maxBranchSize && branchFill + branchSize <= maxBranchFill) {
						generatedGibs.add(gibBranch);
						branchFill += branchSize;
						continue;
					}
				}
			}

			AbstractGib gibSingle = new GeoGib(bone, false);
			if(gibSingle.valid()) generatedGibs.add(gibSingle);

			rootQueue.addAll(bone.getChildBones());
		}
		return generatedGibs;
	}
}