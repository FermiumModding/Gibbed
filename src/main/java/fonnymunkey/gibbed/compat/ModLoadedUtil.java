package fonnymunkey.gibbed.compat;

import net.minecraftforge.fml.common.Loader;

public abstract class ModLoadedUtil {
	private static final String BALLISTICBLOOD_MODID = "ballisticblood";
	private static final String GECKOLIB_MODID = "geckolib3";
	private static final String LYCANITES_MODID = "lycanitesmobs";
	private static final String MOBENDS_MODID = "mobends";
	
	private static Boolean BALLISTICBLOOD_LOADED = null;
	private static Boolean GECKOLIB_LOADED = null;
	private static Boolean LYCANITES_LOADED = null;
	private static Boolean MOBENDS_LOADED = null;
	
	public static boolean isBallisticBloodLoaded() {
		if(BALLISTICBLOOD_LOADED == null) BALLISTICBLOOD_LOADED = Loader.isModLoaded(BALLISTICBLOOD_MODID);
		return BALLISTICBLOOD_LOADED;
	}
	
	public static boolean isGeckolibLoaded() {
		if(GECKOLIB_LOADED == null) GECKOLIB_LOADED = Loader.isModLoaded(GECKOLIB_MODID);
		return GECKOLIB_LOADED;
	}
	
	public static boolean isLycanitesLoaded() {
		if(LYCANITES_LOADED == null) LYCANITES_LOADED = Loader.isModLoaded(LYCANITES_MODID);
		return LYCANITES_LOADED;
	}
	
	public static boolean isMobendsLoaded() {
		if(MOBENDS_LOADED == null) MOBENDS_LOADED = Loader.isModLoaded(MOBENDS_MODID);
		return MOBENDS_LOADED;
	}
}
