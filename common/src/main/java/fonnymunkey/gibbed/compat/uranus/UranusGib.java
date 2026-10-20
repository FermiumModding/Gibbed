package fonnymunkey.gibbed.compat.uranus;

import com.iafenvoy.uranus.client.model.AdvancedModelBox;
import com.iafenvoy.uranus.client.model.TabulaModelRenderUtils;
import com.iafenvoy.uranus.client.model.basic.BasicModelPart;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fonnymunkey.gibbed.client.gib.AbstractGib;
import fonnymunkey.gibbed.client.gib.Gib;
import fonnymunkey.gibbed.mixin.uranus.AdvancedModelBoxInvoker;
import fonnymunkey.gibbed.util.UVScalingVertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

public class UranusGib extends AbstractGib {
	private final AdvancedModelBox box;

	public UranusGib(AdvancedModelBox box, boolean renderChildren, Matrix4f parentTransform) {
		super(renderChildren);
		this.box = box;

		diveCubes(box, 0, 0, 0);
		if(!this.valid()) return;

		this.cX = (this.maxX + this.minX) / 2.0F;
		this.cY = (this.maxY + this.minY) / 2.0F;
		this.cZ = (this.maxZ + this.minZ) / 2.0F;
		this.dX = this.maxX - this.minX;
		this.dY = this.maxY - this.minY;
		this.dZ = this.maxZ - this.minZ;

		parentTransform.transformPosition(this.cX + box.defaultPositionX, this.cY + box.defaultPositionY, this.cZ + box.defaultPositionZ, this.restCenter);
	}

	public static void applyRestTransform(Matrix4f matrix, AdvancedModelBox box) {
		matrix.translate(box.defaultPositionX, box.defaultPositionY, box.defaultPositionZ);
		if(box.defaultRotationX != 0.0F || box.defaultRotationY != 0.0F || box.defaultRotationZ != 0.0F) {
			matrix.rotate(new Quaternionf().rotationZYX(box.defaultRotationZ, box.defaultRotationY, box.defaultRotationX));
		}
	}

	@Override
	public Object getContextKey() {
		return this.box;
	}

	@Override
	public void render(Gib gib, PoseStack poseStack, MultiBufferSource buffer, int packedLight, float partialTicks) {
		float alpha = this.fadeOut() ? gib.getAlpha(partialTicks) : 1.0F;

		poseStack.pushPose();
		this.applyModelRenderTransform(gib, poseStack, partialTicks);

		List<BoxState> states = new ArrayList<>();
		states.add(new BoxState(this.box));
		this.box.setPos(0.0F, 0.0F, 0.0F);
		this.box.rotateAngleX = 0.0F;
		this.box.rotateAngleY = 0.0F;
		this.box.rotateAngleZ = 0.0F;
		this.box.setScale(1.0F, 1.0F, 1.0F);
		this.box.showModel = true;
		if(this.renderChildren) {
			resetChildren(this.box, states);
		}

		this.renderBox(poseStack, buffer, gib.baseTexture, gib, packedLight, alpha);
		if(gib.layerTexture != null) {
			this.renderBox(poseStack, buffer, gib.layerTexture, gib, packedLight, alpha);
		}

		for(BoxState state : states) {
			state.restore();
		}

		poseStack.popPose();
	}

	private void renderBox(PoseStack poseStack, MultiBufferSource buffer, ResourceLocation texture, Gib gib, int packedLight, float alpha) {
		VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(texture));
		if(gib.goreScaleU != 1.0F || gib.goreScaleV != 1.0F) {
			consumer = new UVScalingVertexConsumer(consumer, gib.goreScaleU, gib.goreScaleV);
		}
		if(this.renderChildren) {
			this.box.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, FastColor.ARGB32.colorFromFloat(alpha, 1.0F, 1.0F, 1.0F));
		}
		else {
			poseStack.pushPose();
			this.box.translateAndRotate(poseStack);
			((AdvancedModelBoxInvoker)this.box).gibbed$invokeDoRender(poseStack.last(), consumer, packedLight, OverlayTexture.NO_OVERLAY, FastColor.ARGB32.colorFromFloat(alpha, 1.0F, 1.0F, 1.0F));
			poseStack.popPose();
		}
	}

	private static void resetChildren(AdvancedModelBox box, List<BoxState> states) {
		for(BasicModelPart child : box.childModels) {
			if(child instanceof AdvancedModelBox childBox) {
				states.add(new BoxState(childBox));
				childBox.resetToDefaultPose();
				resetChildren(childBox, states);
			}
		}
	}

	private void diveCubes(AdvancedModelBox box, float rotPointX, float rotPointY, float rotPointZ) {
		for(TabulaModelRenderUtils.ModelBox cube : box.cubeList) {
			if(cube.posX1 + rotPointX < minX) minX = cube.posX1 + rotPointX;
			if(cube.posX2 + rotPointX > maxX) maxX = cube.posX2 + rotPointX;
			if(cube.posY1 + rotPointY < minY) minY = cube.posY1 + rotPointY;
			if(cube.posY2 + rotPointY > maxY) maxY = cube.posY2 + rotPointY;
			if(cube.posZ1 + rotPointZ < minZ) minZ = cube.posZ1 + rotPointZ;
			if(cube.posZ2 + rotPointZ > maxZ) maxZ = cube.posZ2 + rotPointZ;
		}
		if(this.renderChildren) {
			for(BasicModelPart child : box.childModels) {
				if(child instanceof AdvancedModelBox childBox) {
					diveCubes(childBox, rotPointX + childBox.defaultPositionX, rotPointY + childBox.defaultPositionY, rotPointZ + childBox.defaultPositionZ);
				}
			}
		}
	}

	private static class BoxState {
		private final AdvancedModelBox box;
		private final float rotationPointX;
		private final float rotationPointY;
		private final float rotationPointZ;
		private final float rotateAngleX;
		private final float rotateAngleY;
		private final float rotateAngleZ;
		private final float scaleX;
		private final float scaleY;
		private final float scaleZ;
		private final boolean showModel;

		private BoxState(AdvancedModelBox box) {
			this.box = box;
			this.rotationPointX = box.rotationPointX;
			this.rotationPointY = box.rotationPointY;
			this.rotationPointZ = box.rotationPointZ;
			this.rotateAngleX = box.rotateAngleX;
			this.rotateAngleY = box.rotateAngleY;
			this.rotateAngleZ = box.rotateAngleZ;
			this.scaleX = box.scaleX;
			this.scaleY = box.scaleY;
			this.scaleZ = box.scaleZ;
			this.showModel = box.showModel;
		}

		private void restore() {
			this.box.setPos(this.rotationPointX, this.rotationPointY, this.rotationPointZ);
			this.box.rotateAngleX = this.rotateAngleX;
			this.box.rotateAngleY = this.rotateAngleY;
			this.box.rotateAngleZ = this.rotateAngleZ;
			this.box.setScale(this.scaleX, this.scaleY, this.scaleZ);
			this.box.showModel = this.showModel;
		}
	}
}