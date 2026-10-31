package fonnymunkey.gibbed.client.gib;

import fonnymunkey.gibbed.client.particle.ParticleHandler;
import fonnymunkey.gibbed.client.SoundHandler;
import fonnymunkey.gibbed.config.ConfigHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class EntityGib extends Entity {
    public AbstractGib gib = null;
    public float[] scales = null;
    public float scale = 0.0F;
    public float prepScaleX = 0.0F;
    public float prepScaleY = 0.0F;
    public float prepScaleZ = 0.0F;
    public ResourceLocation baseTexture = null;
    public ResourceLocation layerTexture = null;
    @Nullable
    public ResourceLocation stainTexture = null;
    public float zFightOffset = 0.0001F;
    public int particleType = -1;
    public float particleScale = 0.0F;
    public int groundTime = 0;
    private float pitchSpin = 0.0F;
    private float yawSpin = 0.0F;
    private boolean landed = false;
    private int unlandTicks = 0;

    public EntityGib(World world) {
        super(world);
        this.isImmuneToFire = true;
        this.setEntityInvulnerable(true);
    }

    public EntityGib(World world, EntityLivingBase parent, AbstractGib gib, float[] scales, ResourceLocation baseTexture, ResourceLocation layerTexture, boolean explosion, int particleType, float particleScale) {
        this(world);
        this.gib = gib;
        this.scales = scales;
        this.scale = scales[0];
        this.prepScaleX = scales[1];
        this.prepScaleY = scales[2];
        this.prepScaleZ = scales[3];
        this.baseTexture = baseTexture;
        this.layerTexture = layerTexture;
        this.zFightOffset = 0.001F + this.rand.nextFloat() * 0.009F;
        this.particleType = particleType;
        this.particleScale = particleScale;
        
        this.setSize(gib.bbWidth(scale, prepScaleX, prepScaleY, prepScaleZ) * (float)ConfigHandler.PHYSICS.gibCollisionWidthMult,
                     gib.bbHeight(scale, prepScaleX, prepScaleY, prepScaleZ));
        
        float yaw = parent.prevRenderYawOffset;
        double cos = Math.cos(Math.toRadians(yaw));
        double sin = Math.sin(Math.toRadians(yaw));
        double offsetX = gib.offsetX(scale, prepScaleX) * cos - gib.offsetZ(scale, prepScaleZ) * sin;
        double offsetY = gib.offsetY(scale, prepScaleY);
        double offsetZ = gib.offsetX(scale, prepScaleX) * sin + gib.offsetZ(scale, prepScaleZ) * cos;
        this.setLocationAndAngles(parent.posX - offsetX,
                                  parent.getEntityBoundingBox().minY + offsetY,
                                  parent.posZ - offsetZ,
                                  parent.prevRenderYawOffset,
                                  parent.rotationPitch);
        this.prevRotationYaw = parent.prevRenderYawOffset;
        this.prevRotationPitch = parent.rotationPitch;

        this.motionX = parent.motionX + (this.rand.nextDouble() - this.rand.nextDouble()) * (explosion ? 0.5D : 0.25D);
        this.motionY = parent.motionY + this.rand.nextDouble() * (explosion ? 0.25D : 0.125D);
        this.motionZ = parent.motionZ + (this.rand.nextDouble() - this.rand.nextDouble()) * (explosion ? 0.5D : 0.25D);

        float i = 5.0F + this.rand.nextFloat() * 40.0F;
        float j = 5.0F + this.rand.nextFloat() * 40.0F;
        if(this.rand.nextBoolean()) i *= -1.0F;
        if(this.rand.nextBoolean()) j *= -1.0F;
        this.pitchSpin = i * (float)Math.max(this.motionY, 0.3D);
        this.yawSpin = j * (float)Math.max(Math.sqrt(this.motionX * this.motionZ), 0.3D);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        
        if(this.gib == null || this.baseTexture == null || this.scale == 0.0F) {
            this.setDead();
            return;
        }
        
        this.move(MoverType.SELF, this.motionX, this.motionY, this.motionZ);
        
        this.motionY -= 0.08D;
        this.motionY *= 0.98D;
        this.motionX *= 0.91D;
        this.motionZ *= 0.91D;
        
        if(this.inWater) {
            this.motionY = 0.1D;
            this.pitchSpin *= 0.9F;
            this.yawSpin *= 0.9F;
        }
        
        this.prevRotationYaw = this.rotationYaw;
        this.prevRotationPitch = this.rotationPitch;
        if(this.onGround || this.inWater) {
            float dX = this.gib.dX(scale);
            float dY = this.gib.dY(scale);
            float dZ = this.gib.dZ(scale);
            if(dY < dX && dY < dZ) this.rotationPitch = 0;
            else this.rotationPitch = -90;
            this.motionY *= 0.8D;
            this.motionX *= 0.8D;
            this.motionZ *= 0.8D;
        }
        else {
            this.rotationPitch += this.pitchSpin;
            this.rotationYaw += this.yawSpin;
            this.pitchSpin *= 0.98F;
            this.yawSpin *= 0.98F;
        }
        
        if(!this.onGround && !this.inWater && this.landed) {
            this.unlandTicks++;
            if(this.unlandTicks > 4) this.landed = false;
        }
        else this.unlandTicks = 0;
        
        if(this.onGround) {
            if(!this.landed && this.ticksExisted > 1) {
                this.landed = true;
                if(ConfigHandler.SOUNDS.playSoundOnGibLand) {
                    this.world.playSound(this.posX,
                                    this.posY,
                                    this.posZ,
                                    SoundHandler.getGibLandSound(),
                                    SoundCategory.NEUTRAL,
                                    (float)ConfigHandler.SOUNDS.gibLandVolume * (1.0F + (this.world.rand.nextFloat() - this.world.rand.nextFloat()) * 0.15F),
                                    (float)ConfigHandler.SOUNDS.gibLandPitch * (1.0F + (this.world.rand.nextFloat() - this.world.rand.nextFloat()) * 0.15F),
                                    true);
                }
                if(ConfigHandler.PARTICLES.spawnParticlesOnGibLand) {
                    ParticleHandler.spawnGibLandParticle(this);
                }
            }
            
            if(ConfigHandler.PHYSICS.gibsPushGibs || ConfigHandler.PHYSICS.playersPushGibs || ConfigHandler.PHYSICS.entitiesPushGibs) {
                AxisAlignedBB aabb = this.getEntityBoundingBox();
                List<Entity> entities = this.world.getEntitiesWithinAABBExcludingEntity(this, aabb.grow(Math.min(0.15D, 0.15D * (aabb.maxX - aabb.minX)), 0.0D, Math.min(0.15D, 0.15D * (aabb.maxZ - aabb.minZ))));
                for(Entity entity : entities) {
                    if(entity instanceof EntityGib) {
                        if(!ConfigHandler.PHYSICS.gibsPushGibs) continue;
                        entity.applyEntityCollision(this);
                    }
                    else if(entity.canBePushed()) {
                        if(entity instanceof EntityPlayer) {
                            if(!ConfigHandler.PHYSICS.playersPushGibs) continue;
                            this.applyPushFromEntity(entity);
                        }
                        else {
                            if(!ConfigHandler.PHYSICS.entitiesPushGibs) continue;
                            this.applyPushFromEntity(entity);
                        }
                    }
                }
            }
        }
        
        if(this.groundTime > ConfigHandler.PHYSICS.gibGroundLifetime - 20) {
            if(this.groundTime > ConfigHandler.PHYSICS.gibGroundLifetime) {
                this.setDead();
            }
            this.groundTime++;
        }
        else if(this.onGround || this.inWater) {
            this.groundTime++;
        }
        else {
            this.groundTime--;
        }
        
        if(this.ticksExisted > ConfigHandler.PHYSICS.gibLifetime) {
            this.setDead();
        }
    }
    
    private void applyPushFromEntity(Entity entityIn) {
        if(!entityIn.noClip && !this.noClip) {
            double d0 = entityIn.posX - this.posX;
            double d1 = entityIn.posZ - this.posZ;
            double d2 = MathHelper.absMax(d0, d1);
            
            if(d2 >= 0.01D) {
                double d3 = 1.0D / MathHelper.sqrt(d2);
                d0 *= d3;
                d1 *= d3;
                if(d3 > 1.0D) d3 = 1.0D;
                d0 *= d3 * 0.05D * (double)(1.0F - this.entityCollisionReduction);
                d1 *= d3 * 0.05D * (double)(1.0F - this.entityCollisionReduction);
                this.addVelocity(-d0, 0.0D, -d1);
            }
        }
    }
    
    @Override
    public boolean shouldRenderInPass(int pass) {
        return pass == (this.gib.fadeOut() && (this.groundTime > ConfigHandler.PHYSICS.gibGroundLifetime - 20 || this.ticksExisted > ConfigHandler.PHYSICS.gibLifetime - 20) ? 1 : 0);
    }
    
    @Override
    public boolean canFitPassenger(Entity passenger) {
        return false;
    }
    
    @Override
    public boolean canBeRidden(Entity entityIn) {
        return false;
    }
    
    @Override
    public boolean startRiding(Entity entityIn, boolean force) {
        return false;
    }

    @Override
    public void fall(float distance, float damageMultiplier) { }
    
    @Override
    protected void entityInit() { }

    @Override
    public boolean writeToNBTOptional(NBTTagCompound par1NBTTagCompound) {
        return false;
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound nbttagcompound) { }

    @Override
    public void writeEntityToNBT(NBTTagCompound nbttagcompound) { }
}