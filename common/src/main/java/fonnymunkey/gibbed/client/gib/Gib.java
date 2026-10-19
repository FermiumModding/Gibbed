package fonnymunkey.gibbed.client.gib;

import fonnymunkey.gibbed.client.SoundHandler;
import fonnymunkey.gibbed.client.particle.ParticleHandler;
import fonnymunkey.gibbed.config.ConfigHandler;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.List;

public class Gib {
    public final AbstractGib gib;
    public final float[] scales;
    public final float scale;
    public final float prepScaleX;
    public final float prepScaleY;
    public final float prepScaleZ;
    public final ResourceLocation baseTexture;
    public final ResourceLocation layerTexture;
    public final float goreScaleU;
    public final float goreScaleV;
    public final float zFightOffset;
    public final int particleType;
    public final float particleScale;
    public final RandomSource random = RandomSource.create();
    private final float width;
    private final float height;

    public double x, y, z;
    public double xo, yo, zo;
    public double motionX, motionY, motionZ;
    public float yRot, xRot;
    public float yRotO, xRotO;
    public boolean onGround = false;
    public boolean inWater = false;
    public int tickCount = 0;
    public int groundTime = 0;
    private boolean removed = false;
    private float pitchSpin = 0.0F;
    private float yawSpin = 0.0F;
    private boolean landed = false;
    private int unlandTicks = 0;

    public Gib(LivingEntity parent, AbstractGib gib, GibContext context, boolean explosion, int particleType, float particleScale) {
        this.gib = gib;
        this.scales = context.scales();
        this.scale = this.scales[0];
        this.prepScaleX = this.scales[1];
        this.prepScaleY = this.scales[2];
        this.prepScaleZ = this.scales[3];
        this.baseTexture = context.baseTexture();
        this.layerTexture = context.layerTexture();
        this.goreScaleU = context.goreScaleU();
        this.goreScaleV = context.goreScaleV();
        this.zFightOffset = 0.001F + this.random.nextFloat() * 0.009F;
        this.particleType = particleType;
        this.particleScale = particleScale;
        
        this.width = gib.bbWidth(this.scale, this.prepScaleX, this.prepScaleY, this.prepScaleZ) * (float)ConfigHandler.CONFIG.PHYSICS.gibCollisionWidthMult;
        this.height = gib.bbHeight(this.scale, this.prepScaleX, this.prepScaleY, this.prepScaleZ);
        
        float yaw = parent.yBodyRotO;
        double cos = Math.cos(Math.toRadians(yaw));
        double sin = Math.sin(Math.toRadians(yaw));
        double offsetX = gib.offsetX(this.scale, this.prepScaleX) * cos - gib.offsetZ(this.scale, this.prepScaleZ) * sin;
        double offsetY = gib.offsetY(this.scale, this.prepScaleY);
        double offsetZ = gib.offsetX(this.scale, this.prepScaleX) * sin + gib.offsetZ(this.scale, this.prepScaleZ) * cos;
        this.x = this.xo = parent.getX() - offsetX;
        this.y = this.yo = parent.getBoundingBox().minY + Math.max(0.0D, offsetY);
        this.z = this.zo = parent.getZ() - offsetZ;
        this.yRot = this.yRotO = parent.yBodyRotO;
        this.xRot = this.xRotO = parent.getXRot();

        Vec3 parentMotion = parent.getDeltaMovement();
        this.motionX = parentMotion.x + (this.random.nextDouble() - this.random.nextDouble()) * (explosion ? 0.5D : 0.25D);
        this.motionY = parentMotion.y + this.random.nextDouble() * (explosion ? 0.25D : 0.125D);
        this.motionZ = parentMotion.z + (this.random.nextDouble() - this.random.nextDouble()) * (explosion ? 0.5D : 0.25D);

        float i = 5.0F + this.random.nextFloat() * 40.0F;
        float j = 5.0F + this.random.nextFloat() * 40.0F;
        if(this.random.nextBoolean()) i *= -1.0F;
        if(this.random.nextBoolean()) j *= -1.0F;
        this.pitchSpin = i * (float)Math.max(this.motionY, 0.3D);
        this.yawSpin = j * (float)Math.max(Math.sqrt(Math.abs(this.motionX * this.motionZ)), 0.3D);
    }

