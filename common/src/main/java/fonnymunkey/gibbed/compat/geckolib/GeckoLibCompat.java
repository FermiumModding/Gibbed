package fonnymunkey.gibbed.compat.geckolib;

import com.mojang.blaze3d.vertex.PoseStack;
import fonnymunkey.gibbed.compat.BoneModelCapture;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;

public abstract class GeckoLibCompat {
	public static final BoneModelCapture<BakedGeoModel> CAPTURE = new BoneModelCapture<>();

	public static void captureModel(BakedGeoModel model) {
		CAPTURE.captureModel(model);
	}

	public static void captureBone(PoseStack poseStack, CoreGeoBone bone) {
		if(bone instanceof GeoBone) CAPTURE.captureBone(poseStack, bone, bone.isHidden());
	}
}