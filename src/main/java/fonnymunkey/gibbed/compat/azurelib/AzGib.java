package fonnymunkey.gibbed.compat.azurelib;

import fonnymunkey.gibbed.client.gib.AbstractGib;
import fonnymunkey.gibbed.client.gib.EntityGib;
import fonnymunkey.gibbed.client.gib.GibGenerator;
import fonnymunkey.gibbed.client.gib.RenderGib;
import fonnymunkey.gibbed.config.ConfigHandler;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import mod.azure.azurelib.cache.object.GeoCube;
import mod.azure.azurelib.cache.object.GeoQuad;
import mod.azure.azurelib.cache.object.GeoVertex;
import mod.azure.azurelib.model.AzBone;
import mod.azure.azurelib.model.AzBoneSnapshot;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

public class AzGib extends AbstractGib {
	//scale prepScaleX prepScaleY prepScaleZ
	private static final float[] DEFAULT_SCALES = { 1.0F, 1.0F, 1.0F, 1.0F };
	//x y z u v nx ny nz
	private static final int VERTEX_SIZE = 8;
	private final AzBone bone;
	private final boolean renderChildren;
	private final float textureWidth;
	private final float textureHeight;
	private final float[] vertexData;

	public AzGib(AzBone bone, boolean renderChildren, float textureWidth, float textureHeight) {
		super();
		this.bone = bone;
		this.renderChildren = renderChildren;
		this.textureWidth = textureWidth;
		this.textureHeight = textureHeight;

		Matrix4f matrix = new Matrix4f();
		matrix.setIdentity();
		applyParentRestTransforms(matrix, bone.getParent());
		FloatArrayList data = new FloatArrayList();
		this.diveBones(bone, matrix, data, true);
		this.vertexData = data.toFloatArray();
		if(!this.valid()) return;

		this.cX = (this.maxX + this.minX) / 2.0F;
		this.cY = (this.maxY + this.minY) / 2.0F;
		this.cZ = (this.maxZ + this.minZ) / 2.0F;
		this.dX = this.maxX - this.minX;
		this.dY = this.maxY - this.minY;
		this.dZ = this.maxZ - this.minZ;
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
		return -this.cX * scale * prepScaleX;
	}

	@Override
	public float offsetY(float scale, float prepScaleY) {
		return 0.01F + this.cY * scale * prepScaleY;
	}

