package fonnymunkey.gibbed.client.particle;

import fonnymunkey.gibbed.client.gib.Gib;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

public class VanillaBloodParticleHandler implements IBloodParticleHandler {
	
	@Override
	public void spawnGibbedParticle(Gib gib) {
		ClientLevel level = Minecraft.getInstance().level;
		if(level == null) return;
		int particles = Math.max(1, (int)gib.particleScale);
		for(int i = 0; i < particles; i++) {
			Minecraft.getInstance().particleEngine.add(
					new BloodParticle(level,
									  gib.x, gib.y, gib.z,
									  gib.motionX, gib.motionY, gib.motionZ));
		}
	}
	
	@Override
	public void spawnGibLandParticle(Gib gib) {
		ClientLevel level = Minecraft.getInstance().level;
		if(level == null) return;
		int particles = Math.max(1, (int)gib.particleScale);
		for(int i = 0; i < particles; i++) {
			Minecraft.getInstance().particleEngine.add(
					new BloodParticle(level,
									  gib.x, gib.y, gib.z,
									  0.0D, 1.0D, 0.0D));
		}
	}
}