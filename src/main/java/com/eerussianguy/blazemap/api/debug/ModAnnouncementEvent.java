package com.eerussianguy.blazemap.api.debug;

import java.util.Set;

import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.ModList;

/**
 * Used to let Blaze Map know that a certain mod that interacts with it is loaded.
 * Doing this will display the mod name and version on the Debug UI.
 *
 * Not all mods need to do this:
 * - Blaze Map automatically looks for mods it has built-in support for
 * - Blaze Map automatically looks for mods known to break things (like Optifine)
 * - Blaze Map will automatically list any mod that registers an object in BlazeMapAPI
 *
 * Other mods however (that just use events and other stuff) should announce themselves.
 * This will save everyone time and pain when users report issues. Thanks <3
 *
 * @author LordFokas
 */
public class ModAnnouncementEvent extends Event {
    private final Set<String> mods;

    public ModAnnouncementEvent(Set<String> mods){
        this.mods = mods;
    }

    public void announce(String id){
        if(ModList.get().getModContainerById(id).isEmpty()){
            throw new IllegalArgumentException("Mod ID must be that of a currently loaded mod!");
        }
        mods.add(id);
    }
}