    public void tick(ClientLevel level, List<Gib> gibs) {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.tickCount++;
        this.inWater = this.isInWater(level);
        
        if(this.gib == null || this.baseTexture == null || this.scale == 0.0F) {
            this.remove();
            return;
        }
        
        this.move(level);
        
        this.motionY -= 0.08D;
        this.motionY *= 0.98D;
        this.motionX *= 0.91D;
        this.motionZ *= 0.91D;
        
        if(this.inWater) {
            this.motionY = 0.1D;
            this.pitchSpin *= 0.9F;
            this.yawSpin *= 0.9F;
        }
        
        this.yRotO = this.yRot;
        this.xRotO = this.xRot;
        if(this.onGround || this.inWater) {
            float dX = this.gib.dX(this.scale);
            float dY = this.gib.dY(this.scale);
            float dZ = this.gib.dZ(this.scale);
            if(dY < dX && dY < dZ) this.xRot = 0;
            else this.xRot = -90;
            this.motionY *= 0.8D;
            this.motionX *= 0.8D;
            this.motionZ *= 0.8D;
        }
        else {
            this.xRot += this.pitchSpin;
            this.yRot += this.yawSpin;
            this.pitchSpin *= 0.98F;
            this.yawSpin *= 0.98F;
        }
        if(!Float.isFinite(this.xRot)) this.xRot = this.xRotO;
        if(!Float.isFinite(this.yRot)) this.yRot = this.yRotO;
        
        if(!this.onGround && !this.inWater && this.landed) {
            this.unlandTicks++;
            if(this.unlandTicks > 4) this.landed = false;
        }
        else this.unlandTicks = 0;
        
        if(this.onGround) {
            if(!this.landed && this.tickCount > 1) {
                this.landed = true;
                if(ConfigHandler.CONFIG.SOUNDS.playSoundOnGibLand) {
                    level.playLocalSound(this.x,
                                         this.y,
                                         this.z,
                                         SoundHandler.getGibLandSound(),
                                         SoundSource.NEUTRAL,
                                         (float)ConfigHandler.CONFIG.SOUNDS.gibLandVolume * (1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.15F),
                                         (float)ConfigHandler.CONFIG.SOUNDS.gibLandPitch * (1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.15F),
                                         true);
                }
                if(ConfigHandler.CONFIG.PARTICLES.spawnParticlesOnGibLand) {
                    ParticleHandler.spawnGibLandParticle(this);
                }
            }
            
            if(ConfigHandler.CONFIG.PHYSICS.gibsPushGibs || ConfigHandler.CONFIG.PHYSICS.playersPushGibs || ConfigHandler.CONFIG.PHYSICS.entitiesPushGibs) {
                AABB aabb = this.getBoundingBox();
                AABB pushBox = aabb.inflate(Math.min(0.15D, 0.15D * (aabb.maxX - aabb.minX)), 0.0D, Math.min(0.15D, 0.15D * (aabb.maxZ - aabb.minZ)));
                if(ConfigHandler.CONFIG.PHYSICS.gibsPushGibs) {
                    for(Gib other : gibs) {
                        if(other != this && !other.removed && other.getBoundingBox().intersects(pushBox)) {
                            other.pushGib(this);
                        }
                    }
                }
                if(ConfigHandler.CONFIG.PHYSICS.playersPushGibs || ConfigHandler.CONFIG.PHYSICS.entitiesPushGibs) {
                    for(Entity entity : level.getEntities((Entity)null, pushBox, Entity::isPushable)) {
                        if(entity instanceof Player) {
                            if(!ConfigHandler.CONFIG.PHYSICS.playersPushGibs) continue;
                        }
                        else if(!ConfigHandler.CONFIG.PHYSICS.entitiesPushGibs) continue;
                        this.applyPushFromEntity(entity);
                    }
                }
            }
        }
        
        if(this.groundTime > ConfigHandler.CONFIG.PHYSICS.gibGroundLifetime - 20) {
            if(this.groundTime > ConfigHandler.CONFIG.PHYSICS.gibGroundLifetime) {
                this.remove();
            }
            this.groundTime++;
        }
        else if(this.onGround || this.inWater) {
            this.groundTime++;
        }
        else {
            this.groundTime--;
        }
        
        if(this.tickCount > ConfigHandler.CONFIG.PHYSICS.gibLifetime) {
            this.remove();
        }
    }

