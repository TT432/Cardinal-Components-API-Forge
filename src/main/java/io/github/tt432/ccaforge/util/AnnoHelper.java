package io.github.tt432.ccaforge.util;

import dev.onyxstudios.cca.internal.base.asm.StaticComponentLoadingException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IConfigurable;
import net.minecraftforge.forgespi.language.ModFileScanData;
import net.minecraftforge.forgespi.locating.IModFile;
import org.objectweb.asm.Type;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author DustW
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AnnoHelper {
    public static <I> List<I> getAllInstance(Class<?> anno, Class<I> cast) {
        List<I> result = new ArrayList<>();

        ModList.get().forEachModFile(mf -> result.addAll(getInstance(mf, anno, cast)));

        return result;
    }

    public static <I> List<? extends I> getInstance(IModFile mf, Class<?> anno, Class<I> cast) {
        IConfigurable config = mf.getModFileInfo().getConfig();
        Object modName = config.getConfigElement("displayName").orElse("<unnamed>");
        Object modId = config.getConfigElement("modId").orElse("<unnamed>");

        return getClasses(mf, anno, cast).stream().map(c -> {
            try {
                return c.getDeclaredConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e) {
                throw new StaticComponentLoadingException("Exception in mod %s (%s) : %s can't new instance".formatted(modName, modId, c.getName()));
            }
        }).toList();
    }

    public static <I> List<Class<? extends I>> getClasses(IModFile mf, Class<?> anno, Class<I> cast) {
        List<Class<? extends I>> result = new ArrayList<>();

        IConfigurable config = mf.getModFileInfo().getConfig();
        Object modName = config.getConfigElement("displayName").orElse("<unnamed>");
        Object modId = config.getConfigElement("modId").orElse("<unnamed>");

        Type annoType = Type.getType(anno);

        for (ModFileScanData.AnnotationData annotation : mf.getScanResult().getAnnotations()) {
            if (Objects.equals(annotation.annotationType(), annoType)) {
                String memberName = annotation.memberName();

                try {
                    Class<?> asmClass = Class.forName(memberName);

                    Class<? extends I> asmInstanceClass = asmClass.asSubclass(cast);
                    result.add(asmInstanceClass);
                } catch (ReflectiveOperationException | LinkageError e) {
                    throw new StaticComponentLoadingException("Exception while querying %s (%s) for supported static component types".formatted(modName, modId), e);
                } catch (ClassCastException e) {
                    throw new StaticComponentLoadingException("Exception in mod %s (%s) : %s can't cast to %s".formatted(modName, modId, memberName, cast.getName()));
                }
            }
        }

        return result;
    }
}
