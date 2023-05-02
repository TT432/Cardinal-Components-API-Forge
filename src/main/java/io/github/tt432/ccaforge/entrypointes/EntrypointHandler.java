package io.github.tt432.ccaforge.entrypointes;

import dev.onyxstudios.cca.internal.base.asm.StaticComponentLoadingException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IConfigurable;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author DustW
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EntrypointHandler {
    private static boolean init;
    private static final Map<String, List<EntrypointContainer<Object>>> map = new HashMap<>();

    public static void loadAll() {
        if (!init) {
            ModList.get().forEachModFile(mf -> {
                IConfigurable config = mf.getModFileInfo().getConfig();
                List<? extends IConfigurable> mods = config.getConfigList("mods");
                IConfigurable modEntry = mods.get(0);
                Object modName = modEntry.getConfigElement("displayName").orElse("<unnamed>");
                Object modId = modEntry.getConfigElement("modId").orElse("<unnamed>");

                config.<Map<String, List<Object>>>getConfigElement("entrypoints")
                        .orElse(new HashMap<>())
                        .forEach((k, v) -> map.put(k, v.stream().map(Object::toString)
                                .map(s -> {
                                    try {
                                        return Class.forName(s).getDeclaredConstructor().newInstance();
                                    } catch (InstantiationException | IllegalAccessException |
                                             InvocationTargetException | NoSuchMethodException |
                                             ClassNotFoundException e) {
                                        throw new StaticComponentLoadingException(
                                                "can't load %s by mod %s (%s)".formatted(s, modName, modId), e);
                                    }
                                })
                                .map(o -> new EntrypointContainer<Object>(o, mf)).toList()));
            });
        }
    }

    /**
     * Try to get all EntrypointContainer with key as key and convert to type
     *
     * @param key  key
     * @param type witch cast
     * @param <T>  t
     * @return list of container
     */
    public static <T> List<EntrypointContainer<T>> getEntrypointContainers(String key, Class<T> type) {
        loadAll();

        return map.computeIfAbsent(key, s -> new ArrayList<>()).stream().map(ec -> {
            try {
                type.cast(ec.entrypoint());
                return (EntrypointContainer<T>) ec;
            } catch (ClassCastException e) {
                throw new RuntimeException(e);
            }
        }).toList();
    }
}
