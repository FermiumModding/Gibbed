package fonnymunkey.gibbed.client.gib;

import fonnymunkey.gibbed.Gibbed;
import fonnymunkey.gibbed.client.RenderCaptureHandler;
import fonnymunkey.gibbed.compat.azurelib.AzureLibCompat;
import fonnymunkey.gibbed.compat.geckolib.GeckolibCompat;
import fonnymunkey.gibbed.compat.lycanites.LycanitesCompat;
import fonnymunkey.gibbed.compat.mobends.MobendsCompat;
import fonnymunkey.gibbed.compat.ModLoadedUtil;
import fonnymunkey.gibbed.config.ConfigHandler;
import fonnymunkey.gibbed.mixin.vanilla.RenderInvoker;
import fonnymunkey.gibbed.util.IModelRenderer;
import fonnymunkey.gibbed.util.Pair;
import fonnymunkey.gibbed.util.Triple;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.*;

public class GibGenerator {
	public static final ResourceLocation GORE_TEXTURE = new ResourceLocation(Gibbed.MODID, "textures/gore.png");
	private static final Map<Object,List<AbstractGib>> cachedGibs = new HashMap<>();
	
	public static void resetCache() {
		cachedGibs.clear();
	}
	
	public static Map<AbstractGib,Triple<float[],ResourceLocation,ResourceLocation>> getGibsFromEntity(EntityLivingBase entity) {
		Map<Object,Triple<float[],ResourceLocation,ResourceLocation>> capturedRenderers = RenderCaptureHandler.getCapturedRenderers(entity);
		if(capturedRenderers == null && ConfigHandler.MODELS.onlyGibContextCapturedEntities) return Collections.emptyMap();
		
		//blagh
		float[] mobendFirstScale = null;
		if(capturedRenderers != null && ModLoadedUtil.isMobendsLoaded()) {
			for(Map.Entry<Object,Triple<float[],ResourceLocation,ResourceLocation>> entry : capturedRenderers.entrySet()) {
				if(entry.getKey() instanceof ModelRenderer && MobendsCompat.isRendererWrapped((ModelRenderer)entry.getKey())) {
					mobendFirstScale = entry.getValue().left;
					break;
				}
			}
		}
		
		if(capturedRenderers == null) {
			Map<AbstractGib,Triple<float[],ResourceLocation,ResourceLocation>> activeGibs = new HashMap<>();
			for(AbstractGib gib : getCachedGibsFromEntity(entity)) {
				ResourceLocation baseTexture = ConfigHandler.TEXTURES.useGoreTextures ? GORE_TEXTURE : getEntityTexture(entity);
				if(baseTexture == null) continue;
				float[] scales = gib.getDefaultScales();
				if(scales == null) continue;
				activeGibs.put(gib, new Triple<>(scales, baseTexture, null));
			}
			return activeGibs;
		}
		else if(mobendFirstScale != null) {
			return MobendsCompat.getGibsFromEntity(entity, capturedRenderers, getCachedGibsFromEntity(entity), mobendFirstScale);
		}
		else {
			Map<AbstractGib,Triple<float[],ResourceLocation,ResourceLocation>> activeGibs = new HashMap<>();
			for(AbstractGib gib : getCachedGibsFromEntity(entity)) {
				Triple<float[],ResourceLocation,ResourceLocation> context = capturedRenderers.get(gib.getContextKey());
				if(context == null) continue;
				ResourceLocation baseTexture = context.middle;
				ResourceLocation layerTexture = context.right;
				if(baseTexture == null) {
					if(layerTexture != null) {
						baseTexture = layerTexture;
						layerTexture = null;
					}
					else baseTexture = getEntityTexture(entity);
				}
				if(ConfigHandler.TEXTURES.useGoreTextures) {
					baseTexture = GORE_TEXTURE;
					layerTexture = null;
				}
				if(baseTexture == null) continue;
				if(layerTexture != null && !ConfigHandler.shouldLayerTextureBeUsed(entity)) layerTexture = null;
				float[] scales = context.left;
				if(scales == null) scales = gib.getDefaultScales();
				if(scales != null && scales[0] != 0.0F) {
					context.left = scales;
					context.middle = baseTexture;
					context.right = layerTexture;
					activeGibs.put(gib, context);
				}
			}
			return activeGibs;
		}
	}
	
	@Nullable
	public static ResourceLocation getEntityTexture(EntityLivingBase entity) {
		Render<?> renderer = Minecraft.getMinecraft().getRenderManager().getEntityRenderObject(entity);
		if(renderer instanceof RenderLivingBase
				|| (ModLoadedUtil.isGeckolibLoaded() && GeckolibCompat.isRendererGeckolib(renderer))
				|| (ModLoadedUtil.isAzureLibLoaded() && AzureLibCompat.isRendererAzureLib(renderer))) {
			return ((RenderInvoker)renderer).gibbed$invokeGetEntityTexture(entity);
		}
		return null;
	}

