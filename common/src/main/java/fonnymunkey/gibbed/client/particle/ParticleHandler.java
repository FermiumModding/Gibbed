package fonnymunkey.gibbed.client.particle;

import fonnymunkey.gibbed.client.gib.Gib;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Deque;

public abstract class ParticleHandler {
	
	private static final Deque<IBloodParticleHandler> HANDLERS = new ArrayDeque<>();
	
	public static void register(IBloodParticleHandler handler) {
		HANDLERS.addFirst(handler);
	}
	
	@Nullable
	private static IBloodParticleHandler getHandler() {
		for(IBloodParticleHandler handler : HANDLERS) {
			if(handler.isActive()) return handler;
		}
		return null;
	}
	
	public static int getBloodParticleType(LivingEntity entity) {
		IBloodParticleHandler handler = getHandler();
		return handler == null ? -1 : handler.getBloodParticleType(entity);
	}
	
	public static void spawnGibbedParticle(Gib gib) {
		IBloodParticleHandler handler = getHandler();
		if(handler != null) handler.spawnGibbedParticle(gib);
	}
	
	public static void spawnGibLandParticle(Gib gib) {
		IBloodParticleHandler handler = getHandler();
		if(handler != null) handler.spawnGibLandParticle(gib);
	}
}