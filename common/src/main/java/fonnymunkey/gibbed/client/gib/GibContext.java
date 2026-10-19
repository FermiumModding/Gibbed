package fonnymunkey.gibbed.client.gib;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

public record GibContext(float[] scales, ResourceLocation baseTexture, @Nullable ResourceLocation layerTexture, float goreScaleU, float goreScaleV) { }