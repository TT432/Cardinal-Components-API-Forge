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

import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistry;
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import dev.onyxstudios.cca.internal.base.ComponentsInternals;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public final class CcaEntityClientNw {
    public static void handlerPacket(FriendlyByteBuf buffer) {
        Minecraft client = Minecraft.getInstance();

        try {
            int entityId = buffer.readInt();
            ResourceLocation componentTypeId = buffer.readResourceLocation();
            ComponentKey<?> componentType = ComponentRegistry.get(componentTypeId);
            if (componentType == null) {
                return;
            }
            FriendlyByteBuf copy = new FriendlyByteBuf(buffer.copy());
            client.execute(() -> {
                try {
                    componentType.maybeGet(Objects.requireNonNull(client.level).getEntity(entityId))
                            .filter(c -> c instanceof AutoSyncedComponent)
                            .ifPresent(c -> ((AutoSyncedComponent) c).applySyncPacket(copy));
                } finally {
                    copy.release();
                }
            });
        } catch (Exception e) {
            ComponentsInternals.LOGGER.error("Error while reading entity components from network", e);
            throw e;
        }
    }
}
