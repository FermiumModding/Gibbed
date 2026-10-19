package fonnymunkey.gibbed.client;

import fonnymunkey.gibbed.client.gib.AbstractGib;
import fonnymunkey.gibbed.client.gib.Gib;
import fonnymunkey.gibbed.client.gib.GibContext;
import fonnymunkey.gibbed.client.gib.GibGenerator;
import fonnymunkey.gibbed.client.gib.GibManager;
import fonnymunkey.gibbed.client.gib.handler.GibModelHandlers;
import fonnymunkey.gibbed.client.particle.ParticleHandler;
import fonnymunkey.gibbed.config.ConfigHandler;
import fonnymunkey.gibbed.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.*;
import java.util.Map.Entry;

public class EventHandlerClient {
    public static WeakHashMap<LivingEntity,Pair<Boolean,Integer>> dyingEntities = new WeakHashMap<>();
    private static ClientLevel currentLevel = null;

    public static void onClientDeath(LivingEntity entity) {
        if(entity == null) return;
        if(!entity.level().isClientSide()) return;
        if(Minecraft.getInstance().level == null) return;
        if(Minecraft.getInstance().player == null) return;
        if(entity.isBaby()) return;
        if(dyingEntities.containsKey(entity)) return;
        DamageSource source = entity.getLastDamageSource();
        boolean explosion = source != null && source.is(DamageTypeTags.IS_EXPLOSION);
        if(dyingEntities.size() < ConfigHandler.CONFIG.GENERAL.maximumQueuedGibbedEntities &&
                ConfigHandler.shouldEntityBeGibbed(entity, explosion)) {
            dyingEntities.put(entity, new Pair<>(explosion, 2));
        }
    }

    public static void onClientTickEnd() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if(level != currentLevel) {
            dyingEntities.clear();
            currentLevel = level;
        }
        if(level == null || minecraft.player == null) {
            dyingEntities.clear();
            GibManager.tick(null);
            clearCaptures();
            return;
        }
        if(minecraft.isPaused()) {
            clearCaptures();
            return;
        }

        GibManager.tick(level);

        Iterator<Entry<LivingEntity,Pair<Boolean,Integer>>> iter = dyingEntities.entrySet().iterator();
        if(iter.hasNext()) {
            Entry<LivingEntity,Pair<Boolean,Integer>> entry = iter.next();
            LivingEntity entity = entry.getKey();
            if(entity == null || entity.isAlive() || entity.isRemoved()) iter.remove();
            else {
                entity.hurtTime = 0;
                entity.deathTime = 0;
                entry.getValue().right--;
                if(entry.getValue().right <= 0) {
                    if(createGibs(entity, entry.getValue().left)) {
                        if(ConfigHandler.CONFIG.SOUNDS.playSoundOnGibbed) {
                            level.playLocalSound(entity.getX(),
                                                 entity.getY(),
                                                 entity.getZ(),
                                                 SoundHandler.getGibbedSound(),
                                                 SoundSource.NEUTRAL,
                                                 (float)ConfigHandler.CONFIG.SOUNDS.gibbedVolume * (1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.15F),
                                                 (float)ConfigHandler.CONFIG.SOUNDS.gibbedPitch * (1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.25F),
                                                 true);
                        }
                        entity.discard();
                    }
                    iter.remove();
                }
            }
        }

        clearCaptures();
    }

    private static void clearCaptures() {
        RenderCaptureHandler.clearCaptures();
        GibModelHandlers.clearCaptures();
    }

    private static boolean createGibs(LivingEntity entity, boolean explosion) {
        Map<AbstractGib,GibContext> gibMap = GibGenerator.getGibsFromEntity(entity);
        if(gibMap.isEmpty()) return false;
        int guaranteed = entity.getRandom().nextInt(gibMap.size());
        int index = -1;
        int particleType = ParticleHandler.getBloodParticleType(entity);
        float particleScale = (float)ConfigHandler.CONFIG.PARTICLES.particleAmountScale * Math.max(1.0F, Math.min(20.0F, entity.getMaxHealth() / gibMap.size()));
        for(Entry<AbstractGib,GibContext> gibEntry : gibMap.entrySet()) {
            index++;
            if(index == guaranteed || (explosion && ConfigHandler.CONFIG.GENERAL.explosionsSpawnAllGibs) || entity.getRandom().nextInt(100) < ConfigHandler.CONFIG.GENERAL.perGibSpawnChance) {
                Gib gib = new Gib(entity, gibEntry.getKey(), gibEntry.getValue(), explosion, particleType, particleScale);
                GibManager.addGib(gib);
                if(ConfigHandler.CONFIG.PARTICLES.spawnParticlesOnGibbed) ParticleHandler.spawnGibbedParticle(gib);
            }
        }
        return true;
    }
}