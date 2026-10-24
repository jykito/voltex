package com.jykito.industrialcore.api;

/**
 * Marker for armour drawn with the Voltex power suit model ({@code client.armor.PowerSuitModel}), like the Nexite and
 * Plasma suits. For such armour Voltex hides the player's head and hat layer under the helmet (the helmet is smaller
 * than the head cube) and draws the suit arm in first person with the chestplate's armour texture.
 * <p>
 * The item itself returns {@code PowerSuitModel.forSlot(slot)} from its client extensions and its own texture from
 * {@code getArmorTexture}. Energy shows in the Voltex armour HUD by itself for any armour with the energy capability.
 */
public interface PowerSuitArmor {
}