    private void move(ClientLevel level) {
        Vec3 movement = new Vec3(this.motionX, this.motionY, this.motionZ);
        Vec3 collided = Entity.collideBoundingBox(null, movement, this.getBoundingBox(), level, Collections.emptyList());
        boolean collidedX = !Mth.equal(movement.x, collided.x);
        boolean collidedY = movement.y != collided.y;
        boolean collidedZ = !Mth.equal(movement.z, collided.z);
        this.onGround = collidedY && movement.y < 0.0D;
        this.x += collided.x;
        this.y += collided.y;
        this.z += collided.z;
        if(collidedX) this.motionX = 0.0D;
        if(collidedZ) this.motionZ = 0.0D;
        if(collidedY) this.motionY = 0.0D;
    }
    
    private boolean isInWater(ClientLevel level) {
        AABB aabb = this.getBoundingBox().deflate(0.001D);
        int minX = Mth.floor(aabb.minX);
        int maxX = Mth.ceil(aabb.maxX);
        int minY = Mth.floor(aabb.minY);
        int maxY = Mth.ceil(aabb.maxY);
        int minZ = Mth.floor(aabb.minZ);
        int maxZ = Mth.ceil(aabb.maxZ);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for(int i = minX; i < maxX; i++) {
            for(int j = minY; j < maxY; j++) {
                for(int k = minZ; k < maxZ; k++) {
                    pos.set(i, j, k);
                    FluidState fluidState = level.getFluidState(pos);
                    if(fluidState.is(FluidTags.WATER) && j + fluidState.getHeight(level, pos) >= aabb.minY) return true;
                }
            }
        }
        return false;
    }
    
    private void pushGib(Gib other) {
        double d0 = other.x - this.x;
        double d1 = other.z - this.z;
        double d2 = Mth.absMax(d0, d1);
        if(d2 >= 0.01D) {
            d2 = Math.sqrt(d2);
            d0 /= d2;
            d1 /= d2;
            double d3 = 1.0D / d2;
            if(d3 > 1.0D) d3 = 1.0D;
            d0 *= d3 * 0.05D;
            d1 *= d3 * 0.05D;
            this.motionX -= d0;
            this.motionZ -= d1;
            other.motionX += d0;
            other.motionZ += d1;
        }
    }

    private void applyPushFromEntity(Entity entity) {
        if(!entity.noPhysics) {
            double d0 = entity.getX() - this.x;
            double d1 = entity.getZ() - this.z;
            double d2 = Mth.absMax(d0, d1);

            if(d2 >= 0.01D) {
                double d3 = 1.0D / Math.sqrt(d2);
                d0 *= d3;
                d1 *= d3;
                if(d3 > 1.0D) d3 = 1.0D;
                d0 *= d3 * 0.05D;
                d1 *= d3 * 0.05D;
                this.motionX -= d0;
                this.motionZ -= d1;
            }
        }
    }
    
    public AABB getBoundingBox() {
        double halfWidth = this.width / 2.0D;
        return new AABB(this.x - halfWidth, this.y, this.z - halfWidth, this.x + halfWidth, this.y + this.height, this.z + halfWidth);
    }
    
    public double getX(float partialTicks) {
        return Mth.lerp(partialTicks, this.xo, this.x);
    }
    
    public double getY(float partialTicks) {
        return Mth.lerp(partialTicks, this.yo, this.y);
    }
    
    public double getZ(float partialTicks) {
        return Mth.lerp(partialTicks, this.zo, this.z);
    }

    public float getYRot(float partialTicks) {
        return lerpRot(this.yRotO, this.yRot, partialTicks);
    }
    
    public float getXRot(float partialTicks) {
        return lerpRot(this.xRotO, this.xRot, partialTicks);
    }

    private static float lerpRot(float prev, float target, float partial) {
        float diff = ((target - prev + 540) % 360) - 180;
        return prev + diff * partial;
    }

    public float getAlpha(float partialTicks) {
        int groundLifetime = ConfigHandler.CONFIG.PHYSICS.gibGroundLifetime;
        int lifetime = ConfigHandler.CONFIG.PHYSICS.gibLifetime;
        if(this.groundTime > groundLifetime - 20 || this.tickCount > lifetime - 20) {
            return Mth.clamp(1.0F - ((float)Math.max(this.groundTime - (groundLifetime - 20), this.tickCount - (lifetime - 20)) + partialTicks) / 20.0F, 0.0F, 1.0F);
        }
        return 1.0F;
    }

    public void remove() {
        this.removed = true;
    }

    public boolean isRemoved() {
        return this.removed;
    }
}