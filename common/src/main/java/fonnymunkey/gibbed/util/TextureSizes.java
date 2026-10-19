package fonnymunkey.gibbed.util;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Map;

public abstract class TextureSizes {
	private static final int[] DEFAULT_SIZE = { 64, 64 };
	private static final Map<ResourceLocation,int[]> SIZES = new HashMap<>();

	public static int[] getSize(ResourceLocation location) {
		if(!RenderSystem.isOnRenderThread()) return DEFAULT_SIZE;
		return SIZES.computeIfAbsent(location, loc -> {
			AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(loc);
			int previous = GlStateManager._getInteger(GL11.GL_TEXTURE_BINDING_2D);
			GlStateManager._bindTexture(texture.getId());
			int width = GlStateManager._getTexLevelParameter(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
			int height = GlStateManager._getTexLevelParameter(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
			GlStateManager._bindTexture(previous);
			if(width <= 0 || height <= 0) return DEFAULT_SIZE;
			return new int[]{ width, height };
		});
	}
}