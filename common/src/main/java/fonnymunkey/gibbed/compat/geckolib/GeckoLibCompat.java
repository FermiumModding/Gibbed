package fonnymunkey.gibbed.compat.geckolib;

import com.mojang.blaze3d.vertex.PoseStack;
import fonnymunkey.gibbed.client.RenderCaptureHandler;
import fonnymunkey.gibbed.client.gib.handler.GibModelHandlers;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;

import javax.annotation.Nullable;

public abstract class GeckoLibCompat {
	private static final Int2ObjectOpenHashMap<BakedGeoModel> capturedModels = new Int2ObjectOpenHashMap<>();
	private static final Vector3f capturedScale = new Vector3f();
	private static Integer captureEntity = null;
	private static boolean scaleCaptured = false;

	public static void beginRenderStateCapture(LivingEntity entity) {
		captureEntity = entity.getId();
		scaleCaptured = false;
		RenderCaptureHandler.beginAutoLayerTextureCapture();
	}

	public static void endRenderStateCapture() {
		captureEntity = null;
		scaleCaptured = false;
		RenderCaptureHandler.endAutoLayerTextureCapture();
	}

	public static void captureModel(BakedGeoModel model) {
		if(captureEntity != null && model != null && RenderCaptureHandler.isCapturing() && !capturedModels.containsKey((int)captureEntity)) {
			capturedModels.put((int)captureEntity, model);
		}
	}

	public static void captureBone(PoseStack poseStack, CoreGeoBone bone) {
		if(captureEntity == null || !(bone instanceof GeoBone) || bone.isHidden() || !RenderCaptureHandler.isCapturing()) return;
		if(!scaleCaptured) {
			scaleCaptured = true;
			poseStack.last().pose().getScale(capturedScale);
			RenderCaptureHandler.captureScale(capturedScale.x, capturedScale.y, capturedScale.z);
		}
		RenderCaptureHandler.captureRenderer(bone, 1.0F);
	}

	@Nullable
	public static BakedGeoModel getCapturedModel(LivingEntity entity) {
		return capturedModels.get(entity.getId());
	}

	public static void clearCaptures() {
		capturedModels.clear();
		captureEntity = null;
	}
}