package fonnymunkey.gibbed.util;

import fonnymunkey.gibbed.platform.Services;

public abstract class ModLoadedUtil {
	public static final String CITADEL_MODID = "citadel";
	
	private static Boolean CITADEL_LOADED = null;
	
	public static boolean isCitadelLoaded() {
		if(CITADEL_LOADED == null) CITADEL_LOADED = Services.PLATFORM.isModLoaded(CITADEL_MODID);
		return CITADEL_LOADED;
	}
}
