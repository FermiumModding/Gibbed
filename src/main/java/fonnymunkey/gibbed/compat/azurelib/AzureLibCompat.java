package fonnymunkey.gibbed.compat.azurelib;

import fonnymunkey.gibbed.client.RenderCaptureHandler;
import fonnymunkey.gibbed.client.gib.AbstractGib;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import mod.azure.azurelib.loading.FileLoader;
import mod.azure.azurelib.loading.json.raw.MinecraftGeometry;
import mod.azure.azurelib.loading.json.raw.Model;
import mod.azure.azurelib.loading.json.raw.ModelProperties;
import mod.azure.azurelib.model.AzBakedModel;
import mod.azure.azurelib.model.AzBone;
import mod.azure.azurelib.model.cache.AzBakedModelCache;
import mod.azure.azurelib.render.AzRendererPipelineContext;
import mod.azure.azurelib.render.entity.AzEntityRenderer;
import mod.azure.azurelib.render.vertex.RenderType;
import mod.azure.azurelib.util.math.Matrix4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.nio.FloatBuffer;
import java.util.*;

/**
 * AzureLib counterpart to GeckolibCompat.
 * <p>
 * Differences from the Geckolib path, all driven by how AzureLib 1.12.2 renders:
 * <ul>
 * <li>Entity level transforms (rotation, config scale, child scale) live in AzureLib's software PoseStack rather than
 * the GL modelview, so the captured scale combines both.</li>
 * <li>Vertices are batched in AzBufferSource and textures are only bound at endBatch, after every bone has been
 * visited, so textures are read from the render context per bone instead of from TextureManager#bindTexture.</li>
 * <li>Each animated entity renders a deep copy of the shared baked model, so gibs are built from (and cached on) the
 * shared template in AzBakedModelCache. Copies share the template's bone metadata and AzBone#equals compares
 * name/parent/structure, so a captured per-entity bone still matches the template bone used as the gib's context
 * key.</li>
 * </ul>
 */
public abstract class AzureLibCompat {
	private static final float DEFAULT_TEXTURE_SIZE = 64.0F;
	private static final FloatBuffer MATRIX_BUFFER = BufferUtils.createFloatBuffer(16);
	private static final Int2ObjectOpenHashMap<ResourceLocation> capturedModelLocations = new Int2ObjectOpenHashMap<>();
	//Baked models don't keep the geo file's texture size, so read it from the raw file once per model
	private static final Map<AzBakedModel,float[]> textureSizes = new WeakHashMap<>();

	private static Integer captureEntity = null;
	private static boolean scaleCaptured = false;
	private static final ArrayDeque<Boolean> passStack = new ArrayDeque<>();

