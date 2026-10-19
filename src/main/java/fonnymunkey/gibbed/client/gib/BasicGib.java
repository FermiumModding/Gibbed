package fonnymunkey.gibbed.client.gib;

import fonnymunkey.gibbed.config.ConfigHandler;
import fonnymunkey.gibbed.util.IModelRenderer;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.math.MathHelper;

public class BasicGib extends AbstractGib {
	//scale prepScaleX prepScaleY prepScaleZ
	private static final float[] DEFAULT_SCALES = { 0.0625F, 1.0F, 1.0F, 1.0F };
	private final ModelRenderer renderer;
	private final boolean renderChildren;
	
	public BasicGib(ModelRenderer renderer, boolean renderChildren) {
		super();
		this.renderer = renderer;
		this.renderChildren = renderChildren;
		
		diveBoxes(renderer, 0, 0, 0);
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
		return (this.minX + this.dX / 2.0F + ((IModelRenderer)this.renderer).gibbed$getDefaultRotationPointX()) * scale * prepScaleX;
	}
	
	@Override
	public float offsetY(float scale, float prepScaleY) {
		return 1.501F * prepScaleY - (this.minY + this.dY / 2.0F + ((IModelRenderer)this.renderer).gibbed$getDefaultRotationPointY()) * scale * prepScaleY;
	}
	
	@Override
	public float offsetZ(float scale, float prepScaleZ) {
		return (this.minZ + this.dZ / 2.0F + ((IModelRenderer)this.renderer).gibbed$getDefaultRotationPointZ()) * scale * prepScaleZ;
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
		return this.renderer;
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
		return true;
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
		GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
		
		GlStateManager.translate(x, y, z);
		GlStateManager.rotate(180.0F - lerpRot(entityGib.prevRotationYaw, entityGib.rotationYaw, partialTicks), 0.0F, 1.0F, 0.0F);
		GlStateManager.enableRescaleNormal();
		GlStateManager.scale(-1.0F, -1.0F, 1.0F);
		GlStateManager.scale(entityGib.prepScaleX, entityGib.prepScaleY, entityGib.prepScaleZ);
		
		GlStateManager.rotate(lerpRot(entityGib.prevRotationPitch, entityGib.rotationPitch, partialTicks), rotAxisX, rotAxisY, rotAxisZ);
		GlStateManager.translate(-this.cX(entityGib.scale), -this.cY(entityGib.scale), -this.cZ(entityGib.scale));
		if(entityGib.onGround) {
			GlStateManager.translate(rotAxisZ * (shortest / 2.0F + entityGib.zFightOffset), -rotAxisY * (shortest / 2.0F + entityGib.zFightOffset), -rotAxisX * (shortest / 2.0F + entityGib.zFightOffset));
		}
		
		if(ConfigHandler.TEXTURES.useGoreTextures && entityGib.baseTexture == GibGenerator.GORE_TEXTURE) {
			GlStateManager.pushMatrix();
			GlStateManager.matrixMode(5890);
			GlStateManager.loadIdentity();
			GlStateManager.scale(this.renderer.textureWidth / 16.0F, this.renderer.textureHeight / 16.0F, 1.0F);
			GlStateManager.matrixMode(5888);
		}
		
		float modelRotPointX = this.renderer.rotationPointX;
		float modelRotPointY = this.renderer.rotationPointY;
		float modelRotPointZ = this.renderer.rotationPointZ;
		float modelRotX = this.renderer.rotateAngleX;
		float modelRotY = this.renderer.rotateAngleY;
		float modelRotZ = this.renderer.rotateAngleZ;
		float modelX = this.renderer.offsetX;
		float modelY = this.renderer.offsetY;
		float modelZ = this.renderer.offsetZ;
		if(this.renderChildren) ((IModelRenderer)this.renderer).gibbed$setToDefaultStates();
		//Set primary model states to 0 to render flat, let children use rotations
		this.renderer.offsetX = 0;
		this.renderer.offsetY = 0;
		this.renderer.offsetZ = 0;
		this.renderer.rotateAngleX = 0;
		this.renderer.rotateAngleY = 0;
		this.renderer.rotateAngleZ = 0;
		this.renderer.rotationPointX = 0;
		this.renderer.rotationPointY = 0;
		this.renderer.rotationPointZ = 0;
		
		renderGib.bindTexture(entityGib.baseTexture);
		if(this.renderChildren) this.renderer.render(entityGib.scale);
		else ((IModelRenderer)this.renderer).gibbed$renderSingular(entityGib.scale);
		
		if(entityGib.layerTexture != null) {
			renderGib.bindTexture(entityGib.layerTexture);
			if(this.renderChildren) this.renderer.render(entityGib.scale);
			else ((IModelRenderer)this.renderer).gibbed$renderSingular(entityGib.scale);
		}
		
		this.renderer.offsetX = modelX;
		this.renderer.offsetY = modelY;
		this.renderer.offsetZ = modelZ;
		this.renderer.rotateAngleX = modelRotX;
		this.renderer.rotateAngleY = modelRotY;
		this.renderer.rotateAngleZ = modelRotZ;
		this.renderer.rotationPointX = modelRotPointX;
		this.renderer.rotationPointY = modelRotPointY;
		this.renderer.rotationPointZ = modelRotPointZ;
		
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
	
	//TODO this does not account for nested rotations, however fixing it would require much more processing and less caching
	//ignore for now
	private void diveBoxes(ModelRenderer renderer, float rotPointX, float rotPointY, float rotPointZ) {
		for(ModelBox box : renderer.cubeList) {
			if(box.posX1 + rotPointX < minX) minX = box.posX1 + rotPointX;
			if(box.posX2 + rotPointX > maxX) maxX = box.posX2 + rotPointX;
			if(box.posY1 + rotPointY < minY) minY = box.posY1 + rotPointY;
			if(box.posY2 + rotPointY > maxY) maxY = box.posY2 + rotPointY;
			if(box.posZ1 + rotPointZ < minZ) minZ = box.posZ1 + rotPointZ;
			if(box.posZ2 + rotPointZ > maxZ) maxZ = box.posZ2 + rotPointZ;
		}
		if(renderer.childModels != null && this.renderChildren) {
			for(ModelRenderer childRenderer : renderer.childModels) {
				diveBoxes(childRenderer,
						  rotPointX + ((IModelRenderer)childRenderer).gibbed$getDefaultRotationPointX(),
						  rotPointY + ((IModelRenderer)childRenderer).gibbed$getDefaultRotationPointY(),
						  rotPointZ + ((IModelRenderer)childRenderer).gibbed$getDefaultRotationPointZ());
			}
		}
	}
}
