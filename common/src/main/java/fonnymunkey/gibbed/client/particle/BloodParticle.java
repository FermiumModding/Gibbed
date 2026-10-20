package fonnymunkey.gibbed.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;

public class BloodParticle extends TextureSheetParticle {
	private static final ResourceLocation[] SPRITES = {
			ResourceLocation.withDefaultNamespace("splash_0"),
			ResourceLocation.withDefaultNamespace("splash_1"),
			ResourceLocation.withDefaultNamespace("splash_2"),
			ResourceLocation.withDefaultNamespace("splash_3")
	};
	
	public BloodParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
		super(level, x, y, z, xSpeed, ySpeed, zSpeed);
		this.rCol = this.random.nextFloat() * 0.2F + 0.8F;
		this.gCol = 0.0F;
		this.bCol = 0.0F;
		AbstractTexture atlas = Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_PARTICLES);
		if(atlas instanceof TextureAtlas textureAtlas) {
			this.setSprite(textureAtlas.getSprite(SPRITES[this.random.nextInt(SPRITES.length)]));
		}
		this.setSize(0.02F, 0.02F);
		this.gravity = 0.8F;
		this.lifetime = (int)(8.0D / (Math.random() * 0.4D + 0.1D));
	}
	
	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
	}
}