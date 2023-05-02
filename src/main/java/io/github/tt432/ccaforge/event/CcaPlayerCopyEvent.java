package io.github.tt432.ccaforge.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

/**
 * Copy mod data from a player to its clone.
 *
 * <p> When using an end portal from the End to the Overworld, a lossless copy will be made.
 * In such cases, all data should be copied exactly between the original and the clone.
 * Some data may also need exact copying when the {@link net.minecraft.world.level.GameRules#RULE_KEEPINVENTORY keepInventory gamerule}
 * is enabled, which needs to be checked independently using {@code clone.world.getGameRules()}.
 * Otherwise, it is safe to simply ignore any data that should be reset with each death.
 *
 * <p> The {@code clone} is usually repositioned after this method has been called,
 * invalidating position and dimension changes. The dimension of the {@code original}
 * player itself has also been invalidated before this method is called, but is accessible
 * through {@code original.world.dimension}.
 */
public class CcaPlayerCopyEvent extends Event {
    /**
     * the player that is being cloned
     */
    public final ServerPlayer original;
    /**
     * the clone of the {@code original} player
     */
    public final ServerPlayer clone;
    /**
     * {@code true} if all the data should be copied exactly, {@code false} otherwise.
     */
    public final boolean lossless;

    public CcaPlayerCopyEvent(ServerPlayer original, ServerPlayer clone, boolean lossless) {
        this.original = original;
        this.clone = clone;
        this.lossless = lossless;
    }
}
