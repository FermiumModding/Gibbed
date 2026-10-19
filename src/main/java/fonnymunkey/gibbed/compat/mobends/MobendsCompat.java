package fonnymunkey.gibbed.compat.mobends;

import fonnymunkey.gibbed.client.gib.AbstractGib;
import fonnymunkey.gibbed.client.gib.GibGenerator;
import fonnymunkey.gibbed.config.ConfigHandler;
import fonnymunkey.gibbed.util.Triple;
import goblinbob.mobends.core.client.model.ModelPart;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class MobendsCompat {
	
	public static boolean isRendererWrapped(ModelRenderer renderer) {
		return renderer instanceof ModelPart;
	}
	
	//TODO mapping of mobends part context to vanilla part context or special rendering handling for mobend parts
	public static Map<AbstractGib,Triple<float[],ResourceLocation,ResourceLocation>> getGibsFromEntity(EntityLivingBase entity, Map<Object, Triple<float[],ResourceLocation,ResourceLocation>> capturedRenderers, List<AbstractGib> cachedGibs, float[] firstScale) {
		if(capturedRenderers == null || capturedRenderers.isEmpty() || cachedGibs == null || cachedGibs.isEmpty() || firstScale == null) return Collections.emptyMap();
		
		Map<AbstractGib,Triple<float[],ResourceLocation,ResourceLocation>> activeGibs = new HashMap<>();
		for(AbstractGib gib : cachedGibs) {
			Triple<float[],ResourceLocation,ResourceLocation> context = capturedRenderers.get(gib.getContextKey());
			if(context == null) context = new Triple<>(firstScale, null, null);
			ResourceLocation baseTexture = context.middle;
			ResourceLocation layerTexture = context.right;
			if(baseTexture == null) {
				if(layerTexture != null) {
					baseTexture = layerTexture;
					layerTexture = null;
				}
				else baseTexture = GibGenerator.getEntityTexture(entity);
			}
			if(ConfigHandler.shouldGoreTextureBeUsed(entity)) {
				baseTexture = GibGenerator.GORE_TEXTURE;
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