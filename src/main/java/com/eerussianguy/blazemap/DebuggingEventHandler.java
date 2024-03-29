package com.eerussianguy.blazemap;

import net.minecraft.world.level.Level;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.event.world.PistonEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

// This class solely exists for debugging purposes. It should not be initialised in the
// final build.
public class DebuggingEventHandler {

    public static void init() {
        final IEventBus forgeEventBus = MinecraftForge.EVENT_BUS;
        final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        forgeEventBus.addListener(DebuggingEventHandler::logLogin);
        forgeEventBus.addListener(DebuggingEventHandler::logRawMouse);
        // forgeEventBus.addListener(DebuggingEventHandler::logMouseInput);
        // forgeEventBus.addListener(DebuggingEventHandler::logKeyboardInput);

        modEventBus.addListener(DebuggingEventHandler::logCommonSetup);
        modEventBus.addListener(DebuggingEventHandler::logClientSetup);
        modEventBus.addListener(DebuggingEventHandler::logDedicatedServerSetup);
        modEventBus.addListener(DebuggingEventHandler::logInterModEnqueueSetup);
        modEventBus.addListener(DebuggingEventHandler::logInterModProcessSetup);
        
        forgeEventBus.addListener(DebuggingEventHandler::logBlockChange);
        forgeEventBus.addListener(DebuggingEventHandler::logExplosion);
    }

    private static void logLogin(ClientPlayerNetworkEvent.LoggedInEvent event) {
        BlazeMap.LOGGER.info("999 Captured login event: {}", event.getPlayer());
    }


    // Keys and clicks
    private static void logRawMouse(InputEvent.RawMouseEvent event) {
        BlazeMap.LOGGER.info("000 Captured raw mouse: {} {} {}", event.getButton(), event.getAction(), event.getModifiers());
    }

    private static void logMouseInput(InputEvent.MouseInputEvent event) {
        BlazeMap.LOGGER.info("000 Captured mouse input: {} {} {}", event.getButton(), event.getAction(), event.getModifiers());
    }
    
    private static void logKeyboardInput(InputEvent.KeyInputEvent event) {
        BlazeMap.LOGGER.info("000 Captured keyboard input: {} {} {}", event.getKey(), event.getAction(), event.getModifiers());
    }


    // Setup
    private static void logCommonSetup(FMLCommonSetupEvent event) {
        BlazeMap.LOGGER.info("111 Common Setup");
    }

    private static void logClientSetup(FMLClientSetupEvent event) {
        BlazeMap.LOGGER.info("222 Client Setup");
    }

    private static void logDedicatedServerSetup(FMLDedicatedServerSetupEvent event) {
        BlazeMap.LOGGER.info("222 Server Setup");
    }

    private static void logInterModEnqueueSetup(InterModEnqueueEvent event) {
        BlazeMap.LOGGER.info("333 Inter Mod Enqueue Setup");
    }

    private static void logInterModProcessSetup(InterModProcessEvent event) {
        BlazeMap.LOGGER.info("444 Inter Mod Process Setup");
    }


    // World loading
    private static void logBlockChange(BlockEvent event) {
        if (!(event instanceof BlockEvent.CropGrowEvent.Pre || event instanceof BlockEvent.NeighborNotifyEvent || event instanceof PistonEvent.Post)) {
            Level currentLevel = (Level)event.getWorld();
            BlazeMap.LOGGER.info("555 {}: {} {} {}, Chunk: {}", event.getClass().getName(), event.getState(), event.getPos(), currentLevel.dimension(), currentLevel.getChunk(event.getPos()).getPos());
        }
    }

    private static void logExplosion(ExplosionEvent.Detonate event) {
        BlazeMap.LOGGER.info("555 {}: {} {} {}, Chunk: {}", event.getClass().getName(), event.getExplosion().getExploder(), event.getExplosion().getPosition(), event.getWorld().dimension());
    }

}
