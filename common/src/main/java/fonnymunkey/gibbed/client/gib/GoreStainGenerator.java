package fonnymunkey.gibbed.client.gib;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import fonnymunkey.gibbed.Constants;
import fonnymunkey.gibbed.config.ConfigHandler;
import fonnymunkey.gibbed.util.TextureSizes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
	public static ResourceLocation create(LivingEntity entity, ResourceLocation baseTexture) {
		if(!RenderSystem.isOnRenderThread()) return null;
		float coverage = Math.min(100, ConfigHandler.CONFIG.TEXTURES.goreStainCoverage) / 100.0F;
		if(coverage <= 0.0F) return null;
		try {
			int[] gore = getGorePixels();
			AlphaMask base = getAlphaMask(baseTexture);
			if(gore == null || base == null) return null;
			DynamicTexture texture = paint(base, gore, coverage, entity.getRandom().nextLong());
			if(texture == null) return null;
			ResourceLocation id = Minecraft.getInstance().getTextureManager().register("gibbed_gore_stain", texture);
			STAIN_TEXTURES.add(id);
			return id;
		}
		catch(RuntimeException e) {
			if(!loggedFailure) {
				loggedFailure = true;
				Constants.LOG.warn("Failed to create gore stain for {}, further failures will not be logged", baseTexture, e);
			}
			return null;
		}
	}

	public static void releaseUnused(List<Gib> gibs) {
		if(STAIN_TEXTURES.isEmpty()) return;
		Set<ResourceLocation> used = new HashSet<>();
		for(Gib gib : gibs) {
			if(gib.stainTexture != null) used.add(gib.stainTexture);
		}
		TextureManager textureManager = Minecraft.getInstance().getTextureManager();
		Iterator<ResourceLocation> iter = STAIN_TEXTURES.iterator();
		while(iter.hasNext()) {
			ResourceLocation id = iter.next();
			if(used.contains(id)) continue;
			textureManager.release(id);
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
		RandomSource random = RandomSource.create(seed);
		SimplexNoise large = new SimplexNoise(random);
		SimplexNoise small = new SimplexNoise(random);
		
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
		NativeImage image = new NativeImage(width * scale, height * scale, true);
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
						image.setPixelRGBA(px, py, (alpha << 24) | (goreColor & 0x00FFFFFF));
					}
				}
			}
		}
		return new DynamicTexture(image);
	}

	private static boolean isCovered(float[] noise, float threshold, int width, int height, int x, int y) {
		if(x < 0 || y < 0 || x >= width || y >= height) return true;
		return noise[x + y * width] >= threshold;
	}

	private static int darken(int abgr, float factor) {
		int r = (int)((abgr & 0xFF) * factor);
		int g = (int)(((abgr >> 8) & 0xFF) * factor);
		int b = (int)(((abgr >> 16) & 0xFF) * factor);
		return (abgr & 0xFF000000) | (b << 16) | (g << 8) | r;
	}

	@Nullable
	private static int[] getGorePixels() {
		if(gorePixels != null) return gorePixels;
		NativeImage image = readImage(GibGenerator.GORE_TEXTURE);
		if(image == null) return null;
		try {
			int[] pixels = new int[GORE_TILE * GORE_TILE];
			for(int y = 0; y < GORE_TILE; y++) {
				for(int x = 0; x < GORE_TILE; x++) {
					pixels[x + y * GORE_TILE] = image.getPixelRGBA(x * image.getWidth() / GORE_TILE, y * image.getHeight() / GORE_TILE);
				}
			}
			gorePixels = pixels;
			return pixels;
		}
		finally {
			image.close();
		}
	}

	@Nullable
	private static AlphaMask getAlphaMask(ResourceLocation location) {
		AlphaMask cached = ALPHA_CACHE.get(location);
		if(cached != null) return cached;
		NativeImage image = readImage(location);
		if(image == null) return null;
		try {
			int width = image.getWidth();
			int height = image.getHeight();
			byte[] alpha = new byte[width * height];
			for(int y = 0; y < height; y++) {
				for(int x = 0; x < width; x++) {
					alpha[x + y * width] = (byte)(image.getPixelRGBA(x, y) >>> 24);
				}
			}
			AlphaMask mask = new AlphaMask(width, height, alpha);
			ALPHA_CACHE.put(location, mask);
			return mask;
		}
		finally {
			image.close();
		}
	}

	@Nullable
	private static NativeImage readImage(ResourceLocation location) {
		int[] size = TextureSizes.getSize(location);
		if(size[0] > MAX_TEXTURE_SIZE || size[1] > MAX_TEXTURE_SIZE) return null;
		int previous = GlStateManager._getInteger(GL11.GL_TEXTURE_BINDING_2D);
		try {
			GlStateManager._bindTexture(Minecraft.getInstance().getTextureManager().getTexture(location).getId());
			NativeImage image = new NativeImage(size[0], size[1], false);
			image.downloadTexture(0, false);
			return image;
		}
		finally {
			GlStateManager._bindTexture(previous);
		}
	}

	private record AlphaMask(int width, int height, byte[] alpha) { }
}