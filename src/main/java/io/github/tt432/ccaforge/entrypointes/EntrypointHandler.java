package io.github.tt432.ccaforge.entrypointes;

import dev.onyxstudios.cca.internal.base.asm.StaticComponentLoadingException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IConfigurable;
import org.apache.commons.compress.utils.Lists;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author DustW
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EntrypointHandler {
    private static final Map<String, List<EntrypointContainer<?>>> map = new HashMap<>();

    public static void loadAll() {
        ModList.get().forEachModFile(mf -> {
            IConfigurable config = mf.getModFileInfo().getConfig();
            Object modName = config.getConfigElement("displayName").orElse("<unnamed>");
            Object modId = config.getConfigElement("modId").orElse("<unnamed>");

            // TODO maybe need other...

            config.getConfigList("entrypoints").forEach(ic ->
                    ic.<List<Object>>getConfigElement("cardinal-components")
                            .map(lo -> lo.stream().map(Object::toString)
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
                                    .toList())
                            .orElseThrow()
                            .forEach(c -> map.computeIfAbsent("cardinal-components", s -> Lists.newArrayList())
                                    .add(new EntrypointContainer<>(c, mf))));
        });
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
        return map.get(key).stream().map(ec -> {
            try {
                type.cast(ec.entrypoint());
                return (EntrypointContainer<T>) ec;
            } catch (ClassCastException e) {
                throw new RuntimeException(e);
            }
        }).toList();
    }
}
