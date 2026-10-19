package fonnymunkey.gibbed.config;

import fonnymunkey.gibbed.Constants;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.PartitioningSerializer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Config(name = Constants.MOD_ID)
public class GibbedConfig extends PartitioningSerializer.GlobalData {

	@ConfigEntry.Category("GENERAL")
	@ConfigEntry.Gui.TransitiveObject
	public General GENERAL = new General();

	@ConfigEntry.Category("PHYSICS")
	@ConfigEntry.Gui.TransitiveObject
	public Physics PHYSICS = new Physics();

	@ConfigEntry.Category("MODELS")
	@ConfigEntry.Gui.TransitiveObject
	public Models MODELS = new Models();

	@ConfigEntry.Category("TEXTURES")
	@ConfigEntry.Gui.TransitiveObject
	public Textures TEXTURES = new Textures();

	@ConfigEntry.Category("SOUNDS")
	@ConfigEntry.Gui.TransitiveObject
	public Sounds SOUNDS = new Sounds();

	@ConfigEntry.Category("PARTICLES")
	@ConfigEntry.Gui.TransitiveObject
	public Particles PARTICLES = new Particles();

	@Config(name = "general")
	public static class General implements ConfigData {

		@ConfigEntry.Gui.Tooltip
		@ConfigEntry.BoundedDiscrete(min = 0, max = 100)
		public int playerGibChance = 100;

		@ConfigEntry.Gui.Tooltip
		@ConfigEntry.BoundedDiscrete(min = 0, max = 100)
		public int entityGibChance = 100;

		@ConfigEntry.Gui.Tooltip
		@ConfigEntry.BoundedDiscrete(min = 0, max = 100)
		public int perGibSpawnChance = 100;

		@ConfigEntry.Gui.Tooltip
		public boolean explosionsAlwaysGib = true;

		@ConfigEntry.Gui.Tooltip
		public boolean explosionsSpawnAllGibs = true;

		@ConfigEntry.Gui.Tooltip
		public List<String> entityGibBlacklist = new ArrayList<>(Arrays.asList(
				"minecraft:slime",
				"minecraft:magma_cube",
				"minecraft:ender_dragon"
		));

		@ConfigEntry.Gui.Tooltip
		public boolean entityGibBlacklistIsWhitelist = false;

		@ConfigEntry.Gui.Tooltip
		@ConfigEntry.BoundedDiscrete(min = 1, max = 200)
		public int maximumQueuedGibbedEntities = 20;

		private General() {}
	}

	@Config(name = "physics")
	public static class Physics implements ConfigData {

		@ConfigEntry.Gui.Tooltip
		@ConfigEntry.BoundedDiscrete(min = 20, max = 2000)
		public int gibLifetime = 320;

		@ConfigEntry.Gui.Tooltip
		@ConfigEntry.BoundedDiscrete(min = 20, max = 2000)
		public int gibGroundLifetime = 120;

		@ConfigEntry.Gui.Tooltip
		public double gibCollisionWidthMult = 0.9D;

		@ConfigEntry.Gui.Tooltip
		public boolean gibsPushGibs = false;

		@ConfigEntry.Gui.Tooltip
		public boolean playersPushGibs = true;

		@ConfigEntry.Gui.Tooltip
		public boolean entitiesPushGibs = false;

		private Physics() {}
	}

	@Config(name = "models")
	public static class Models implements ConfigData {

		@ConfigEntry.Gui.Tooltip(count = 3)
		public boolean onlyGibContextCapturedEntities = true;

		@ConfigEntry.Gui.Tooltip(count = 2)
		public double maxBranchSizeRatio = 0.3D;

		@ConfigEntry.Gui.Tooltip(count = 2)
		public double maxBranchesRatio = 1.0D;

		@ConfigEntry.Gui.Tooltip(count = 3)
		public List<String> modelBranchRatioOverrides = new ArrayList<>();

		private Models() {}
	}

	@Config(name = "textures")
	public static class Textures implements ConfigData {

		@ConfigEntry.Gui.Tooltip
		public boolean useGoreTextures = false;

		@ConfigEntry.Gui.Tooltip
		public List<String> entityGoreTextureBlacklist = new ArrayList<>(Arrays.asList(
				"minecraft:skeleton"
		));

		@ConfigEntry.Gui.Tooltip
		public boolean entityGoreTextureBlacklistIsWhitelist = false;

		@ConfigEntry.Gui.Tooltip
		public List<String> entityLayeredTextureBlacklist = new ArrayList<>(Arrays.asList(
				"minecraft:mooshroom"
		));

		@ConfigEntry.Gui.Tooltip
		public boolean layerTextureBlacklistIsWhitelist = false;

		private Textures() {}
	}

	@Config(name = "sounds")
	public static class Sounds implements ConfigData {

		@ConfigEntry.Gui.Tooltip
		public boolean playSoundOnGibbed = true;

		@ConfigEntry.Gui.Tooltip
		public double gibbedVolume = 1.0D;

		@ConfigEntry.Gui.Tooltip
		public double gibbedPitch = 0.9D;

		@ConfigEntry.Gui.Tooltip
		public boolean playSoundOnGibLand = true;

		@ConfigEntry.Gui.Tooltip
		public double gibLandVolume = 1.0D;

		@ConfigEntry.Gui.Tooltip
		public double gibLandPitch = 0.9D;

		private Sounds() {}
	}

	@Config(name = "particles")
	public static class Particles implements ConfigData {

		@ConfigEntry.Gui.Tooltip
		public boolean spawnParticlesOnGibbed = true;

		@ConfigEntry.Gui.Tooltip
		public boolean spawnParticlesOnGibLand = true;

		@ConfigEntry.Gui.Tooltip
		public double particleAmountScale = 1.0D;

		private Particles() {}
	}
}