package fonnymunkey.gibbed.compat.azurelib;

import fonnymunkey.gibbed.compat.BoneGib;
import mod.azure.azurelib.cache.object.GeoCube;
import mod.azure.azurelib.cache.object.GeoQuad;
import mod.azure.azurelib.cache.object.GeoVertex;
import mod.azure.azurelib.model.AzBone;
import mod.azure.azurelib.model.AzBoneSnapshot;
import mod.azure.azurelib.util.client.RenderUtils;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.List;

public class AzGib extends BoneGib<AzBone> {

	public AzGib(AzBone bone, boolean renderChildren) {
		super(bone, renderChildren);
	}

	@Override
	protected AzBone getParent(AzBone bone) {
		return bone.getParent();
	}

	@Override
	protected List<AzBone> getChildren(AzBone bone) {
		return bone.getChildBones();
	}

	@Override
	protected boolean isHidden(AzBone bone) {
		return bone.isHidden();
	}

	@Override
	protected boolean isHidingChildren(AzBone bone) {
		return bone.isHidingChildren();
	}

	@Override
	protected void applyRestTransform(Matrix4f matrix, AzBone bone) {
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

		matrix.translate(-posX / 16.0F, posY / 16.0F, posZ / 16.0F);
		matrix.translate(bone.getPivotX() / 16.0F, bone.getPivotY() / 16.0F, bone.getPivotZ() / 16.0F);
		if(rotZ != 0.0F) matrix.rotateZ(rotZ);
		if(rotY != 0.0F) matrix.rotateY(rotY);
		if(rotX != 0.0F) matrix.rotateX(rotX);
		matrix.scale(scaleX, scaleY, scaleZ);
		matrix.translate(-bone.getPivotX() / 16.0F, -bone.getPivotY() / 16.0F, -bone.getPivotZ() / 16.0F);
	}

	@Override
	protected void bakeCubes(AzBone bone, Matrix4f boneMatrix) {
		Vector4f position = new Vector4f();
		for(GeoCube cube : bone.getCubes()) {
			Matrix4f cubeMatrix = new Matrix4f(boneMatrix);
			if(!cube.transform().identity()) cubeMatrix.mul(cube.transform().pose());
			Matrix3f normalMatrix = cubeMatrix.normal(new Matrix3f());

			for(GeoQuad quad : cube.quads()) {
				if(quad == null) continue;
				Vector3f normal = normalMatrix.transform(new Vector3f(quad.normal()));
				if(normal.lengthSquared() > 0.0F) normal.normalize();
				RenderUtils.fixInvertedFlatCube(cube.normalFlips(), normal);

				for(GeoVertex vertex : quad.vertices()) {
					cubeMatrix.transform(position.set(vertex.position(), 1.0F));
					this.addVertex(position, vertex.texU(), vertex.texV(), normal);
				}
			}
		}
	}
}