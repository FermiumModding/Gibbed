package fonnymunkey.gibbed.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import fonnymunkey.gibbed.client.RenderCaptureHandler;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;

import javax.annotation.Nullable;

public class BoneModelCapture<M> {
	private final Int2ObjectOpenHashMap<M> capturedModels = new Int2ObjectOpenHashMap<>();
	private final Vector3f capturedScale = new Vector3f();
	private Integer captureEntity = null;
	private boolean scaleCaptured = false;

	public void beginRenderStateCapture(LivingEntity entity) {
		this.captureEntity = entity.getId();
		this.scaleCaptured = false;
		RenderCaptureHandler.beginAutoLayerTextureCapture();
	}

	public void endRenderStateCapture() {
		this.captureEntity = null;
		this.scaleCaptured = false;
		RenderCaptureHandler.endAutoLayerTextureCapture();
	}

	public void captureModel(@Nullable M model) {
		if(this.captureEntity != null && model != null && RenderCaptureHandler.isCapturing() && !this.capturedModels.containsKey((int)this.captureEntity)) {
			this.capturedModels.put((int)this.captureEntity, model);
		}
	}

	public void captureBone(PoseStack poseStack, Object bone, boolean hidden) {
		if(this.captureEntity == null || hidden || !RenderCaptureHandler.isCapturing()) return;
		if(!this.scaleCaptured) {
			this.scaleCaptured = true;
			poseStack.last().pose().getScale(this.capturedScale);
			RenderCaptureHandler.captureScale(this.capturedScale.x, this.capturedScale.y, this.capturedScale.z);
		}
		RenderCaptureHandler.captureRenderer(bone, 1.0F);
	}

	@Nullable
	public M getCapturedModel(LivingEntity entity) {
		return this.capturedModels.get(entity.getId());
	}

	public void clearCaptures() {
		this.capturedModels.clear();
		this.captureEntity = null;
	}
}