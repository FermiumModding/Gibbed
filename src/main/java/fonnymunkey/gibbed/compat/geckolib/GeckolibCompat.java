package fonnymunkey.gibbed.compat.geckolib;

import fonnymunkey.gibbed.client.RenderCaptureHandler;
import fonnymunkey.gibbed.client.gib.AbstractGib;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import software.bernie.geckolib3.geo.render.built.GeoBone;
import software.bernie.geckolib3.geo.render.built.GeoModel;
import software.bernie.geckolib3.model.provider.GeoModelProvider;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;
import software.bernie.geckolib3.renderers.geo.IGeoRenderer;

import javax.annotation.Nullable;
import java.nio.FloatBuffer;
import java.util.*;

public abstract class GeckolibCompat {
	private static final float DEFAULT_TEXTURE_SIZE = 64.0F;
	private static final FloatBuffer MATRIX_BUFFER = BufferUtils.createFloatBuffer(16);
	private static final Int2ObjectOpenHashMap<GeoModel> capturedModels = new Int2ObjectOpenHashMap<>();

	private static Integer captureEntity = null;
	private static boolean scaleCaptured = false;
	private static ResourceLocation capturedBaseTexture = null;

	public static boolean isRendererGeckolib(Render<?> renderer) {
		return renderer instanceof IGeoRenderer;
	}

	public static void beginRenderStateCapture(EntityLivingBase entity) {
		captureEntity = entity.getEntityId();
		scaleCaptured = false;
		capturedBaseTexture = null;
	}

	public static void endRenderStateCapture() {
		captureEntity = null;
		scaleCaptured = false;
		capturedBaseTexture = null;
	}

	public static void captureModel(GeoModel model) {
		if(captureEntity != null && model != null && !capturedModels.containsKey((int)captureEntity)) {
			capturedModels.put((int)captureEntity, model);
		}
	}

	public static void captureBone(GeoBone bone) {
		if(captureEntity == null || bone == null || bone.isHidden()) return;
		if(!scaleCaptured) {
			scaleCaptured = true;
			captureGlScale();
		}
		RenderCaptureHandler.captureRenderer(bone, 1.0F);
	}

	public static void captureTexture(ResourceLocation location) {
		if(captureEntity == null || location == null) return;
		if(capturedBaseTexture == null) {
			capturedBaseTexture = location;
			RenderCaptureHandler.captureTexture(location);
		}
		else if(location.equals(capturedBaseTexture)) {
			RenderCaptureHandler.endLayerTextureCapture();
			RenderCaptureHandler.beginBaseTextureCapture();
			RenderCaptureHandler.captureTexture(location);
		}
		else {
			RenderCaptureHandler.endBaseTextureCapture();
			RenderCaptureHandler.beginLayerTextureCapture();
			RenderCaptureHandler.captureTexture(location);
		}
	}

	public static void clearCaptures() {
		capturedModels.clear();
		captureEntity = null;
	}

	private static void captureGlScale() {
		MATRIX_BUFFER.clear();
		GlStateManager.getFloat(GL11.GL_MODELVIEW_MATRIX, MATRIX_BUFFER);
		float scaleX = axisLength(MATRIX_BUFFER.get(0), MATRIX_BUFFER.get(1), MATRIX_BUFFER.get(2));
		float scaleY = axisLength(MATRIX_BUFFER.get(4), MATRIX_BUFFER.get(5), MATRIX_BUFFER.get(6));
		float scaleZ = axisLength(MATRIX_BUFFER.get(8), MATRIX_BUFFER.get(9), MATRIX_BUFFER.get(10));
		if(scaleX <= 0.0F || scaleY <= 0.0F || scaleZ <= 0.0F) return;
		RenderCaptureHandler.beginScaleCapture();
		RenderCaptureHandler.captureScale(scaleX, scaleY, scaleZ);
		RenderCaptureHandler.endScaleCapture();
	}

	private static float axisLength(float x, float y, float z) {
		return (float)Math.sqrt(x * x + y * y + z * z);
	}

	@Nullable
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static Object getGibCacheKey(EntityLivingBase entity, Render<?> renderer) {
		GeoModel model = capturedModels.get(entity.getEntityId());
		if(model != null) return model;
		if(renderer instanceof GeoEntityRenderer) {
			try {
				GeoModelProvider provider = ((GeoEntityRenderer)renderer).getGeoModelProvider();
				return provider.getModel(provider.getModelLocation(entity));
			}
			catch(Exception ignored) { }
		}
		return null;
	}

