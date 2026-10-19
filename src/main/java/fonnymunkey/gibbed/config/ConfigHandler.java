package fonnymunkey.gibbed.config;

import fonnymunkey.gibbed.Gibbed;
import fonnymunkey.gibbed.client.gib.GibGenerator;
import fonnymunkey.gibbed.util.Pair;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Config(modid = Gibbed.MODID)
public class ConfigHandler {
	
	@Config.Name("General")
	public static final General GENERAL = new General();
	
	@Config.Name("Physics")
	public static final Physics PHYSICS = new Physics();
	
	@Config.Name("Models")
	public static final Models MODELS = new Models();
	
	@Config.Name("Textures")
	public static final Textures TEXTURES = new Textures();
	
	@Config.Name("Sounds")
	public static final Sounds SOUNDS = new Sounds();
	
	@Config.Name("Particles")
	public static final Particles PARTICLES = new Particles();
	
	public static class General {
		
		@Config.Comment("Chance on death for a player to be gibbed" + "\n" +
				"Player gibbing is unaffected by explosion config due to MC/Forge handling of clientside player entities")
		@Config.Name("Player Gib Chance")
		@Config.RangeDouble(min = 0.0D, max = 1.0D)
		public double playerGibChance = 1.0D;
		
		@Config.Comment("Chance on death for an entity to be gibbed")
		@Config.Name("Entity Gib Chance")
		@Config.RangeDouble(min = 0.0D, max = 1.0D)
		public double entityGibChance = 1.0D;
		
		@Config.Comment("Chance per gib to be spawned when an entity is already being gibbed (Minimum 1 gib will always spawn)")
		@Config.Name("Per Gib Spawn Chance")
		@Config.RangeDouble(min = 0.0D, max = 1.0D)
		public double perGibSpawnChance = 1.0D;
		
		@Config.Comment("If death by explosion should override gib chance and always gib")
		@Config.Name("Explosions Always Gib")
		public boolean explosionsAlwaysGib = true;
		
		@Config.Comment("If death by explosion should override per gib spawn chance and spawn all gibs")
		@Config.Name("Explosions Spawn All Gibs")
		public boolean explosionsSpawnAllGibs = true;
		
		@Config.Comment("Blacklist of Entity Registry Names to blacklist from gibbing")
		@Config.Name("Entity Gib Blacklist")
		public String[] entityGibBlacklist = {
				"minecraft:slime",
				"minecraft:magma_cube",
				"minecraft:ender_dragon",
				"iceandfire:firedragon",
				"iceandfire:icedragon",
				"iceandfire:lightningdragon",
				"iceandfire:shivaxi_dragon",
				"iceandfire:black_frost",
				"iceandfire:dread_queen",
				"iceandfire:dread_lich",
				"iceandfire:dread_knight",
				"iceandfire:dread_thrall",
				"iceandfire:ghost",
				"iceandfire:gorgon",
				"iceandfire:if_hydra",
				"lycanitesmobs:amalgalich",
				"lycanitesmobs:asmodeus",
				"lycanitesmobs:rahovart",
				"lycanitesmobs:sylph",
				"lycanitesmobs:wisp",
				"lycanitesmobs:jengu",
				"lycanitesmobs:cinder",
				"lycanitesmobs:reiver",
				"lycanitesmobs:wraith",
				"lycanitesmobs:zephyr",
				"lycanitesmobs:djinn",
				"lycanitesmobs:grue",
				"lycanitesmobs:spectre",
				"defiledlands:the_destroyer",
				"defiledlands:the_mourner",
				"defiledlands:slime_defiled",
				"betternether:firefly",
				"charm:spectre",
				"srparasites:*"//To many to test with how many have custom death handling
		};
		
		@Config.Comment("Sets the Entity Gib Blacklist to instead act as a whitelist")
		@Config.Name("Entity Gib Blacklist is Whitelist")
		public boolean entityGibBlacklistIsWhitelist = false;
		
		@Config.Comment("Maximum amount of dying entities allowed to be queued at one time")
		@Config.Name("Maximum Queued Gibbed Entities")
		public int maximumQueuedGibbedEntities = 20;
	}
	
