package io.github.tt432.ccaforge.util;

import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.entity.Entity;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author DustW
 */
public class Players {
    public static Collection<ServerPlayer> tracking(Entity entity) {
        Objects.requireNonNull(entity, "Entity cannot be null");
        var manager = entity.level.getChunkSource();

        if (manager instanceof ServerChunkCache scc) {
            var storage = scc.chunkMap;
            var tracker = storage.entityMap.get(entity.getId());

            // return an immutable collection to guard against accidental removals.
            if (tracker != null) {
                return tracker.seenBy.stream()
                        .map(ServerPlayerConnection::getPlayer)
                        .collect(Collectors.toUnmodifiableSet());
            }

            return Collections.emptySet();
        }

        throw new IllegalArgumentException("Only supported on server worlds!");
    }
}
