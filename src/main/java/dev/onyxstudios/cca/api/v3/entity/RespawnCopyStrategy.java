/*
 * Cardinal-Components-API
 * Copyright (C) 2019-2023 OnyxStudios
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
 * OR OTHER DEALINGS IN THE SOFTWARE.
 */
package dev.onyxstudios.cca.api.v3.entity;

import dev.onyxstudios.cca.api.v3.component.Component;
import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.CopyableComponent;
import dev.onyxstudios.cca.internal.entity.CardinalEntityInternals;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.GameRules;

/**
 * Represents a strategy to copy a component from a player to another.
 *
 * <p>Copy strategies can be registered using methods on {@link EntityComponentFactoryRegistry}.
 *
 * @param <C> the type of components handled by this strategy
 * @see io.github.tt432.ccaforge.event.CcaPlayerCopyEvent
 */
@FunctionalInterface
public interface RespawnCopyStrategy<C extends Component> {

    /**
     * Always copy a component no matter the cause of respawn.
     *
     * <p>This strategy is relevant for persistent metadata such as statistics, or knowledge the player
     * cannot lose.
     */
    RespawnCopyStrategy<Component> ALWAYS_COPY = (from, to, lossless, keepInventory, sameCharacter) -> copy(from, to);

    /**
     * Always copy a component, unless the player is switching to another character.
     *
     * <p>In vanilla minecraft, this copy strategy is effectively the same as {@link #ALWAYS_COPY}.
     * The difference becomes apparent with mods that let players have multiple bodies, or take
     * over the body of another player.
     */
    RespawnCopyStrategy<Component> CHARACTER = (from, to, lossless, keepInventory, sameCharacter) -> {
        if (sameCharacter) {
            copy(from, to);
        }
    };

    /**
     * Copy a component whenever the player's inventory would be copied.
     *
     * <p>This strategy is relevant for any data storage tied to items or experience.
     */
    RespawnCopyStrategy<Component> INVENTORY = (from, to, lossless, keepInventory, sameCharacter) -> {
        if (lossless || keepInventory) {
            copy(from, to);
        }
    };

    /**
     * Copy a component only when the entire data is transferred from a player to the other (eg. return from the End).
     *
     * <p>This strategy is the default.
     */
    RespawnCopyStrategy<Component> LOSSLESS_ONLY = (from, to, lossless, keepInventory, sameCharacter) -> {
        if (lossless) {
            copy(from, to);
        }
    };

    /**
     * Never copy a component no matter the cause of respawn.
     *
     * <p>This strategy can be used when {@code RespawnCopyStrategy} does not offer enough context,
     * in which case {@link io.github.tt432.ccaforge.event.CcaPlayerCopyEvent} may be used directly.
     */
    RespawnCopyStrategy<Component> NEVER_COPY = (from, to, lossless, keepInventory, sameCharacter) -> { };

    static <C extends Component> RespawnCopyStrategy<? super C> get(ComponentKey<C> key) {
        return CardinalEntityInternals.getRespawnCopyStrategy(key);
    }

    /**
     * Copies data from one component to the other.
     *
     * <p> If {@code to} implements {@link CopyableComponent}, its {@link CopyableComponent#copyFrom(Component)}
     * method will be called, otherwise data will be copied using NBT serialization.
     *
     * @param from the component to copy data from
     * @param to   the component to copy data to
     * @param <C>  the common component type
     */
    static <C extends Component> void copy(C from, C to) {
        if (to instanceof CopyableComponent) {
            CardinalEntityInternals.copyAsCopyable(from, (CopyableComponent<?>) to);
        } else {
            CompoundTag tag = new CompoundTag();
            from.writeToNbt(tag);
            to.readFromNbt(tag);
        }
    }

    /**
     * Copy data from a component to another as part of a player respawn.
     *
     * @param from          the component to copy data from
     * @param to            the component to copy data to
     * @param lossless      {@code true} if the player is copied exactly, such as when coming back from the End
     * @param keepInventory {@code true} if the player's inventory and XP are kept, such as when
     *                      {@link GameRules#RULE_KEEPINVENTORY} is enabled or the player is in spectator mode
     * @param sameCharacter {@code true} if the player is not switching to an unrelated body.
     *                      Can only be {@code false} with other mods installed.
     */
    void copyForRespawn(C from, C to, boolean lossless, boolean keepInventory, boolean sameCharacter);
}