	public static class Physics {
		
		@Config.Comment("Total lifetime of gibs in ticks")
		@Config.Name("Gib Lifetime")
		@Config.RangeInt(min = 20, max = 2000)
		public int gibLifetime = 320;
		
		@Config.Comment("Lifetime of gibs in ticks once on idle on the ground")
		@Config.Name("Gib Ground Lifetime")
		@Config.RangeInt(min = 20, max = 2000)
		public int gibGroundLifetime = 120;
		
		@Config.Comment("Multiplier to apply to gib collision box width to reduce collisions/size")
		@Config.Name("Gib Collision Width Multiplier")
		@Config.RangeDouble(min = 0.0D, max = 1.0D)
		public double gibCollisionWidthMult = 0.9D;
		
		@Config.Comment("If gibs should be able to push gibs on the ground")
		@Config.Name("Gibs Push Gibs")
		public boolean gibsPushGibs = false;
		
		@Config.Comment("If players should be able to push gibs on the ground")
		@Config.Name("Players Push Gibs")
		public boolean playersPushGibs = true;
		
		@Config.Comment("If other entities should be able to push gibs on the ground")
		@Config.Name("Entities Push Gibs")
		public boolean entitiesPushGibs = false;
	}
	
	public static class Models {
		
		@Config.Comment("Will only gib entities that have had their rendering context captured" + "\n" +
				"Rendering context includes specific scaling of parts and status of optionally rendered parts such as saddles" + "\n" +
				"If disabled, entities that use a custom renderer may be able to be gibbed, however with some visual oddities")
		@Config.Name("Only Gib Context Captured Entities")
		public boolean onlyGibContextCapturedEntities = true;
		
		@Config.Comment("Affects models constructed with proper branching (Finger on Hand, Hand on Arm, etc.)" + "\n" +
				"Limits the largest model branch size by area ratio to total model area that can be retained as a single gib" + "\n" +
				"Lower values will result in more separated gibs, setting to 0 will result in a gib for each model box")
		@Config.Name("Model Max Branch Size Ratio")
		@Config.RangeDouble(min = 0.0D, max = 1.0D)
		public double maxBranchSizeRatio = 0.3D;
		
		@Config.Comment("Affects models constructed with proper branching (Finger on Hand, Hand on Arm, etc.)" + "\n" +
				"Limits the combined model branch sizes by area ratio to total model area" + "\n" +
				"Lower values will limit how many branches can be retained resulting in more separated gibs, setting to 0 will result in a gib for each model box")
		@Config.Name("Model Max Branches Ratio")
		@Config.RangeDouble(min = 0.0D, max = 1.0D)
		public double maxBranchesRatio = 1.0D;
		
		@Config.Comment("Affects models constructed with proper branching (Finger on Hand, Hand on Arm, etc.)" + "\n" +
				"Allows for overriding Max Branch Size Ratio and Max Branches Ratio per Entity Registry Name" + "\n" +
				"Format: Entity Registry Name, Max Branch Size Ratio, Max Branches Ratio" + "\n" +
				"Example: minecraft:witch,0.4,0.8")
		@Config.Name("Model Branch Ratio Overrides")
		public String[] modelBranchRatioOverrides = {
				"iceandfire:dread_scuttler,0.2,1.0",
				"iceandfire:if_cockatrice,0.2,1.0",
				"iceandfire:stymphalianbird,0.1,1.0",
				"iceandfire:deathworm,0.1,1.0",
				"iceandfire:hippocampus,0.2,1.0",
				"iceandfire:amphithere,0.05,1.0",
				"iceandfire:seaserpent,0.11,1.0"
		};
	}
	
	public static class Textures {
		
		@Config.Comment("Instead of rendering gibs using the original entity textures, a custom gore texture will be applied")
		@Config.Name("Use Gore Textures")
		public boolean useGoreTextures = false;
		
		@Config.Comment("If Use Gore Textures is enabled, entities in this list will still have their gibs rendered with the original entity texture")
		@Config.Name("Entity Gore Texture Blacklist")
		public String[] entityGoreTextureBlacklist = { "minecraft:skeleton" };
		
