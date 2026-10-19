package fonnymunkey.gibbed.client.gib.handler;

import fonnymunkey.gibbed.client.gib.AbstractGib;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.List;

public interface IGibModelHandler {

	boolean canHandle(EntityRenderer<?> renderer);

	@Nullable
	Object getGibCacheKey(LivingEntity entity, EntityRenderer<?> renderer);

	List<AbstractGib> generateGibs(Object cacheKey, LivingEntity entity, EntityRenderer<?> renderer, float maxBranchSizeRatio, float maxBranchesRatio);

	@Nullable
	@SuppressWarnings("unchecked")
	default ResourceLocation getEntityTexture(LivingEntity entity, EntityRenderer<?> renderer) {
		return ((EntityRenderer<Entity>)renderer).getTextureLocation(entity);
	}

	default void beginRenderStateCapture(LivingEntity entity, EntityRenderer<?> renderer) { }

	default void endRenderStateCapture(LivingEntity entity, EntityRenderer<?> renderer) { }

	default void clearCaptures() { }
}