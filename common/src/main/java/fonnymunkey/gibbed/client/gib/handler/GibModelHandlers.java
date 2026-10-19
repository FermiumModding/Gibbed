package fonnymunkey.gibbed.client.gib.handler;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Deque;

public abstract class GibModelHandlers {
	private static final Deque<IGibModelHandler> HANDLERS = new ArrayDeque<>();

	public static void register(IGibModelHandler handler) {
		HANDLERS.addFirst(handler);
	}

	@Nullable
	public static IGibModelHandler getHandler(@Nullable EntityRenderer<?> renderer) {
		if(renderer == null) return null;
		for(IGibModelHandler handler : HANDLERS) {
			if(handler.canHandle(renderer)) return handler;
		}
		return null;
	}

	public static void beginRenderStateCapture(LivingEntity entity, EntityRenderer<?> renderer) {
		IGibModelHandler handler = getHandler(renderer);
		if(handler != null) handler.beginRenderStateCapture(entity, renderer);
	}

	public static void endRenderStateCapture(LivingEntity entity, EntityRenderer<?> renderer) {
		IGibModelHandler handler = getHandler(renderer);
		if(handler != null) handler.endRenderStateCapture(entity, renderer);
	}

	public static void clearCaptures() {
		for(IGibModelHandler handler : HANDLERS) {
			handler.clearCaptures();
		}
	}
}