		@Config.Comment("Sets the Entity Gore Texture Blacklist to instead act as a whitelist")
		@Config.Name("Entity Gore Texture Blacklist is Whitelist")
		public boolean entityGoreTextureBlacklistIsWhitelist = false;
		
		@Config.Comment("Blacklist of Entity Registry Names to blacklist from applying captured layer overlay textures to gibs")
		@Config.Name("Entity Layered Texture Blacklist")
		public String[] entityLayeredTextureBlacklist = { "minecraft:mooshroom" };
		
		@Config.Comment("Sets the Entity Layered Texture Blacklist to instead act as a whitelist")
		@Config.Name("Entity Layered Texture Blacklist is Whitelist")
		public boolean layerTextureBlacklistIsWhitelist = false;
	}
	
	public static class Sounds {
		
		@Config.Comment("A custom sound will be played when an entity is gibbed")
		@Config.Name("Play Sound On Gibbed")
		public boolean playSoundOnGibbed = true;
		
		@Config.Comment("Volume to use when playing a sound from an entity being gibbed")
		@Config.Name("Gibbed Sound Volume")
		public double gibbedVolume = 1.0D;
		
		@Config.Comment("Pitch to use when playing a sound from an entity being gibbed")
		@Config.Name("Gibbed Sound Pitch")
		public double gibbedPitch = 0.9D;
		
		@Config.Comment("A custom sound will be played when a gib hits the ground")
		@Config.Name("Play Sound On Gib Land")
		public boolean playSoundOnGibLand = true;
		
		@Config.Comment("Volume to use when playing a sound from a gib hitting the ground")
		@Config.Name("Gib Land Sound Volume")
		public double gibLandVolume = 1.0D;
		
		@Config.Comment("Pitch to use when playing a sound from a gib hitting the ground")
		@Config.Name("Gib Land Sound Pitch")
		public double gibLandPitch = 0.9D;
	}
	
	public static class Particles {
		
		@Config.Comment("Spawns basic blood particles from gibs when an entity is gibbed" + "\n" +
				"If BallisticBlood is loaded, fancier BallisticBlood particles will be used")
		@Config.Name("Spawn Particles On Gibbed")
		public boolean spawnParticlesOnGibbed = true;
		
		@Config.Comment("Spawns basic blood particles from gibs when they hit the ground" + "\n" +
				"If BallisticBlood is loaded, fancier BallisticBlood particles will be used")
		@Config.Name("Spawn Particles On Gib Land")
		public boolean spawnParticlesOnGibLand = true;
		
		@Config.Comment("Forces particles to use the generic BallisticBlood blood type rather than being based on the entity being gibbed" + "\n" +
				"Only affects BallisticBlood particles")
		@Config.Name("Use Generic Blood Type")
		public boolean useGenericBloodType = false;
		
		@Config.Comment("Scales the amount of particles attempted to be created based on the entities max health and amount of gibs created" + "\n" +
				"If BallisticBlood is loaded, BallisticBlood config values can affect the amount of particles actually created based on the computed value")
		@Config.Name("Particle Amount Scale")
		public float particleAmountScale = 1.0F;
	}
	
	private static Set<ResourceLocation> entityGibBlacklist = null;
	private static Set<String> entityGibModidBlacklist = null;
	private static Set<ResourceLocation> goreTextureBlacklist = null;
	private static Set<ResourceLocation> layerTextureBlacklist = null;
	private static Map<ResourceLocation,Pair<Float,Float>> entityBranchRatioOverrides = null;
	
	public static boolean shouldEntityBeGibbed(EntityLivingBase entity, boolean explosion) {
		if(entity == null || !entity.world.isRemote) return false;
		if(entity instanceof EntityPlayer) {
			return entity.getRNG().nextFloat() < GENERAL.playerGibChance;
			//if(entity == Minecraft.getMinecraft().player && Minecraft.getMinecraft().gameSettings.thirdPersonView == 0) return false;
		}
		if(entityGibBlacklist == null || entityGibModidBlacklist == null) createEntityGibBlacklist();
		ResourceLocation loc = EntityList.getKey(entity);
		if(loc == null) return false;
		if(GENERAL.entityGibBlacklistIsWhitelist == (entityGibModidBlacklist.contains(loc.getNamespace()) || entityGibBlacklist.contains(loc))) {
			if(explosion && GENERAL.explosionsAlwaysGib) return true;
			return entity.getRNG().nextFloat() < GENERAL.entityGibChance;
		}
		return false;
	}
	
