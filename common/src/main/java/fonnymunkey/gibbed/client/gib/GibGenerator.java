package fonnymunkey.gibbed.client.gib;

import fonnymunkey.gibbed.Constants;
import fonnymunkey.gibbed.client.RenderCaptureHandler;
import fonnymunkey.gibbed.client.gib.handler.GibModelHandlers;
import fonnymunkey.gibbed.client.gib.handler.IGibModelHandler;
import fonnymunkey.gibbed.config.ConfigHandler;
import fonnymunkey.gibbed.util.Pair;
import fonnymunkey.gibbed.util.TextureSizes;
import fonnymunkey.gibbed.util.Triple;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.*;

public class GibGenerator {
	public static final ResourceLocation GORE_TEXTURE = new ResourceLocation(Constants.MOD_ID, "textures/gore.png");
	private static final Map<Object,List<AbstractGib>> cachedGibs = new WeakHashMap<>();
	
	public static void resetCache() {
		cachedGibs.clear();
	}
	
	public static Map<AbstractGib,GibContext> getGibsFromEntity(LivingEntity entity) {
		Map<Object,Triple<float[],ResourceLocation,ResourceLocation>> capturedRenderers = RenderCaptureHandler.getCapturedRenderers(entity);
		if(capturedRenderers == null && ConfigHandler.CONFIG.MODELS.onlyGibContextCapturedEntities) return Collections.emptyMap();
		
		boolean useGore = ConfigHandler.shouldGoreTextureBeUsed(entity);
		Map<AbstractGib,GibContext> activeGibs = new HashMap<>();
		if(capturedRenderers == null) {
			ResourceLocation entityTexture = getEntityTexture(entity);
			if(entityTexture == null) return activeGibs;
			for(AbstractGib gib : getCachedGibsFromEntity(entity)) {
				float[] scales = gib.getDefaultScales();
				if(scales == null) continue;
				activeGibs.put(gib, createContext(scales, entityTexture, null, useGore));
			}
		}
		else {
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
				if(baseTexture == null) continue;
				if(layerTexture != null && !ConfigHandler.shouldLayerTextureBeUsed(entity)) layerTexture = null;
				float[] scales = context.left;
				if(scales == null) scales = gib.getDefaultScales();
				if(scales != null && scales[0] != 0.0F) {
					activeGibs.put(gib, createContext(scales, baseTexture, layerTexture, useGore));
				}
			}
		}
		return activeGibs;
	}

	private static GibContext createContext(float[] scales, ResourceLocation baseTexture, @Nullable ResourceLocation layerTexture, boolean useGore) {
		if(useGore) {
			int[] size = TextureSizes.getSize(baseTexture);
			return new GibContext(scales, GORE_TEXTURE, null, size[0] / 16.0F, size[1] / 16.0F);
		}
		return new GibContext(scales, baseTexture, layerTexture, 1.0F, 1.0F);
	}
	
	@Nullable
	public static ResourceLocation getEntityTexture(LivingEntity entity) {
		EntityRenderer<?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
		IGibModelHandler handler = GibModelHandlers.getHandler(renderer);
		if(handler == null) return null;
		return handler.getEntityTexture(entity, renderer);
	}

	private static List<AbstractGib> getCachedGibsFromEntity(LivingEntity entity) {
		EntityRenderer<?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
		IGibModelHandler handler = GibModelHandlers.getHandler(renderer);
		if(handler == null) return Collections.emptyList();
		Object cacheKey = handler.getGibCacheKey(entity, renderer);
		if(cacheKey == null) return Collections.emptyList();
		if(!cachedGibs.containsKey(cacheKey)) {
			Pair<Float,Float> ratios = getBranchRatios(entity);
			cachedGibs.put(cacheKey, handler.generateGibs(cacheKey, entity, renderer, ratios.left, ratios.right));
		}
		return cachedGibs.get(cacheKey);
	}

	private static Pair<Float,Float> getBranchRatios(LivingEntity entity) {
		Pair<Float,Float> ratioOverride = ConfigHandler.getEntityBranchRatioOverride(entity);
		if(ratioOverride != null) return ratioOverride;
		return new Pair<>((float)ConfigHandler.CONFIG.MODELS.maxBranchSizeRatio, (float)ConfigHandler.CONFIG.MODELS.maxBranchesRatio);
	}
}