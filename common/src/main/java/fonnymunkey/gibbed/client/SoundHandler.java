package fonnymunkey.gibbed.client;

import fonnymunkey.gibbed.Constants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import java.time.LocalDate;

public class SoundHandler {
	public static final SoundEvent GIBBED = createSound("gibbed");
	public static final SoundEvent GIBBED_SPECIAL = createSound("gibbed_special");
	public static final SoundEvent GIBBED_LAND = createSound("gibbed_land");
	public static final SoundEvent GIBBED_SPECIAL_LAND = createSound("gibbed_special_land");
	private static Boolean isApril1 = null;
	
	private static SoundEvent createSound(String name) {
		return SoundEvent.createVariableRangeEvent(new ResourceLocation(Constants.MOD_ID, name));
	}
	
	public static SoundEvent getGibbedSound() {
		if(isApril1 == null) isApril1 = LocalDate.now().withYear(2025).equals(LocalDate.of(2025, 4, 1));
		return isApril1 ? GIBBED_SPECIAL : GIBBED;
	}
	
	public static SoundEvent getGibLandSound() {
		if(isApril1 == null) isApril1 = LocalDate.now().withYear(2025).equals(LocalDate.of(2025, 4, 1));
		return isApril1 ? GIBBED_SPECIAL_LAND : GIBBED_LAND;
	}
}