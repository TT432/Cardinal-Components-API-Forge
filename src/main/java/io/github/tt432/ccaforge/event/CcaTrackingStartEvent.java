package io.github.tt432.ccaforge.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.eventbus.api.Event;

/**
 * @author DustW
 */
public class CcaTrackingStartEvent extends Event {
    public final ServerPlayer player;
    public final Entity entity;

    public CcaTrackingStartEvent(ServerPlayer player, Entity entity) {
        this.player = player;
        this.entity = entity;
    }
}