	private static List<AbstractGib> getCachedGibsFromEntity(EntityLivingBase entity) {
		Render<? extends Entity> entityRenderer = Minecraft.getMinecraft().getRenderManager().getEntityRenderObject(entity);
		if(entityRenderer instanceof RenderLivingBase) {
			RenderLivingBase<? extends EntityLivingBase> entityRendererLiving = (RenderLivingBase<? extends EntityLivingBase>)entityRenderer;
			if(!cachedGibs.containsKey(entityRendererLiving)) {
				Pair<Float,Float> ratios = getBranchRatios(entity);
				cachedGibs.put(entityRendererLiving, generateGibs(entityRendererLiving, ratios.left, ratios.right));
			}
			return cachedGibs.get(entityRendererLiving);
		}
		else if(ModLoadedUtil.isGeckolibLoaded() && GeckolibCompat.isRendererGeckolib(entityRenderer)) {
			Object cacheKey = GeckolibCompat.getGibCacheKey(entity, entityRenderer);
			if(cacheKey == null) return Collections.emptyList();
			if(!cachedGibs.containsKey(cacheKey)) {
				Pair<Float,Float> ratios = getBranchRatios(entity);
				cachedGibs.put(cacheKey, GeckolibCompat.generateGibs(cacheKey, ratios.left, ratios.right));
			}
			return cachedGibs.get(cacheKey);
		}
		else if(ModLoadedUtil.isAzureLibLoaded() && AzureLibCompat.isRendererAzureLib(entityRenderer)) {
			Object cacheKey = AzureLibCompat.getGibCacheKey(entity, entityRenderer);
			if(cacheKey == null) return Collections.emptyList();
			if(!cachedGibs.containsKey(cacheKey)) {
				Pair<Float,Float> ratios = getBranchRatios(entity);
				cachedGibs.put(cacheKey, AzureLibCompat.generateGibs(cacheKey, ratios.left, ratios.right));
			}
			return cachedGibs.get(cacheKey);
		}
		return Collections.emptyList();
	}

	private static Pair<Float,Float> getBranchRatios(EntityLivingBase entity) {
		Pair<Float,Float> ratioOverride = ConfigHandler.getEntityBranchRatioOverride(entity);
		if(ratioOverride != null) return ratioOverride;
		return new Pair<>((float)ConfigHandler.MODELS.maxBranchSizeRatio, (float)ConfigHandler.MODELS.maxBranchesRatio);
	}
	
	private static List<AbstractGib> generateGibs(RenderLivingBase<? extends EntityLivingBase> entityRendererLiving, float maxBranchSizeRatio, float maxBranchesRatio) {
		List<ModelRenderer> originalFlatList = entityRendererLiving.getMainModel().boxList;
		if(originalFlatList == null || originalFlatList.isEmpty()) {
			//I think? lycanites can render with normal renderers when using things like redux?
			//Only render as obj if no normal renderers found
			if(ModLoadedUtil.isLycanitesLoaded() && LycanitesCompat.isRendererLycanite(entityRendererLiving)) {
				return LycanitesCompat.generateGibsForLycanite(entityRendererLiving);
			}
			else return Collections.emptyList();
		}
		
		boolean mobendsModified = false;
		Set<ModelRenderer> nestedModels = new HashSet<>();
		for(ModelRenderer renderer : originalFlatList) {
			if(ModLoadedUtil.isMobendsLoaded() && MobendsCompat.isRendererWrapped(renderer)) {
				mobendsModified = true;
				break;
			}
			if(renderer.childModels != null) {
				nestedModels.addAll(renderer.childModels);
			}
		}
		
		if(maxBranchSizeRatio <= 0.0F || maxBranchesRatio <= 0.0F || nestedModels.isEmpty() || mobendsModified) return generateGibsSimple(originalFlatList);
		else return generateGibsBranched(originalFlatList, nestedModels, maxBranchSizeRatio, maxBranchesRatio);
	}
	
	private static List<AbstractGib> generateGibsSimple(List<ModelRenderer> originalFlatList) {
		List<AbstractGib> generatedGibs = new ArrayList<>();
		for(ModelRenderer renderer : originalFlatList) {
			if(ModLoadedUtil.isMobendsLoaded() && MobendsCompat.isRendererWrapped(renderer)) continue;
			AbstractGib gib = new BasicGib(renderer, renderer.childModels != null);
			if(gib.valid()) generatedGibs.add(gib);
		}
		return generatedGibs;
	}
	
