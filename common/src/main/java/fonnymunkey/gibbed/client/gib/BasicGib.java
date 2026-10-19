package fonnymunkey.gibbed.client.gib;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import fonnymunkey.gibbed.mixin.vanilla.ModelPartAccessor;
import fonnymunkey.gibbed.util.UVScalingVertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class BasicGib extends AbstractGib {
	//scale prepScaleX prepScaleY prepScaleZ
	private static final float[] DEFAULT_SCALES = { 0.0625F, 1.0F, 1.0F, 1.0F };
	private final ModelPart part;
	private final boolean renderChildren;
	private final Vector3f restCenter = new Vector3f();
	
	public BasicGib(ModelPart part, boolean renderChildren, Matrix4f parentTransform) {
		super();
		this.part = part;
		this.renderChildren = renderChildren;

		diveCubes(part, 0, 0, 0);
		if(!this.valid()) return;

		this.cX = (this.maxX + this.minX) / 2.0F;
		this.cY = (this.maxY + this.minY) / 2.0F;
		this.cZ = (this.maxZ + this.minZ) / 2.0F;
		this.dX = this.maxX - this.minX;
		this.dY = this.maxY - this.minY;
		this.dZ = this.maxZ - this.minZ;

		PartPose pose = part.getInitialPose();
		parentTransform.transformPosition(this.cX + pose.x, this.cY + pose.y, this.cZ + pose.z, this.restCenter);
	}
	
	@Override
	public float cX(float scale) {
		return this.cX * scale;
	}
	
	@Override
	public float cY(float scale) {
		return this.cY * scale;
	}
	
	@Override
	public float cZ(float scale) {
		return this.cZ * scale;
	}
	
	@Override
	public float dX(float scale) {
		return this.dX * scale;
	}
	
	@Override
	public float dY(float scale) {
		return this.dY * scale;
	}
	
	@Override
	public float dZ(float scale) {
		return this.dZ * scale;
	}
	
	@Override
	public float offsetX(float scale, float prepScaleX) {
		return this.restCenter.x * scale * prepScaleX;
	}
	
	@Override
	public float offsetY(float scale, float prepScaleY) {
		return 1.501F * prepScaleY - this.restCenter.y * scale * prepScaleY;
	}
	
	@Override
	public float offsetZ(float scale, float prepScaleZ) {
		return this.restCenter.z * scale * prepScaleZ;
	}

	public static void applyRestTransform(Matrix4f matrix, ModelPart part) {
		PartPose pose = part.getInitialPose();
		matrix.translate(pose.x, pose.y, pose.z);
		if(pose.xRot != 0.0F || pose.yRot != 0.0F || pose.zRot != 0.0F) {
			matrix.rotate(new Quaternionf().rotationZYX(pose.zRot, pose.yRot, pose.xRot));
		}
	}
	
	@Override
	public float bbWidth(float scale, float prepScaleX, float prepScaleY, float prepScaleZ) {
		return Math.max(this.dX * prepScaleX, Math.max(this.dY * prepScaleY, this.dZ * prepScaleZ)) * scale;
	}
	
	@Override
	public float bbHeight(float scale, float prepScaleX, float prepScaleY, float prepScaleZ) {
		return Math.min(this.dX * prepScaleX, Math.min(this.dY * prepScaleY, this.dZ * prepScaleZ)) * scale;
	}
	
	@Override
	public Object getContextKey() {
		return this.part;
	}
	
	@Override
	public float[] getDefaultScales() {
		return DEFAULT_SCALES;
	}
	
	@Override
	public boolean fadeOut() {
		return true;
	}
	
	@Override
	public void render(Gib gib, PoseStack poseStack, MultiBufferSource buffer, int packedLight, float partialTicks) {
		float dX = this.dX(gib.scale);
		float dY = this.dY(gib.scale);
		float dZ = this.dZ(gib.scale);
		float shortest;
		float rotAxisX = 0.0F;
		float rotAxisY = 0.0F;
		float rotAxisZ = 0.0F;
		if(dY < dX && dY < dZ) {
			shortest = dY;
			rotAxisY = 1.0F;
		}
		else if(dZ <= dX && dZ <= dY) {
			shortest = dZ;
			rotAxisX = 1.0F;
		}
		else {
			shortest = dX;
			rotAxisZ = 1.0F;
		}
		if(!gib.onGround) {
			rotAxisX = 1.0F;
			rotAxisY = 0.0F;
			rotAxisZ = 0.0F;
		}

		float alpha = this.fadeOut() ? gib.getAlpha(partialTicks) : 1.0F;

		poseStack.pushPose();
		poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - gib.getYRot(partialTicks)));
		poseStack.scale(-1.0F, -1.0F, 1.0F);
		poseStack.scale(gib.prepScaleX, gib.prepScaleY, gib.prepScaleZ);

		poseStack.mulPose(rotation(rotAxisX, rotAxisY, rotAxisZ, gib.getXRot(partialTicks)));
		poseStack.translate(-this.cX(gib.scale), -this.cY(gib.scale), -this.cZ(gib.scale));
		if(gib.onGround) {
			poseStack.translate(rotAxisZ * (shortest / 2.0F + gib.zFightOffset), -rotAxisY * (shortest / 2.0F + gib.zFightOffset), -rotAxisX * (shortest / 2.0F + gib.zFightOffset));
		}

		List<PartState> states = new ArrayList<>();
		states.add(new PartState(this.part));
		this.part.loadPose(PartPose.ZERO);
		this.part.visible = true;
		if(this.renderChildren) {
			this.part.getAllParts().skip(1).forEach(child -> {
				states.add(new PartState(child));
				child.resetPose();
			});
		}

		this.renderPart(poseStack, buffer, gib.baseTexture, gib, packedLight, alpha);
		if(gib.layerTexture != null) {
			this.renderPart(poseStack, buffer, gib.layerTexture, gib, packedLight, alpha);
		}

		for(PartState state : states) {
			state.restore();
		}

		poseStack.popPose();
	}

	private void renderPart(PoseStack poseStack, MultiBufferSource buffer, ResourceLocation texture, Gib gib, int packedLight, float alpha) {
		VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(texture));
		if(gib.goreScaleU != 1.0F || gib.goreScaleV != 1.0F) {
			consumer = new UVScalingVertexConsumer(consumer, gib.goreScaleU, gib.goreScaleV);
		}
		if(this.renderChildren) {
			this.part.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, alpha);
		}
		else {
			poseStack.pushPose();
			this.part.translateAndRotate(poseStack);
			PoseStack.Pose pose = poseStack.last();
			for(ModelPart.Cube cube : ((ModelPartAccessor)(Object)this.part).gibbed$getCubes()) {
				cube.compile(pose, consumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, alpha);
			}
			poseStack.popPose();
		}
	}
	
	private static Quaternionf rotation(float axisX, float axisY, float axisZ, float degrees) {
		if(axisX != 0.0F) return Axis.XP.rotationDegrees(degrees);
		else if(axisY != 0.0F) return Axis.YP.rotationDegrees(degrees);
		else return Axis.ZP.rotationDegrees(degrees);
	}
	
	private void diveCubes(ModelPart part, float rotPointX, float rotPointY, float rotPointZ) {
		for(ModelPart.Cube cube : ((ModelPartAccessor)(Object)part).gibbed$getCubes()) {
			if(cube.minX + rotPointX < minX) minX = cube.minX + rotPointX;
			if(cube.maxX + rotPointX > maxX) maxX = cube.maxX + rotPointX;
			if(cube.minY + rotPointY < minY) minY = cube.minY + rotPointY;
			if(cube.maxY + rotPointY > maxY) maxY = cube.maxY + rotPointY;
			if(cube.minZ + rotPointZ < minZ) minZ = cube.minZ + rotPointZ;
			if(cube.maxZ + rotPointZ > maxZ) maxZ = cube.maxZ + rotPointZ;
		}
		if(this.renderChildren) {
			for(ModelPart childPart : ((ModelPartAccessor)(Object)part).gibbed$getChildren().values()) {
				PartPose pose = childPart.getInitialPose();
				diveCubes(childPart, rotPointX + pose.x, rotPointY + pose.y, rotPointZ + pose.z);
			}
		}
	}

	private static class PartState {
		private final ModelPart part;
		private final PartPose pose;
		private final float xScale;
		private final float yScale;
		private final float zScale;
		private final boolean visible;

		private PartState(ModelPart part) {
			this.part = part;
			this.pose = part.storePose();
			this.xScale = part.xScale;
			this.yScale = part.yScale;
			this.zScale = part.zScale;
			this.visible = part.visible;
		}

		private void restore() {
			this.part.loadPose(this.pose);
			this.part.xScale = this.xScale;
			this.part.yScale = this.yScale;
			this.part.zScale = this.zScale;
			this.part.visible = this.visible;
		}
	}
}