package fonnymunkey.gibbed.compat.azurelib;

import com.mojang.blaze3d.vertex.PoseStack;
import fonnymunkey.gibbed.compat.BoneModelCapture;
import mod.azure.azurelib.common.model.AzBakedModel;
import mod.azure.azurelib.common.model.AzBone;

public abstract class AzureLibCompat {
	public static final BoneModelCapture<AzBakedModel> CAPTURE = new BoneModelCapture<>();

	public static void captureModel(AzBakedModel model) {
		CAPTURE.captureModel(model);
	}

	public static void captureBone(PoseStack poseStack, AzBone bone) {
		if(bone != null) CAPTURE.captureBone(poseStack, bone, bone.isHidden());
	}
}