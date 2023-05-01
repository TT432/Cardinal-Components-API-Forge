package dev.onyxstudios.cca.api.v3.util;

import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierDefault;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author DustW
 */
@Documented
@Nonnull
@TypeQualifierDefault(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ComponentInitializerEntryPoint {
}
