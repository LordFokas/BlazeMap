package com.eerussianguy.blazemap.api.event;

import net.minecraftforge.eventbus.api.Event;

/**
 * Fired immediately after all registries have been frozen.
 * Used by systems that need to be sure the registries will no longer be changed.
 *
 * @author LordFokas
 */
public class BlazeRegistriesFrozenEvent extends Event {}
