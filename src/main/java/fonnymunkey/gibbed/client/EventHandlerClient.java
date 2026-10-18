package fonnymunkey.gibbed.client;

import fonnymunkey.gibbed.client.gib.EntityGib;
import fonnymunkey.gibbed.client.gib.AbstractGib;
import fonnymunkey.gibbed.client.gib.GibGenerator;
import fonnymunkey.gibbed.client.particle.ParticleHandler;
import fonnymunkey.gibbed.compat.ModLoadedUtil;
import fonnymunkey.gibbed.compat.azurelib.AzureLibCompat;
import fonnymunkey.gibbed.compat.geckolib.GeckolibCompat;
import fonnymunkey.gibbed.config.ConfigHandler;
import fonnymunkey.gibbed.util.Pair;
import fonnymunkey.gibbed.util.Triple;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;
import java.util.Map.Entry;

public class EventHandlerClient {
    public static WeakHashMap<EntityLivingBase,Pair<Boolean, Integer>> dyingEntities = new WeakHashMap<>();
    
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if(event.getEntityLiving() == null) return;
        if(!event.getEntityLiving().world.isRemote) return;
        if(event.getEntityLiving() instanceof EntityPlayer &&
                !(event.getEntityLiving() instanceof AbstractClientPlayer)) return;
        if(Minecraft.getMinecraft().world == null) return;
        if(Minecraft.getMinecraft().player == null) return;
        if(event.getEntityLiving().isChild()) return;
        boolean explosion = event.getSource().isExplosion();
        if(dyingEntities.size() < ConfigHandler.GENERAL.maximumQueuedGibbedEntities &&
                ConfigHandler.shouldEntityBeGibbed(event.getEntityLiving(), explosion)) {
            dyingEntities.put(event.getEntityLiving(), new Pair<>(explosion, 2));
        }
    }
    
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onWorldLoad(WorldEvent.Load event) {
        dyingEntities.clear();
    }
    
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onWorldUnload(WorldEvent.Unload event) {
        dyingEntities.clear();
    }
    
    @SubscribeEvent
    public static void onClientTickEnd(TickEvent.ClientTickEvent event) {
        if(event.phase != TickEvent.Phase.END) return;
        WorldClient world = Minecraft.getMinecraft().world;
        if(world == null || Minecraft.getMinecraft().player == null) {
            dyingEntities.clear();
            return;
        }
        if(Minecraft.getMinecraft().isGamePaused()) return;
        
        Iterator<Entry<EntityLivingBase,Pair<Boolean, Integer>>> iter = dyingEntities.entrySet().iterator();
        if(iter.hasNext()) {
            Entry<EntityLivingBase,Pair<Boolean, Integer>> entry = iter.next();
            EntityLivingBase entity = entry.getKey();
            if(entity == null || entity.isEntityAlive() || entity.isDead) iter.remove();
            else {
                entity.hurtTime = 0;
                entity.deathTime = 0;
                entry.getValue().right--;
                if(entry.getValue().right <= 0) {
                    if(createGibs(world, entry.getKey(), entry.getValue().left)) {
                        if(ConfigHandler.SOUNDS.playSoundOnGibbed) {
                            world.playSound(entity.posX,
                                            entity.posY,
                                            entity.posZ,
                                            SoundHandler.getGibbedSound(),
                                            SoundCategory.NEUTRAL,
                                            (float)ConfigHandler.SOUNDS.gibbedVolume * (1.0F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.15F),
                                            (float)ConfigHandler.SOUNDS.gibbedPitch * (1.0F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.25F),
                                            true);
                        }
                        entry.getKey().setDead();
                    }
                    iter.remove();
                }
            }
        }
    }
    
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onClientTickEndLowest(TickEvent.ClientTickEvent event) {
        if(event.phase != TickEvent.Phase.END) return;
        RenderCaptureHandler.clearCaptures();
        if(ModLoadedUtil.isGeckolibLoaded()) GeckolibCompat.clearCaptures();
        if(ModLoadedUtil.isAzureLibLoaded()) AzureLibCompat.clearCaptures();
    }
    
    private static boolean createGibs(World world, EntityLivingBase entity, boolean explosion) {
        Map<AbstractGib,Triple<float[],ResourceLocation,ResourceLocation>> gibMap = GibGenerator.getGibsFromEntity(entity);
        if(gibMap.isEmpty()) return false;
        int guaranteed = entity.getRNG().nextInt(gibMap.size());
        int index = -1;
        int particleType = ParticleHandler.getBloodParticleType(entity);
        float particleScale = ConfigHandler.PARTICLES.particleAmountScale * Math.max(1.0F, Math.min(20.0F, entity.getMaxHealth() / gibMap.size()));
        for(Entry<AbstractGib,Triple<float[],ResourceLocation,ResourceLocation>> gibEntry : gibMap.entrySet()) {
            index++;
            if(index == guaranteed || (explosion && ConfigHandler.GENERAL.explosionsSpawnAllGibs) || entity.getRNG().nextFloat() < ConfigHandler.GENERAL.perGibSpawnChance) {
                EntityGib gib = new EntityGib(world, entity, gibEntry.getKey(), gibEntry.getValue().left, gibEntry.getValue().middle, gibEntry.getValue().right, explosion, particleType, particleScale);
                world.spawnEntity(gib);
                if(ConfigHandler.PARTICLES.spawnParticlesOnGibbed) ParticleHandler.spawnGibbedParticle(gib);
            }
        }
        return true;
    }
    
    public static void addDyingEntityManual(EntityLivingBase entity) {
        if(!dyingEntities.containsKey(entity)) {
            if(dyingEntities.size() < ConfigHandler.GENERAL.maximumQueuedGibbedEntities &&
                    ConfigHandler.shouldEntityBeGibbed(entity, false)) {
                dyingEntities.put(entity, new Pair<>(false, 1));
            }
        }
    }
}