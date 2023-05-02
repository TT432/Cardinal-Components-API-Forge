package io.github.tt432.ccaforge.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * @author DustW
 */
@Mod.EventBusSubscriber
public class CcaEventHandler {
    @SubscribeEvent
    public static void onEvent(PlayerEvent.StartTracking event) {
        MinecraftForge.EVENT_BUS.post(new CcaTrackingStartEvent((ServerPlayer) event.getEntity(), event.getTarget()));
    }

    @SubscribeEvent
    public static void onEvent(PlayerEvent.Clone event) {
        MinecraftForge.EVENT_BUS.post(new CcaPlayerCopyEvent(
                (ServerPlayer) event.getOriginal(), (ServerPlayer) event.getEntity(), !event.isWasDeath()));
    }
}