	@Override
	public float offsetZ(float scale, float prepScaleZ) {
		return this.cZ * scale * prepScaleZ;
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
		return this.bone;
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
	public boolean flipYaw() {
		return false;
	}

	@Override
	public boolean flipY() {
		return false;
	}

	@Override
	public boolean valid() {
		return super.valid() && this.vertexData != null && this.vertexData.length > 0;
	}

	@Override
	public void render(RenderGib renderGib, EntityGib entityGib, double x, double y, double z, float partialTicks, float[] scales) {
		float dX = this.dX(entityGib.scale);
		float dY = this.dY(entityGib.scale);
		float dZ = this.dZ(entityGib.scale);
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
		if(!entityGib.onGround) {
			rotAxisX = 1.0F;
			rotAxisY = 0.0F;
			rotAxisZ = 0.0F;
		}

		float alpha = 1.0F;
		if(entityGib.groundTime > ConfigHandler.PHYSICS.gibGroundLifetime - 20 || entityGib.ticksExisted > ConfigHandler.PHYSICS.gibLifetime - 20) {
			alpha = MathHelper.clamp(1.0F - ((float)Math.max(entityGib.groundTime - (ConfigHandler.PHYSICS.gibGroundLifetime - 20), entityGib.ticksExisted - (ConfigHandler.PHYSICS.gibLifetime - 20)) + partialTicks) / 20.0F, 0.0F, 1.0F);
		}

		GlStateManager.pushMatrix();
		GlStateManager.disableCull();
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

		GlStateManager.translate(x, y, z);
		GlStateManager.rotate(180.0F - lerpRot(entityGib.prevRotationYaw, entityGib.rotationYaw, partialTicks), 0.0F, 1.0F, 0.0F);
		GlStateManager.enableRescaleNormal();
		GlStateManager.scale(entityGib.prepScaleX, entityGib.prepScaleY, entityGib.prepScaleZ);

		GlStateManager.rotate(lerpRot(entityGib.prevRotationPitch, entityGib.rotationPitch, partialTicks), rotAxisX, rotAxisY, rotAxisZ);
		GlStateManager.translate(-this.cX(entityGib.scale), -this.cY(entityGib.scale), -this.cZ(entityGib.scale));
		if(entityGib.onGround) {
			GlStateManager.translate(-rotAxisZ * (shortest / 2.0F + entityGib.zFightOffset), rotAxisY * (shortest / 2.0F + entityGib.zFightOffset), rotAxisX * (shortest / 2.0F + entityGib.zFightOffset));
		}
		GlStateManager.scale(entityGib.scale, entityGib.scale, entityGib.scale);

		if(ConfigHandler.TEXTURES.useGoreTextures && entityGib.baseTexture == GibGenerator.GORE_TEXTURE) {
			GlStateManager.pushMatrix();
			GlStateManager.matrixMode(5890);
			GlStateManager.loadIdentity();
			GlStateManager.scale(this.textureWidth / 16.0F, this.textureHeight / 16.0F, 1.0F);
			GlStateManager.matrixMode(5888);
		}

		renderGib.bindTexture(entityGib.baseTexture);
		this.drawVertices(alpha);
		if(entityGib.layerTexture != null) {
			renderGib.bindTexture(entityGib.layerTexture);
			this.drawVertices(alpha);
		}

		if(ConfigHandler.TEXTURES.useGoreTextures && entityGib.baseTexture == GibGenerator.GORE_TEXTURE) {
			GlStateManager.matrixMode(5890);
			GlStateManager.loadIdentity();
			GlStateManager.matrixMode(5888);
			GlStateManager.popMatrix();
		}

		GlStateManager.disableRescaleNormal();
		GlStateManager.color(1.0F, 1.0F, 1.0F,  1.0F);
		GlStateManager.disableBlend();
		GlStateManager.enableCull();
		GlStateManager.popMatrix();
	}

	private void drawVertices(float alpha) {
		BufferBuilder builder = Tessellator.getInstance().getBuffer();
		builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR_NORMAL);
		for(int i = 0; i < this.vertexData.length; i += VERTEX_SIZE) {
			builder.pos(this.vertexData[i], this.vertexData[i + 1], this.vertexData[i + 2])
					.tex(this.vertexData[i + 3], this.vertexData[i + 4])
					.color(1.0F, 1.0F, 1.0F, alpha)
					.normal(this.vertexData[i + 5], this.vertexData[i + 6], this.vertexData[i + 7])
					.endVertex();
		}
		Tessellator.getInstance().draw();
	}

	private static float lerpRot(float prev, float target, float partial) {
		float diff = ((target - prev + 540) % 360) - 180;
		return prev + diff * partial;
	}

	private void diveBones(AzBone bone, Matrix4f parentMatrix, FloatArrayList data, boolean isRoot) {
		Matrix4f boneMatrix = new Matrix4f(parentMatrix);
		applyRestTransform(boneMatrix, bone);

		if(isRoot || !bone.isHidden()) {
			for(GeoCube cube : bone.getCubes()) {
				Matrix4f cubeMatrix = new Matrix4f(boneMatrix);
				applyCubeTransform(cubeMatrix, cube);
				this.addCube(cube, cubeMatrix, data);
			}
		}
		if(this.renderChildren && !bone.isHidingChildren()) {
			for(AzBone childBone : bone.getChildBones()) {
				this.diveBones(childBone, boneMatrix, data, false);
			}
		}
	}

	private void addCube(GeoCube cube, Matrix4f matrix, FloatArrayList data) {
		Matrix3f normalMatrix = new Matrix3f();
		matrix.getRotationScale(normalMatrix);
		int normalFlips = cube.normalFlips();
		for(GeoQuad quad : cube.quads()) {
			if(quad == null) continue;
			Vector3f normal = new Vector3f(quad.normal().x(), quad.normal().y(), quad.normal().z());
			//Same order as AzModelRenderer: fix flat cube normals on the baked normal, then transform
			if(normalFlips != 0) {
				if(normal.x < 0 && (normalFlips & GeoCube.FLIP_X) != 0) normal.x *= -1;
				if(normal.y < 0 && (normalFlips & GeoCube.FLIP_Y) != 0) normal.y *= -1;
				if(normal.z < 0 && (normalFlips & GeoCube.FLIP_Z) != 0) normal.z *= -1;
			}
			normalMatrix.transform(normal);
			if(normal.lengthSquared() > 0.0F) normal.normalize();

			for(GeoVertex vertex : quad.vertices()) {
				Vector4f pos = new Vector4f(vertex.position().x(), vertex.position().y(), vertex.position().z(), 1.0F);
				matrix.transform(pos);

				this.minX = Math.min(this.minX, pos.x);
				this.minY = Math.min(this.minY, pos.y);
				this.minZ = Math.min(this.minZ, pos.z);
				this.maxX = Math.max(this.maxX, pos.x);
				this.maxY = Math.max(this.maxY, pos.y);
				this.maxZ = Math.max(this.maxZ, pos.z);

				data.add(pos.x);
				data.add(pos.y);
				data.add(pos.z);
				data.add(vertex.texU());
				data.add(vertex.texV());
				data.add(normal.x);
				data.add(normal.y);
				data.add(normal.z);
			}
		}
	}

