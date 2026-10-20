package fonnymunkey.gibbed.util;

import fonnymunkey.gibbed.platform.Services;

public abstract class ModLoadedUtil {
	public static final String CITADEL_MODID = "citadel";
	public static final String GECKOLIB_MODID = "geckolib";
	public static final String AZURELIB_MODID = "azurelib";
	public static final String URANUS_MODID = "uranus";
	
	private static Boolean CITADEL_LOADED = null;
	private static Boolean GECKOLIB_LOADED = null;
	private static Boolean AZURELIB_LOADED = null;
	private static Boolean URANUS_LOADED = null;
	
	public static boolean isCitadelLoaded() {
		if(CITADEL_LOADED == null) CITADEL_LOADED = Services.PLATFORM.isModLoaded(CITADEL_MODID);
		return CITADEL_LOADED;
	}
	
	public static boolean isGeckoLibLoaded() {
		if(GECKOLIB_LOADED == null) GECKOLIB_LOADED = Services.PLATFORM.isModLoaded(GECKOLIB_MODID);
		return GECKOLIB_LOADED;
	}

	public static boolean isAzureLibLoaded() {
		if(AZURELIB_LOADED == null) AZURELIB_LOADED = Services.PLATFORM.isModLoaded(AZURELIB_MODID);
		return AZURELIB_LOADED;
	}

	public static boolean isUranusLoaded() {
		if(URANUS_LOADED == null) URANUS_LOADED = Services.PLATFORM.isModLoaded(URANUS_MODID);
		return URANUS_LOADED;
	}
}
