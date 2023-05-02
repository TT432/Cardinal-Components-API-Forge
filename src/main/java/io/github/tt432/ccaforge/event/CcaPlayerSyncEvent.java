package io.github.tt432.ccaforge.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

/**
 * @author DustW
 */
public class CcaPlayerSyncEvent extends Event {
    public final ServerPlayer player;

    public CcaPlayerSyncEvent(ServerPlayer player) {
        this.player = player;
    }
}
