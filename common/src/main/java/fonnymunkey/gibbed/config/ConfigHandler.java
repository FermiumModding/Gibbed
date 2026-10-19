package fonnymunkey.gibbed.config;

import fonnymunkey.gibbed.client.gib.GibGenerator;
import fonnymunkey.gibbed.util.Pair;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.PartitioningSerializer;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class ConfigHandler {
	
	public static GibbedConfig CONFIG;
	
	private static Set<ResourceLocation> entityGibBlacklist = null;
	private static Set<String> entityGibModidBlacklist = null;
	private static Set<ResourceLocation> goreTextureBlacklist = null;
	private static Set<ResourceLocation> layerTextureBlacklist = null;
	private static Map<ResourceLocation,Pair<Float,Float>> entityBranchRatioOverrides = null;
	
	public static void init() {
		ConfigHolder<GibbedConfig> holder = AutoConfig.register(GibbedConfig.class, PartitioningSerializer.wrap(Toml4jConfigSerializer::new));
		CONFIG = holder.getConfig();
		holder.registerSaveListener((configHolder, config) -> {
			resetCaches();
			return InteractionResult.SUCCESS;
		});
		holder.registerLoadListener((configHolder, config) -> {
			resetCaches();
			return InteractionResult.SUCCESS;
		});
	}

	private static void resetCaches() {
		entityGibBlacklist = null;
		entityGibModidBlacklist = null;
		goreTextureBlacklist = null;
		layerTextureBlacklist = null;
		entityBranchRatioOverrides = null;
		GibGenerator.resetCache();
	}

	public static boolean shouldEntityBeGibbed(LivingEntity entity, boolean explosion) {
		if(entity == null || !entity.level().isClientSide()) return false;
		if(entity instanceof Player) {
			return entity.getRandom().nextInt(100) < CONFIG.GENERAL.playerGibChance;
		}
		if(entityGibBlacklist == null || entityGibModidBlacklist == null) createEntityGibBlacklist();
		ResourceLocation loc = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
		if(CONFIG.GENERAL.entityGibBlacklistIsWhitelist == (entityGibModidBlacklist.contains(loc.getNamespace()) || entityGibBlacklist.contains(loc))) {
			if(explosion && CONFIG.GENERAL.explosionsAlwaysGib) return true;
			return entity.getRandom().nextInt(100) < CONFIG.GENERAL.entityGibChance;
		}
		return false;
	}
	
	private static void createEntityGibBlacklist() {
		entityGibBlacklist = new HashSet<>();
		entityGibModidBlacklist = new HashSet<>();
		for(String name : CONFIG.GENERAL.entityGibBlacklist) {
			name = name.trim();
			if(name.isEmpty()) continue;
			String[] split = name.split(":");
			if(split.length == 2 && split[1].equals("*")) entityGibModidBlacklist.add(split[0]);
			else {
				ResourceLocation loc = ResourceLocation.tryParse(name);
				if(loc != null) entityGibBlacklist.add(loc);
			}
		}
	}
	
	public static boolean shouldGoreTextureBeUsed(LivingEntity entity) {
		if(entity == null || !entity.level().isClientSide()) return false;
		if(!CONFIG.TEXTURES.useGoreTextures) return false;
		if(goreTextureBlacklist == null) goreTextureBlacklist = createLocationSet(CONFIG.TEXTURES.entityGoreTextureBlacklist);
		return goreTextureBlacklist.contains(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType())) == CONFIG.TEXTURES.entityGoreTextureBlacklistIsWhitelist;
	}
	
	public static boolean shouldLayerTextureBeUsed(LivingEntity entity) {
		if(entity == null || !entity.level().isClientSide()) return false;
		if(layerTextureBlacklist == null) layerTextureBlacklist = createLocationSet(CONFIG.TEXTURES.entityLayeredTextureBlacklist);
		return layerTextureBlacklist.contains(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType())) == CONFIG.TEXTURES.layerTextureBlacklistIsWhitelist;
	}
	
	private static Set<ResourceLocation> createLocationSet(Iterable<String> names) {
		Set<ResourceLocation> set = new HashSet<>();
		for(String name : names) {
			name = name.trim();
			if(name.isEmpty()) continue;
			ResourceLocation loc = ResourceLocation.tryParse(name);
			if(loc != null) set.add(loc);
		}
		return set;
	}
	
	@Nullable
	public static Pair<Float,Float> getEntityBranchRatioOverride(LivingEntity entity) {
		if(entityBranchRatioOverrides == null) createEntityBranchRatioOverrides();
		return entityBranchRatioOverrides.get(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
	}
	
	private static void createEntityBranchRatioOverrides() {
		entityBranchRatioOverrides = new HashMap<>();
		for(String entry : CONFIG.MODELS.modelBranchRatioOverrides) {
			String[] entries = entry.split(",");
			if(entries.length != 3) continue;
			String name = entries[0].trim();
			if(name.isEmpty()) continue;
			ResourceLocation loc = ResourceLocation.tryParse(name);
			if(loc == null) continue;
			float maxSizeRatio = (float)CONFIG.MODELS.maxBranchSizeRatio;
			try {
				maxSizeRatio = Float.parseFloat(entries[1].trim());
			}
			catch(Exception ignored) { }
			float maxBranchRatio = (float)CONFIG.MODELS.maxBranchesRatio;
			try {
				maxBranchRatio = Float.parseFloat(entries[2].trim());
			}
			catch(Exception ignored) { }
			entityBranchRatioOverrides.put(loc, new Pair<>(maxSizeRatio, maxBranchRatio));
		}
	}
}