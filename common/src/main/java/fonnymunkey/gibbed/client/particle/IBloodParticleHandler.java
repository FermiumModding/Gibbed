package fonnymunkey.gibbed.client.particle;

import fonnymunkey.gibbed.client.gib.Gib;
import net.minecraft.world.entity.LivingEntity;

public interface IBloodParticleHandler {
	
	default boolean isActive() {
		return true;
	}
	
	default int getBloodParticleType(LivingEntity entity) {
		return -1;
	}
	
	void spawnGibbedParticle(Gib gib);
	
	void spawnGibLandParticle(Gib gib);
}