	private static void createEntityGibBlacklist() {
		entityGibBlacklist = new HashSet<>();
		entityGibModidBlacklist = new HashSet<>();
		for(String name : GENERAL.entityGibBlacklist) {
			name = name.trim();
			if(name.isEmpty()) continue;
			String[] split = name.split(":");
			if(split.length == 2 && split[1].equals("*")) entityGibModidBlacklist.add(split[0]);
			else entityGibBlacklist.add(new ResourceLocation(name));
		}
	}
	
	public static boolean shouldGoreTextureBeUsed(EntityLivingBase entity) {
		if(entity == null || !entity.world.isRemote) return false;
		if(!TEXTURES.useGoreTextures) return false;
		if(goreTextureBlacklist == null) createGoreTextureBlacklist();
		if(goreTextureBlacklist.contains(EntityList.getKey(entity)) == TEXTURES.entityGoreTextureBlacklistIsWhitelist) {
			return true;
		}
		return false;
	}
	
	private static void createGoreTextureBlacklist() {
		goreTextureBlacklist = new HashSet<>();
		for(String name : TEXTURES.entityGoreTextureBlacklist) {
			name = name.trim();
			if(name.isEmpty()) continue;
			goreTextureBlacklist.add(new ResourceLocation(name));
		}
	}
	
	public static boolean shouldLayerTextureBeUsed(EntityLivingBase entity) {
		if(entity == null || !entity.world.isRemote) return false;
		if(layerTextureBlacklist == null) createLayerTextureBlacklist();
		if(layerTextureBlacklist.contains(EntityList.getKey(entity)) == TEXTURES.layerTextureBlacklistIsWhitelist) {
			return true;
		}
		return false;
	}
	
	private static void createLayerTextureBlacklist() {
		layerTextureBlacklist = new HashSet<>();
		for(String name : TEXTURES.entityLayeredTextureBlacklist) {
			name = name.trim();
			if(name.isEmpty()) continue;
			layerTextureBlacklist.add(new ResourceLocation(name));
		}
	}
	
	@Nullable
	public static Pair<Float,Float> getEntityBranchRatioOverride(EntityLivingBase entity) {
		if(entityBranchRatioOverrides == null) createEntityBranchRatioOverrides();
		return entityBranchRatioOverrides.get(EntityList.getKey(entity));
	}
	
	private static void createEntityBranchRatioOverrides() {
		entityBranchRatioOverrides = new HashMap<>();
		for(String entry : MODELS.modelBranchRatioOverrides) {
			String[] entries = entry.split(",");
			if(entries.length != 3) continue;
			String name = entries[0].trim();
			if(name.isEmpty()) continue;
			float maxSizeRatio = (float)MODELS.maxBranchSizeRatio;
			try {
				maxSizeRatio = Float.parseFloat(entries[1].trim());
			}
			catch(Exception ignored) { }
			float maxBranchRatio = (float)MODELS.maxBranchesRatio;
			try {
				maxBranchRatio = Float.parseFloat(entries[2].trim());
			}
			catch(Exception ignored) { }
			entityBranchRatioOverrides.put(new ResourceLocation(name), new Pair<>(maxSizeRatio, maxBranchRatio));
		}
	}
	
	@Mod.EventBusSubscriber(modid = Gibbed.MODID, value = Side.CLIENT)
	public static class EventHandler {
		
		@SubscribeEvent
		public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
			if(event.getModID().equals(Gibbed.MODID)) {
				ConfigManager.sync(Gibbed.MODID, Config.Type.INSTANCE);
				entityGibBlacklist = null;
				entityGibModidBlacklist = null;
				layerTextureBlacklist = null;
				entityBranchRatioOverrides = null;
				GibGenerator.resetCache();
			}
		}
	}
}