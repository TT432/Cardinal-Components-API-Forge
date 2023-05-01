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
package dev.onyxstudios.cca.internal.base;

import dev.onyxstudios.cca.api.v3.component.ComponentRegistry;
import dev.onyxstudios.cca.internal.base.asm.StaticComponentLoadingException;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;

public final class ComponentsInternals {
    public static final Logger LOGGER = LogManager.getLogger("Cardinal Components API");
    private static boolean logDeserializationWarnings = true;

    public static void init() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("cardinal-components-api.properties");
        try(Reader reader = Files.newBufferedReader(path)) {
            Properties cfg = new Properties();
            cfg.load(reader);
            logDeserializationWarnings = Boolean.parseBoolean(cfg.getProperty("log-deserialization-warnings", "true"));
        } catch (IOException e) {
            try {
                Files.writeString(path, """
                    # If set to false, warnings will not get logged when a component fails to be resolved (typically due to mods being removed)
                    # Default value: true
                    log-deserialization-warnings = true

                    # Internal value, do not edit or your changes may be arbitrarily reset
                    config-version = 1
                    """);
            } catch (IOException ex) {
                LOGGER.error("Failed to write config file at {}", path);
            }
        }
    }

    @Nonnull
    public static <R> R createFactory(Class<R> factoryClass) {
        try {
            Constructor<?>[] constructors = factoryClass.getConstructors();
            if (constructors.length != 1) {
                throw new IllegalArgumentException("Expected 1 constructor declaration in " + factoryClass + ", got " + Arrays.toString(constructors));
            }
            @SuppressWarnings("unchecked") R ret =
                (R) constructors[0].newInstance();
            return ret;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new StaticComponentLoadingException("Failed to instantiate generated component factory", e);
        }
    }

    public static void logDeserializationWarnings(Collection<String> missedKeyIds) {
        if (logDeserializationWarnings) {
            for (String missedKeyId : missedKeyIds) {
                ResourceLocation id = ResourceLocation.tryParse(missedKeyId);
                String cause;
                if (id == null) cause = "invalid identifier";
                else if (ComponentRegistry.get(id) == null) cause = "unregistered key";
                else cause = "provider does not have ";
                LOGGER.warn("Failed to deserialize component: {} {}", cause, missedKeyId);
            }
        }
    }
}
