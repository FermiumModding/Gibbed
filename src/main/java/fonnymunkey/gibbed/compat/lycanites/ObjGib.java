package fonnymunkey.gibbed.compat.lycanites;

import com.lycanitesmobs.client.obj.ObjObject;
import com.lycanitesmobs.client.obj.TessellatorModel;
import com.lycanitesmobs.client.obj.Vertex;
import fonnymunkey.gibbed.client.gib.AbstractGib;
import fonnymunkey.gibbed.client.gib.EntityGib;
import fonnymunkey.gibbed.client.gib.GibGenerator;
import fonnymunkey.gibbed.client.gib.RenderGib;
import fonnymunkey.gibbed.config.ConfigHandler;
import net.minecraft.client.renderer.GlStateManager;

import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

public class ObjGib extends AbstractGib {
	//scale prepScaleX prepScaleY prepScaleZ r g b a texX texY
	private static final float[] DEFAULT_SCALES = { 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F, 0.0F };
	private final TessellatorModel tessellator;
	private final ObjObject obj;
	private final float textureWidth;
	private final float textureHeight;
	
	public ObjGib(TessellatorModel tessellator, ObjObject obj, float textureWidth, float textureHeight) {
		super();
		this.tessellator = tessellator;
		this.obj = obj;
		this.textureWidth = textureWidth;
		this.textureHeight = textureHeight;
		
		this.diveMesh(obj.mesh.vertices);
		if(!this.valid()) return;
		
		this.cX = obj.center != null && !Float.isNaN(obj.center.x) ? obj.center.x : (this.maxX + this.minX) / 2.0F;
		this.cY = obj.center != null && !Float.isNaN(obj.center.y) ? obj.center.y : (this.maxY + this.minY) / 2.0F;
		this.cZ = obj.center != null && !Float.isNaN(obj.center.z) ? obj.center.z : (this.maxZ + this.minZ) / 2.0F;
		this.dX = this.maxX - this.minX;
		this.dY = this.maxY - this.minY;
		this.dZ = this.maxZ - this.minZ;
	}
	
	@Override
	public float cX(float scale) {
		return this.cX;
	}
	
	@Override
	public float cY(float scale) {
		return this.cY;
	}
	
	@Override
	public float cZ(float scale) {
		return this.cZ;
	}
	
	@Override
	public float dX(float scale) {
		return this.dX;
	}
	
	@Override
	public float dY(float scale) {
		return this.dY;
	}
	
	@Override
	public float dZ(float scale) {
		return this.dZ;
	}
	
	@Override
	public float offsetX(float scale, float prepScaleX) {
		return -(this.minX + this.dX / 2.0F);
	}
	
	@Override
	public float offsetY(float scale, float prepScaleY) {
		return this.minY + this.dY / 2.0F;
	}
	
	@Override
	public float offsetZ(float scale, float prepScaleZ) {
		return -(this.minZ + this.dZ / 2.0F);
	}
	
	@Override
	public float bbWidth(float scale, float prepScaleX, float prepScaleY, float prepScaleZ) {
		return Math.max(this.dX, Math.max(this.dY, this.dZ));
	}
	
	@Override
	public float bbHeight(float scale, float prepScaleX, float prepScaleY, float prepScaleZ) {
		return Math.min(this.dX, Math.min(this.dY, this.dZ));
	}
	
	@Override
	public Object getContextKey() {
		return this.obj;
	}
	
	@Override
	public float[] getDefaultScales() {
		return DEFAULT_SCALES;
	}
	
	@Override
	public boolean fadeOut() {
		return false;
	}
	
	@Override
	public boolean flipYaw() {
		return true;
	}
	
	@Override
	public boolean flipY() {
		return false;
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
		
		GlStateManager.pushMatrix();
		GlStateManager.disableCull();
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		
		GlStateManager.translate(x, y, z);
		GlStateManager.rotate(360.0F - lerpRot(entityGib.prevRotationYaw, entityGib.rotationYaw, partialTicks), 0.0F, 1.0F, 0.0F);
		GlStateManager.enableRescaleNormal();
		GlStateManager.scale(-1.0F, 1.0F, 1.0F);
		GlStateManager.scale(entityGib.prepScaleX, entityGib.prepScaleY, entityGib.prepScaleZ);
		
		GlStateManager.rotate(lerpRot(entityGib.prevRotationPitch, entityGib.rotationPitch, partialTicks), rotAxisX, rotAxisY, rotAxisZ);
		GlStateManager.translate(-this.cX(entityGib.scale), -this.cY(entityGib.scale), -this.cZ(entityGib.scale));
		if(entityGib.onGround) {
			GlStateManager.translate(-rotAxisZ * (shortest / 2.0F + entityGib.zFightOffset), rotAxisY * (shortest / 2.0F + entityGib.zFightOffset), rotAxisX * (shortest / 2.0F + entityGib.zFightOffset));
		}
		
		if(ConfigHandler.TEXTURES.useGoreTextures && entityGib.baseTexture == GibGenerator.GORE_TEXTURE) {
			GlStateManager.pushMatrix();
			GlStateManager.matrixMode(5890);
			GlStateManager.loadIdentity();
			GlStateManager.scale(this.textureWidth / 16.0F, this.textureHeight / 16.0F, 1.0F);
			GlStateManager.matrixMode(5888);
		}
		
		renderGib.bindTexture(entityGib.baseTexture);
		this.tessellator.renderGroup(this.obj, new Vector4f(scales[4], scales[5], scales[6], scales[7]), new Vector2f(scales[8], scales[9]), null);
		if(entityGib.layerTexture != null) {
			renderGib.bindTexture(entityGib.layerTexture);
			this.tessellator.renderGroup(this.obj, new Vector4f(scales[4], scales[5], scales[6], scales[7]), new Vector2f(scales[8], scales[9]), null);
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
	
	private static float lerpRot(float prev, float target, float partial) {
		float diff = ((target - prev + 540) % 360) - 180;
		return prev + diff * partial;
	}
	
	private void diveMesh(Vertex[] vertices) {
		for(Vertex vert : vertices) {
			Vector3f vertPos = vert.getPos();
			this.minX = Math.min(this.minX, vertPos.x);
			this.minY = Math.min(this.minY, vertPos.y);
			this.minZ = Math.min(this.minZ, vertPos.z);
			this.maxX = Math.max(this.maxX, vertPos.x);
			this.maxY = Math.max(this.maxY, vertPos.y);
			this.maxZ = Math.max(this.maxZ, vertPos.z);
		}
	}
}