	public static boolean isRendererAzureLib(Render<?> renderer) {
		return renderer instanceof AzEntityRenderer;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static void beginRenderStateCapture(EntityLivingBase entity, Render<?> renderer) {
		captureEntity = entity.getEntityId();
		scaleCaptured = false;
		passStack.clear();
		if(!capturedModelLocations.containsKey((int)captureEntity) && renderer instanceof AzEntityRenderer) {
			try {
				ResourceLocation location = ((AzEntityRenderer)renderer).config().modelLocation(entity, entity);
				if(location != null) capturedModelLocations.put((int)captureEntity, location);
			}
			catch(Exception ignored) { }
		}
	}

	public static void endRenderStateCapture() {
		captureEntity = null;
		scaleCaptured = false;
		passStack.clear();
	}

	private static boolean isCapturing(AzRendererPipelineContext<?,?> context) {
		if(captureEntity == null || context == null) return false;
		Object animatable = context.animatable();
		//Filters out AzureLib items/blocks rendered through layers while the entity is being captured
		return animatable instanceof Entity && ((Entity)animatable).getEntityId() == captureEntity;
	}

	/**
	 * Called at the head of AzModelRenderer#render, where the pose stack holds the entity level transforms but none of
	 * the bone transforms yet.
	 */
	public static void beginModelPass(AzRendererPipelineContext<?,?> context, boolean isReRender) {
		if(!isCapturing(context)) return;
		passStack.push(isReRender);
		if(!isReRender && !scaleCaptured) {
			scaleCaptured = true;
			captureScale(context.poseStack().last().pose());
		}
	}

	public static void endModelPass(AzRendererPipelineContext<?,?> context) {
		if(!isCapturing(context)) return;
		if(!passStack.isEmpty()) passStack.pop();
	}

	/**
	 * Called at the head of AzModelRenderer#renderCubesOfBone, which only runs for bones that actually draw their cubes.
	 */
	@SuppressWarnings({"rawtypes"})
	public static void captureBone(AzRendererPipelineContext context, AzBone bone) {
		if(bone == null || bone.isHidden() || !isCapturing(context)) return;
		if(bone.getScaleX() == 0.0F && bone.getScaleY() == 0.0F && bone.getScaleZ() == 0.0F) return;

		boolean layerPass = !passStack.isEmpty() && passStack.peek();
		ResourceLocation texture = getPassTexture(context, layerPass);

		if(layerPass) {
			//Re-renders are layers (auto glow etc.), record them as the layer texture without touching the base
			RenderCaptureHandler.endBaseTextureCapture();
			RenderCaptureHandler.beginLayerTextureCapture();
			if(texture != null) RenderCaptureHandler.captureTexture(texture);
			RenderCaptureHandler.captureRenderer(bone, 1.0F);
			RenderCaptureHandler.endLayerTextureCapture();
			RenderCaptureHandler.beginBaseTextureCapture();
		}
		else {
			if(texture != null) RenderCaptureHandler.captureTexture(texture);
			RenderCaptureHandler.captureRenderer(bone, 1.0F);
		}
	}

	@Nullable
	@SuppressWarnings({"rawtypes", "unchecked"})
	private static ResourceLocation getPassTexture(AzRendererPipelineContext context, boolean layerPass) {
		if(!layerPass && context.getTextureOverride() != null) return context.getTextureOverride();
		RenderType renderType = context.renderType();
		if(renderType != null && renderType.texture() != null) return renderType.texture();
		if(layerPass) return null;
		try {
			return context.rendererPipeline().config().textureLocation(context.currentEntity(), context.animatable());
		}
		catch(Exception ignored) {
			return null;
		}
	}

	public static void clearCaptures() {
		capturedModelLocations.clear();
		captureEntity = null;
		scaleCaptured = false;
		passStack.clear();
	}

	private static void captureScale(Matrix4f pose) {
		MATRIX_BUFFER.clear();
		GlStateManager.getFloat(GL11.GL_MODELVIEW_MATRIX, MATRIX_BUFFER);
		//GL is column-major, AzureLib's Matrix4f is mRowCol; combine the upper 3x3 of GL * pose and measure each axis
		float scaleX = combinedAxisLength(pose.m00, pose.m10, pose.m20);
		float scaleY = combinedAxisLength(pose.m01, pose.m11, pose.m21);
		float scaleZ = combinedAxisLength(pose.m02, pose.m12, pose.m22);
		if(scaleX <= 0.0F || scaleY <= 0.0F || scaleZ <= 0.0F) return;
		RenderCaptureHandler.beginScaleCapture();
		RenderCaptureHandler.captureScale(scaleX, scaleY, scaleZ);
		RenderCaptureHandler.endScaleCapture();
	}

	private static float combinedAxisLength(float c0, float c1, float c2) {
		float x = MATRIX_BUFFER.get(0) * c0 + MATRIX_BUFFER.get(4) * c1 + MATRIX_BUFFER.get(8) * c2;
		float y = MATRIX_BUFFER.get(1) * c0 + MATRIX_BUFFER.get(5) * c1 + MATRIX_BUFFER.get(9) * c2;
		float z = MATRIX_BUFFER.get(2) * c0 + MATRIX_BUFFER.get(6) * c1 + MATRIX_BUFFER.get(10) * c2;
		return (float)Math.sqrt(x * x + y * y + z * z);
	}

	@Nullable
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static Object getGibCacheKey(EntityLivingBase entity, Render<?> renderer) {
		ResourceLocation location = capturedModelLocations.get(entity.getEntityId());
		if(location == null && renderer instanceof AzEntityRenderer) {
			try {
				location = ((AzEntityRenderer)renderer).config().modelLocation(entity, entity);
			}
			catch(Exception ignored) { }
		}
		if(location == null) return null;

		//Shared template, never animated itself (animators work on deep copies) so it stays at rest pose
		AzBakedModel model = AzBakedModelCache.getInstance().getNullable(location);
		if(model == null) return null;
		if(!textureSizes.containsKey(model)) textureSizes.put(model, loadTextureSize(location));
		return model;
	}

	private static float[] loadTextureSize(ResourceLocation location) {
		float[] size = new float[]{ DEFAULT_TEXTURE_SIZE, DEFAULT_TEXTURE_SIZE };
		try {
			Model raw = FileLoader.loadModelFile(location, Minecraft.getMinecraft().getResourceManager());
			if(raw == null || raw.minecraftGeometry() == null) return size;
			for(MinecraftGeometry geometry : raw.minecraftGeometry()) {
				ModelProperties properties = geometry == null ? null : geometry.modelProperties();
				if(properties == null) continue;
				if(properties.textureWidth() > 0) size[0] = (float)properties.textureWidth();
				if(properties.textureHeight() > 0) size[1] = (float)properties.textureHeight();
				break;
			}
		}
		catch(Exception ignored) { }
		return size;
	}

	public static List<AbstractGib> generateGibs(Object cacheKey, float maxBranchSizeRatio, float maxBranchesRatio) {
		if(!(cacheKey instanceof AzBakedModel)) return Collections.emptyList();
		AzBakedModel model = (AzBakedModel)cacheKey;
		List<AzBone> topLevelBones = model.getTopLevelBones();
		if(topLevelBones == null || topLevelBones.isEmpty()) return Collections.emptyList();

		float[] size = textureSizes.get(model);
		float textureWidth = size != null ? size[0] : DEFAULT_TEXTURE_SIZE;
		float textureHeight = size != null ? size[1] : DEFAULT_TEXTURE_SIZE;

		boolean nested = false;
		for(AzBone bone : topLevelBones) {
			if(!bone.getChildBones().isEmpty()) {
				nested = true;
				break;
			}
		}

		if(maxBranchSizeRatio <= 0.0F || maxBranchesRatio <= 0.0F || !nested) return generateGibsSimple(topLevelBones, textureWidth, textureHeight);
		else return generateGibsBranched(topLevelBones, textureWidth, textureHeight, maxBranchSizeRatio, maxBranchesRatio);
	}

	private static List<AbstractGib> generateGibsSimple(List<AzBone> rootBones, float textureWidth, float textureHeight) {
		List<AbstractGib> generatedGibs = new ArrayList<>();
		Queue<AzBone> boneQueue = new LinkedList<>(rootBones);
		while(!boneQueue.isEmpty()) {
			AzBone bone = boneQueue.poll();
			AbstractGib gib = new AzGib(bone, false, textureWidth, textureHeight);
			if(gib.valid()) generatedGibs.add(gib);
			boneQueue.addAll(bone.getChildBones());
		}
		return generatedGibs;
	}

	private static List<AbstractGib> generateGibsBranched(List<AzBone> rootBones, float textureWidth, float textureHeight, float maxBranchSizeRatio, float maxBranchesRatio) {
		float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
		float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
		for(AzBone bone : rootBones) {
			AbstractGib fullGib = new AzGib(bone, true, textureWidth, textureHeight);
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
		Queue<AzBone> rootQueue = new LinkedList<>(rootBones);
		while(!rootQueue.isEmpty()) {
			AzBone bone = rootQueue.poll();
			if(!bone.getChildBones().isEmpty() && branchFill < maxBranchFill) {
				AbstractGib gibBranch = new AzGib(bone, true, textureWidth, textureHeight);
				if(gibBranch.valid()) {
					float branchSize = gibBranch.dX(1.0F) * gibBranch.dY(1.0F) * gibBranch.dZ(1.0F);
					if(branchSize <= maxBranchSize && branchFill + branchSize <= maxBranchFill) {
						generatedGibs.add(gibBranch);
						branchFill += branchSize;
						continue;
					}
				}
			}

			AbstractGib gibSingle = new AzGib(bone, false, textureWidth, textureHeight);
			if(gibSingle.valid()) generatedGibs.add(gibSingle);

			rootQueue.addAll(bone.getChildBones());
		}
		return generatedGibs;
	}
}