	private static void applyParentRestTransforms(Matrix4f matrix, AzBone parent) {
		if(parent == null) return;
		applyParentRestTransforms(matrix, parent.getParent());
		applyRestTransform(matrix, parent);
	}

	//Mirrors RenderUtils#prepMatrixForBone using the bone's rest values
	private static void applyRestTransform(Matrix4f matrix, AzBone bone) {
		AzBoneSnapshot snapshot = bone.getInitialAzSnapshot();
		float posX = snapshot != null ? snapshot.getOffsetX() : bone.getPosX();
		float posY = snapshot != null ? snapshot.getOffsetY() : bone.getPosY();
		float posZ = snapshot != null ? snapshot.getOffsetZ() : bone.getPosZ();
		float rotX = snapshot != null ? snapshot.getRotX() : bone.getRotX();
		float rotY = snapshot != null ? snapshot.getRotY() : bone.getRotY();
		float rotZ = snapshot != null ? snapshot.getRotZ() : bone.getRotZ();
		float scaleX = snapshot != null ? snapshot.getScaleX() : bone.getScaleX();
		float scaleY = snapshot != null ? snapshot.getScaleY() : bone.getScaleY();
		float scaleZ = snapshot != null ? snapshot.getScaleZ() : bone.getScaleZ();

		translate(matrix, -posX / 16.0F, posY / 16.0F, posZ / 16.0F);
		translate(matrix, bone.getPivotX() / 16.0F, bone.getPivotY() / 16.0F, bone.getPivotZ() / 16.0F);
		if(rotZ != 0.0F) rotate(matrix, rotZ, 2);
		if(rotY != 0.0F) rotate(matrix, rotY, 1);
		if(rotX != 0.0F) rotate(matrix, rotX, 0);
		Matrix4f scale = new Matrix4f();
		scale.setIdentity();
		scale.setM00(scaleX);
		scale.setM11(scaleY);
		scale.setM22(scaleZ);
		matrix.mul(scale);
		translate(matrix, -bone.getPivotX() / 16.0F, -bone.getPivotY() / 16.0F, -bone.getPivotZ() / 16.0F);
	}

	//Mirrors GeoCube.Transform#of
	private static void applyCubeTransform(Matrix4f matrix, GeoCube cube) {
		float rotX = (float)cube.rotation().x();
		float rotY = (float)cube.rotation().y();
		float rotZ = (float)cube.rotation().z();
		if(rotX == 0.0F && rotY == 0.0F && rotZ == 0.0F) return;
		float pivotX = (float)cube.pivot().x() / 16.0F;
		float pivotY = (float)cube.pivot().y() / 16.0F;
		float pivotZ = (float)cube.pivot().z() / 16.0F;
		translate(matrix, pivotX, pivotY, pivotZ);
		if(rotZ != 0.0F) rotate(matrix, rotZ, 2);
		if(rotY != 0.0F) rotate(matrix, rotY, 1);
		if(rotX != 0.0F) rotate(matrix, rotX, 0);
		translate(matrix, -pivotX, -pivotY, -pivotZ);
	}

	private static void translate(Matrix4f matrix, float x, float y, float z) {
		Matrix4f translation = new Matrix4f();
		translation.setIdentity();
		translation.setTranslation(new Vector3f(x, y, z));
		matrix.mul(translation);
	}

	private static void rotate(Matrix4f matrix, float radians, int axis) {
		Matrix4f rotation = new Matrix4f();
		if(axis == 0) rotation.rotX(radians);
		else if(axis == 1) rotation.rotY(radians);
		else rotation.rotZ(radians);
		matrix.mul(rotation);
	}
}
