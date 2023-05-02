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
package dev.onyxstudios.cca.internal.entity;

import dev.onyxstudios.cca.api.v3.component.Component;
import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentProvider;
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import dev.onyxstudios.cca.api.v3.entity.RespawnCopyStrategy;
import io.github.tt432.ccaforge.event.CcaPlayerCopyEvent;
import io.github.tt432.ccaforge.event.CcaPlayerSyncEvent;
import io.github.tt432.ccaforge.event.CcaTrackingStartEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.common.MinecraftForge;

import java.util.Set;

public final class CardinalComponentsEntity {
    /**
     * {@link ClientboundCustomPayloadPacket} channel for default entity component synchronization.
     *
     * <p> Packets emitted on this channel must begin with, in order, the {@link Entity#getId() entity id} (as an int),
     * and the {@link ComponentKey#getId() component's type} (as an Identifier).
     *
     * <p> Components synchronized through this channel will have {@linkplain AutoSyncedComponent#applySyncPacket(FriendlyByteBuf)}
     * called on the game thread.
     */
    public static final ResourceLocation PACKET_ID = new ResourceLocation("cardinal-components", "entity_sync");

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(CardinalComponentsEntity::playerSyncHandler);
        MinecraftForge.EVENT_BUS.addListener(CardinalComponentsEntity::trackingHandler);
        MinecraftForge.EVENT_BUS.addListener(CardinalComponentsEntity::copyDataHandler);
    }

    private static void copyDataHandler(CcaPlayerCopyEvent event) {
        copyData(event.original, event.clone, event.lossless);
    }

    private static void playerSyncHandler(CcaPlayerSyncEvent event) {
        syncEntityComponents(event.player, event.player);
    }

    private static void trackingHandler(CcaTrackingStartEvent event) {
        syncEntityComponents(event.player, event.entity);
    }

    private static void copyData(ServerPlayer original, ServerPlayer clone, boolean lossless) {
        boolean keepInventory = original.level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY) || clone.isSpectator();
        Set<ComponentKey<?>> keys = ((ComponentProvider) original).getComponentContainer().keys();

        for (ComponentKey<?> key : keys) {
            copyData(original, clone, lossless, keepInventory, key, !((SwitchablePlayerEntity) original).cca$isSwitchingCharacter());
        }
    }

    private static <C extends Component> void copyData(ServerPlayer original, ServerPlayer clone, boolean lossless, boolean keepInventory, ComponentKey<C> key, boolean sameCharacter) {
        C from = key.get(original);
        C to = key.get(clone);
        RespawnCopyStrategy.get(key).copyForRespawn(from, to, lossless, keepInventory, sameCharacter);
    }

    private static void syncEntityComponents(ServerPlayer player, Entity tracked) {
        ComponentProvider provider = (ComponentProvider) tracked;

        for (ComponentKey<?> key : provider.getComponentContainer().keys()) {
            key.syncWith(player, provider);
        }
    }
}
