package fonnymunkey.gibbed.client.gib;

import fonnymunkey.gibbed.Gibbed;
import fonnymunkey.gibbed.config.ConfigHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.gen.NoiseGeneratorSimplex;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import javax.annotation.Nullable;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public abstract class GoreStainGenerator {
	private static final int GORE_TILE = 32;
	private static final int MAX_TEXTURE_SIZE = 2048;
	private static final int NOISE_BINS = 1024;

	private static final Map<ResourceLocation,AlphaMask> ALPHA_CACHE = new HashMap<>();
	private static final Set<ResourceLocation> STAIN_TEXTURES = new HashSet<>();
	@Nullable
	private static int[] gorePixels = null;
	private static boolean loggedFailure = false;

	public static void resetCache() {
		ALPHA_CACHE.clear();
		gorePixels = null;
	}

	@Nullable
	public static ResourceLocation create(EntityLivingBase entity, ResourceLocation baseTexture) {
		if(!Minecraft.getMinecraft().isCallingFromMinecraftThread()) return null;
		float coverage = Math.min(100, ConfigHandler.TEXTURES.goreStainCoverage) / 100.0F;
		if(coverage <= 0.0F) return null;
		try {
			int[] gore = getGorePixels();
			AlphaMask base = getAlphaMask(baseTexture);
			if(gore == null || base == null) return null;
			DynamicTexture texture = paint(base, gore, coverage, entity.getRNG().nextLong());
			if(texture == null) return null;
			ResourceLocation id = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation("gibbed_gore_stain", texture);
			STAIN_TEXTURES.add(id);
			return id;
		}
		catch(RuntimeException e) {
			if(!loggedFailure) {
				loggedFailure = true;
				Gibbed.LOGGER.warn("Failed to create gore stain for {}, further failures will not be logged", baseTexture, e);
			}
			return null;
		}
	}

	public static void releaseUnused(@Nullable World world) {
		if(STAIN_TEXTURES.isEmpty()) return;
		Set<ResourceLocation> used = new HashSet<>();
		if(world != null) {
			for(Entity entity : world.loadedEntityList) {
				if(entity instanceof EntityGib && !entity.isDead && ((EntityGib)entity).stainTexture != null) {
					used.add(((EntityGib)entity).stainTexture);
				}
			}
		}
		TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
		Iterator<ResourceLocation> iter = STAIN_TEXTURES.iterator();
		while(iter.hasNext()) {
			ResourceLocation id = iter.next();
			if(used.contains(id)) continue;
			textureManager.deleteTexture(id);
			iter.remove();
		}
	}

	@Nullable
	private static DynamicTexture paint(AlphaMask base, int[] gore, float coverage, long seed) {
		int width = base.width;
		int height = base.height;
		float unit = Math.max(1.0F, Math.max(width, height) / 64.0F);
		double largeCell = 14.0D * unit;
		double smallCell = 6.0D * unit;
		Random random = new Random(seed);
		NoiseGeneratorSimplex large = new NoiseGeneratorSimplex(random);
		NoiseGeneratorSimplex small = new NoiseGeneratorSimplex(random);

		float[] noise = new float[width * height];
		int[] histogram = new int[NOISE_BINS];
		int opaque = 0;
		for(int y = 0; y < height; y++) {
			for(int x = 0; x < width; x++) {
				int i = x + y * width;
				if(base.alpha[i] == 0) {
					noise[i] = -1.0F;
					continue;
				}
				double value = 0.75D * large.getValue(x / largeCell, y / largeCell) + 0.25D * small.getValue(x / smallCell, y / smallCell);
				float normalized = (float)Math.max(0.0D, Math.min(1.0D, (value + 1.0D) * 0.5D));
				noise[i] = normalized;
				histogram[Math.min(NOISE_BINS - 1, (int)(normalized * NOISE_BINS))]++;
				opaque++;
			}
		}
		if(opaque == 0) return null;

		int target = Math.max(1, Math.round(opaque * coverage));
		int counted = 0;
		float threshold = 0.0F;
		for(int bin = NOISE_BINS - 1; bin >= 0; bin--) {
			counted += histogram[bin];
			if(counted >= target) {
				threshold = bin / (float)NOISE_BINS;
				break;
			}
		}

		int scale = Math.max(width, height) <= 128 ? 2 : 1;
		int goreStep = 2 / scale;
		int scaledWidth = width * scale;
		DynamicTexture texture = new DynamicTexture(scaledWidth, height * scale);
		int[] data = texture.getTextureData();
		for(int y = 0; y < height; y++) {
			for(int x = 0; x < width; x++) {
				int i = x + y * width;
				if(noise[i] < threshold) continue;
				boolean edge = !isCovered(noise, threshold, width, height, x - 1, y) || !isCovered(noise, threshold, width, height, x + 1, y)
						|| !isCovered(noise, threshold, width, height, x, y - 1) || !isCovered(noise, threshold, width, height, x, y + 1);
				int alpha = base.alpha[i] & 0xFF;
				for(int sy = 0; sy < scale; sy++) {
					for(int sx = 0; sx < scale; sx++) {
						int px = x * scale + sx;
						int py = y * scale + sy;
						int goreColor = gore[((px * goreStep) % GORE_TILE) + ((py * goreStep) % GORE_TILE) * GORE_TILE];
						if(edge) goreColor = darken(goreColor, 0.7F);
						data[px + py * scaledWidth] = (alpha << 24) | (goreColor & 0x00FFFFFF);
					}
				}
			}
		}
		texture.updateDynamicTexture();
		return texture;
	}

	private static boolean isCovered(float[] noise, float threshold, int width, int height, int x, int y) {
		if(x < 0 || y < 0 || x >= width || y >= height) return true;
		return noise[x + y * width] >= threshold;
	}

	private static int darken(int argb, float factor) {
		int r = (int)(((argb >> 16) & 0xFF) * factor);
		int g = (int)(((argb >> 8) & 0xFF) * factor);
		int b = (int)((argb & 0xFF) * factor);
		return (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
	}

	@Nullable
	private static int[] getGorePixels() {
		if(gorePixels != null) return gorePixels;
		Image image = readImage(GibGenerator.GORE_TEXTURE);
		if(image == null) return null;
		int[] pixels = new int[GORE_TILE * GORE_TILE];
		for(int y = 0; y < GORE_TILE; y++) {
			for(int x = 0; x < GORE_TILE; x++) {
				pixels[x + y * GORE_TILE] = image.pixels[(x * image.width / GORE_TILE) + (y * image.height / GORE_TILE) * image.width];
			}
		}
		gorePixels = pixels;
		return pixels;
	}

	@Nullable
	private static AlphaMask getAlphaMask(ResourceLocation location) {
		AlphaMask cached = ALPHA_CACHE.get(location);
		if(cached != null) return cached;
		Image image = readImage(location);
		if(image == null) return null;
		byte[] alpha = new byte[image.width * image.height];
		for(int i = 0; i < alpha.length; i++) {
			alpha[i] = (byte)(image.pixels[i] >>> 24);
		}
		AlphaMask mask = new AlphaMask(image.width, image.height, alpha);
		ALPHA_CACHE.put(location, mask);
		return mask;
	}

	@Nullable
	private static Image readImage(ResourceLocation location) {
		TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
		ITextureObject texture = textureManager.getTexture(location);
		if(texture == null) {
			texture = new SimpleTexture(location);
			if(!textureManager.loadTexture(location, texture)) return null;
		}
		int previous = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
		try {
			GlStateManager.bindTexture(texture.getGlTextureId());
			int width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
			int height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
			if(width <= 0 || height <= 0 || width > MAX_TEXTURE_SIZE || height > MAX_TEXTURE_SIZE) return null;
			IntBuffer buffer = BufferUtils.createIntBuffer(width * height);
			GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, buffer);
			int[] pixels = new int[width * height];
			buffer.get(pixels);
			return new Image(width, height, pixels);
		}
		finally {
			GlStateManager.bindTexture(previous);
		}
	}

	private static class Image {
		private final int width;
		private final int height;
		private final int[] pixels;

		private Image(int width, int height, int[] pixels) {
			this.width = width;
			this.height = height;
			this.pixels = pixels;
		}
	}

	private static class AlphaMask {
		private final int width;
		private final int height;
		private final byte[] alpha;

		private AlphaMask(int width, int height, byte[] alpha) {
			this.width = width;
			this.height = height;
			this.alpha = alpha;
		}
	}
}
