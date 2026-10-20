package fonnymunkey.gibbed.client.gib;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public abstract class AbstractGib {
	protected float cX = 0.0F;
	protected float cY = 0.0F;
	protected float cZ = 0.0F;
	protected float dX = 0.0F;
	protected float dY = 0.0F;
	protected float dZ = 0.0F;
	protected float minX = Float.MAX_VALUE;
	protected float minY = Float.MAX_VALUE;
	protected float minZ = Float.MAX_VALUE;
	protected float maxX = -Float.MAX_VALUE;
	protected float maxY = -Float.MAX_VALUE;
	protected float maxZ = -Float.MAX_VALUE;
	
	protected static final float[] DEFAULT_SCALES = { 0.0625F, 1.0F, 1.0F, 1.0F };
	protected final boolean renderChildren;
	protected final Vector3f restCenter;
	
	public AbstractGib(boolean renderChildren) {
		this.renderChildren = renderChildren;
		this.restCenter = new Vector3f();
	}
	
	public float cX(float scale) {
		return this.cX * scale;
	}
	
	public float cY(float scale) {
		return this.cY * scale;
	}
	
	public float cZ(float scale) {
		return this.cZ * scale;
	}
	
	public float dX(float scale) {
		return this.dX * scale;
	}
	
	public float dY(float scale) {
		return this.dY * scale;
	}
	
	public float dZ(float scale) {
		return this.dZ * scale;
	}
	
	public float offsetX(float scale, float prepScaleX) {
		return this.restCenter.x * scale * prepScaleX;
	}
	
	public float offsetY(float scale, float prepScaleY) {
		return 1.501F * prepScaleY - this.restCenter.y * scale * prepScaleY;
	}
	
	public float offsetZ(float scale, float prepScaleZ) {
		return this.restCenter.z * scale * prepScaleZ;
	}
	
	public float bbWidth(float scale, float prepScaleX, float prepScaleY, float prepScaleZ) {
		return Math.max(this.dX * prepScaleX, Math.max(this.dY * prepScaleY, this.dZ * prepScaleZ)) * scale;
	}
	
	public float bbHeight(float scale, float prepScaleX, float prepScaleY, float prepScaleZ) {
		return Math.min(this.dX * prepScaleX, Math.min(this.dY * prepScaleY, this.dZ * prepScaleZ)) * scale;
	}
	
	public abstract Object getContextKey();
	
	public float[] getDefaultScales() {
		return DEFAULT_SCALES;
	}
	
	public boolean fadeOut() {
		return true;
	}
	
	public abstract void render(Gib gib, PoseStack poseStack, MultiBufferSource buffer, int packedLight, float partialTicks);
	
	public boolean valid() {
		return this.minX != Float.MAX_VALUE && this.minY != Float.MAX_VALUE && this.minZ != Float.MAX_VALUE &&
				this.maxX != -Float.MAX_VALUE && this.maxY != -Float.MAX_VALUE && this.maxZ != -Float.MAX_VALUE;
	}

	protected void applyModelRenderTransform(Gib gib, PoseStack poseStack, float partialTicks) {
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

		poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - gib.getYRot(partialTicks)));
		poseStack.scale(-1.0F, -1.0F, 1.0F);
		poseStack.scale(gib.prepScaleX, gib.prepScaleY, gib.prepScaleZ);

		poseStack.mulPose(rotation(rotAxisX, rotAxisY, rotAxisZ, gib.getXRot(partialTicks)));
		poseStack.translate(-this.cX(gib.scale), -this.cY(gib.scale), -this.cZ(gib.scale));
		if(gib.onGround) {
			poseStack.translate(rotAxisZ * (shortest / 2.0F + gib.zFightOffset), -rotAxisY * (shortest / 2.0F + gib.zFightOffset), -rotAxisX * (shortest / 2.0F + gib.zFightOffset));
		}
	}

	private static Quaternionf rotation(float axisX, float axisY, float axisZ, float degrees) {
		if(axisX != 0.0F) return Axis.XP.rotationDegrees(degrees);
		else if(axisY != 0.0F) return Axis.YP.rotationDegrees(degrees);
		else return Axis.ZP.rotationDegrees(degrees);
	}
}