	public static List<AbstractGib> generateGibs(Object cacheKey, float maxBranchSizeRatio, float maxBranchesRatio) {
		if(!(cacheKey instanceof GeoModel)) return Collections.emptyList();
		GeoModel model = (GeoModel)cacheKey;
		if(model.topLevelBones == null || model.topLevelBones.isEmpty()) return Collections.emptyList();

		float textureWidth = DEFAULT_TEXTURE_SIZE;
		float textureHeight = DEFAULT_TEXTURE_SIZE;
		if(model.properties != null) {
			if(model.properties.getTextureWidth() != null) textureWidth = model.properties.getTextureWidth().floatValue();
			if(model.properties.getTextureHeight() != null) textureHeight = model.properties.getTextureHeight().floatValue();
		}

		boolean nested = false;
		for(GeoBone bone : model.topLevelBones) {
			if(!bone.childBones.isEmpty()) {
				nested = true;
				break;
			}
		}

		if(maxBranchSizeRatio <= 0.0F || maxBranchesRatio <= 0.0F || !nested) return generateGibsSimple(model.topLevelBones, textureWidth, textureHeight);
		else return generateGibsBranched(model.topLevelBones, textureWidth, textureHeight, maxBranchSizeRatio, maxBranchesRatio);
	}

	private static List<AbstractGib> generateGibsSimple(List<GeoBone> rootBones, float textureWidth, float textureHeight) {
		List<AbstractGib> generatedGibs = new ArrayList<>();
		Queue<GeoBone> boneQueue = new LinkedList<>(rootBones);
		while(!boneQueue.isEmpty()) {
			GeoBone bone = boneQueue.poll();
			AbstractGib gib = new GeoGib(bone, false, textureWidth, textureHeight);
			if(gib.valid()) generatedGibs.add(gib);
			boneQueue.addAll(bone.childBones);
		}
		return generatedGibs;
	}

	private static List<AbstractGib> generateGibsBranched(List<GeoBone> rootBones, float textureWidth, float textureHeight, float maxBranchSizeRatio, float maxBranchesRatio) {
		float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
		float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
		for(GeoBone bone : rootBones) {
			AbstractGib fullGib = new GeoGib(bone, true, textureWidth, textureHeight);
			if(!fullGib.valid()) continue;
			minX = Math.min(minX, fullGib.cX(1.0F) - fullGib.dX(1.0F) / 2.0F);
			minY = Math.min(minY, fullGib.cY(1.0F) - fullGib.dY(1.0F) / 2.0F);
			minZ = Math.min(minZ, fullGib.cZ(1.0F) - fullGib.dZ(1.0F) / 2.0F);
			maxX = Math.max(maxX, fullGib.cX(1.0F) + fullGib.dX(1.0F) / 2.0F);
			maxY = Math.max(maxY, fullGib.cY(1.0F) + fullGib.dY(1.0F) / 2.0F);
			maxZ = Math.max(maxZ, fullGib.cZ(1.0F) + fullGib.dZ(1.0F) / 2.0F);
		}
		if(minX == Float.MAX_VALUE || minY == Float.MAX_VALUE || minZ == Float.MAX_VALUE ||
				maxX == -Float.MAX_VALUE || maxY == -Float.MAX_VALUE || maxZ == -Float.MAX_VALUE) return Collections.emptyList();
		float fullArea = (maxX - minX) * (maxY - minY) * (maxZ - minZ);
		float maxBranchSize = maxBranchSizeRatio * fullArea;
		float maxBranchFill = maxBranchesRatio * fullArea;

		List<AbstractGib> generatedGibs = new ArrayList<>();
		float branchFill = 0.0F;
		Queue<GeoBone> rootQueue = new LinkedList<>(rootBones);
		while(!rootQueue.isEmpty()) {
			GeoBone bone = rootQueue.poll();
			if(!bone.childBones.isEmpty() && branchFill < maxBranchFill) {
				AbstractGib gibBranch = new GeoGib(bone, true, textureWidth, textureHeight);
				if(gibBranch.valid()) {
					float branchSize = gibBranch.dX(1.0F) * gibBranch.dY(1.0F) * gibBranch.dZ(1.0F);
					if(branchSize <= maxBranchSize && branchFill + branchSize <= maxBranchFill) {
						generatedGibs.add(gibBranch);
						branchFill += branchSize;
						continue;
					}
				}
			}

			AbstractGib gibSingle = new GeoGib(bone, false, textureWidth, textureHeight);
			if(gibSingle.valid()) generatedGibs.add(gibSingle);

			rootQueue.addAll(bone.childBones);
		}
		return generatedGibs;
	}
}