	private static List<AbstractGib> generateGibsBranched(List<ModelRenderer> originalFlatList, Set<ModelRenderer> nestedModels, float maxBranchSizeRatio, float maxBranchesRatio) {
		List<ModelRenderer> rootModels = new ArrayList<>();
		for(ModelRenderer renderer : originalFlatList) {
			if(!nestedModels.contains(renderer)) {
				rootModels.add(renderer);
			}
		}
		
		FullBody fullBody = new FullBody(rootModels);
		if(!fullBody.valid()) return Collections.emptyList();
		float fullArea = fullBody.dX() * fullBody.dY() * fullBody.dZ();
		float maxBranchSize = maxBranchSizeRatio * fullArea;
		float maxBranchFill = maxBranchesRatio * fullArea;
		
		List<AbstractGib> generatedGibs = new ArrayList<>();
		float branchFill = 0.0F;
		Queue<ModelRenderer> rootQueue = new LinkedList<>(rootModels);
		while(!rootQueue.isEmpty()) {
			ModelRenderer renderer = rootQueue.poll();
			if(renderer.childModels != null && branchFill < maxBranchFill) {
				AbstractGib gibBranch = new BasicGib(renderer, true);
				if(!gibBranch.valid()) continue;
				float branchSize = gibBranch.dX(1.0F) * gibBranch.dY(1.0F) * gibBranch.dZ(1.0F);
				if(branchSize <= maxBranchSize && branchFill + branchSize <= maxBranchFill) {
					generatedGibs.add(gibBranch);
					branchFill += branchSize;
					continue;
				}
			}
			
			AbstractGib gibSingle = new BasicGib(renderer, false);
			if(gibSingle.valid()) generatedGibs.add(gibSingle);
			
			if(renderer.childModels != null) {
				rootQueue.addAll(renderer.childModels);
			}
		}
		return generatedGibs;
	}
	
	//Lazy way to get full gib model bounds
	protected static class FullBody {
		private float minX = Float.MAX_VALUE;
		private float minY = Float.MAX_VALUE;
		private float minZ = Float.MAX_VALUE;
		private float maxX = -Float.MAX_VALUE;
		private float maxY = -Float.MAX_VALUE;
		private float maxZ = -Float.MAX_VALUE;
		
		protected FullBody(List<ModelRenderer> renderers) {
			for(ModelRenderer renderer : renderers) {
				this.diveBoxes(renderer,
							   ((IModelRenderer)renderer).gibbed$getDefaultRotationPointX(),
							   ((IModelRenderer)renderer).gibbed$getDefaultRotationPointY(),
							   ((IModelRenderer)renderer).gibbed$getDefaultRotationPointZ());
			}
		}
		
		protected float dX() {
			return this.maxX - this.minX;
		}
		
		protected float dY() {
			return this.maxY - this.minY;
		}
		
		protected float dZ() {
			return this.maxZ - this.minZ;
		}
		
		protected boolean valid() {
			return this.minX != Float.MAX_VALUE && this.minY != Float.MAX_VALUE && this.minZ != Float.MAX_VALUE &&
					this.maxX != -Float.MAX_VALUE && this.maxY != -Float.MAX_VALUE && this.maxZ != -Float.MAX_VALUE;
		}
		
		protected void diveBoxes(ModelRenderer renderer, float rotPointX, float rotPointY, float rotPointZ) {
			for(ModelBox box : renderer.cubeList) {
				if(box.posX1 + rotPointX < minX) minX = box.posX1 + rotPointX;
				if(box.posX2 + rotPointX > maxX) maxX = box.posX2 + rotPointX;
				if(box.posY1 + rotPointY < minY) minY = box.posY1 + rotPointY;
				if(box.posY2 + rotPointY > maxY) maxY = box.posY2 + rotPointY;
				if(box.posZ1 + rotPointZ < minZ) minZ = box.posZ1 + rotPointZ;
				if(box.posZ2 + rotPointZ > maxZ) maxZ = box.posZ2 + rotPointZ;
			}
			if(renderer.childModels != null) {
				for(ModelRenderer childRenderer : renderer.childModels) {
					diveBoxes(childRenderer,
							  rotPointX + ((IModelRenderer)childRenderer).gibbed$getDefaultRotationPointX(),
							  rotPointY + ((IModelRenderer)childRenderer).gibbed$getDefaultRotationPointY(),
							  rotPointZ + ((IModelRenderer)childRenderer).gibbed$getDefaultRotationPointZ());
				}
			}
		}
	}
}