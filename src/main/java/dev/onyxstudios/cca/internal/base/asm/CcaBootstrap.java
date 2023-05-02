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
package dev.onyxstudios.cca.internal.base.asm;

import com.google.common.annotations.VisibleForTesting;
import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentProvider;
import dev.onyxstudios.cca.api.v3.component.StaticComponentInitializer;
import dev.onyxstudios.cca.internal.base.LazyDispatcher;
import io.github.tt432.ccaforge.entrypointes.EntrypointContainer;
import io.github.tt432.ccaforge.entrypointes.EntrypointHandler;
import io.github.tt432.ccaforge.util.AnnoHelper;
import io.github.tt432.ccaforge.util.ComponentInitializerEntryPoint;
import net.minecraft.ResourceLocationException;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IConfigurable;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;

public final class CcaBootstrap extends LazyDispatcher {

    public static final String COMPONENT_TYPE_INIT_DESC = Type.getMethodDescriptor(Type.VOID_TYPE, Type.getObjectType(CcaAsmHelper.IDENTIFIER), Type.getType(Class.class));
    public static final String COMPONENT_TYPE_GET0_DESC = "(L" + CcaAsmHelper.COMPONENT_CONTAINER + ";)L" + CcaAsmHelper.COMPONENT + ";";
    public static final CcaBootstrap INSTANCE = new CcaBootstrap();

    private final List<StaticComponentInitializer> staticComponentInitializers = new ArrayList<>();

    @VisibleForTesting
    Collection<ResourceLocation> additionalComponentIds = new ArrayList<>();
    private Map<ResourceLocation, Class<? extends ComponentKey<?>>> generatedComponentTypes = new HashMap<>();

    public CcaBootstrap() {
        super("registering a ComponentType");
    }

    public boolean isGenerated(Class<?> keyClass) {
        if (this.requiresInitialization()) return false;
        return this.generatedComponentTypes.containsValue(keyClass);
    }

    @Nullable
    public Class<? extends ComponentKey<?>> getGeneratedComponentTypeClass(ResourceLocation componentId) {
        this.ensureInitialized();
        assert this.generatedComponentTypes != null;
        return this.generatedComponentTypes.get(componentId);
    }

    @Override
    protected void init() {
        try {
            EntrypointHandler.loadAll();

            Set<ResourceLocation> staticComponentTypes = new TreeSet<>(Comparator.comparing(ResourceLocation::toString));

            staticComponentInitializers.addAll(EntrypointHandler.getEntrypointContainers("cardinal-components",
                    StaticComponentInitializer.class).stream().map(EntrypointContainer::entrypoint).toList());

            ModList.get().forEachModFile(mf -> {
                IConfigurable config = mf.getModFileInfo().getConfig();
                List<? extends IConfigurable> mods = config.getConfigList("mods");
                IConfigurable modEntry = mods.get(0);
                Object modName = modEntry.getConfigElement("displayName").orElse("<unnamed>");
                Object modId = modEntry.getConfigElement("modId").orElse("<unnamed>");

                config.<List<Object>>getConfigElement("custom",  "cardinal-components").ifPresent(ls -> ls.stream()
                        .map(Object::toString)
                        .forEach(value -> {
                            try {
                                staticComponentTypes.add(new ResourceLocation(value));
                            } catch (ClassCastException | ResourceLocationException e) {
                                throw new StaticComponentLoadingException(
                                        "Failed to load component ids declared by %s (%s)".formatted(modName, modId), e);
                            }
                        }));

                staticComponentInitializers.addAll(AnnoHelper.getInstance(mf,
                        ComponentInitializerEntryPoint.class, StaticComponentInitializer.class));
            });

            staticComponentTypes.addAll(staticComponentInitializers.stream()
                    .map(StaticComponentInitializer::getSupportedComponentKeys)
                    .flatMap(Collection::stream)
                    .toList());

            staticComponentTypes.addAll(this.additionalComponentIds);

            this.spinStaticContainerItf(staticComponentTypes);
            this.generatedComponentTypes = this.spinStaticComponentKeys(staticComponentTypes);
        } catch (IOException | UncheckedIOException e) {
            throw new StaticComponentLoadingException("Failed to load statically defined components", e);
        }
    }

    @Override
    protected void postInit() {
        for (StaticComponentInitializer staticInitializer : this.staticComponentInitializers) {
            staticInitializer.finalizeStaticBootstrap();
        }
    }

    /**
     * Defines a {@link ComponentKey} subclass for every statically declared component, as well as
     * a global {@link ComponentProvider} specialized interface
     * that declares a direct getter for every {@link ComponentKey} that has been scanned by plugins.
     *
     * @param staticComponentKeys the set of all statically declared {@link ComponentKey} ids
     * @return a map of {@link ComponentKey} ids to specialized implementations
     */
    private Map<ResourceLocation, Class<? extends ComponentKey<?>>> spinStaticComponentKeys(Set<ResourceLocation> staticComponentKeys) throws IOException {
        Map<ResourceLocation, Class<? extends ComponentKey<?>>> generatedComponentTypes = new HashMap<>(staticComponentKeys.size());

        for (ResourceLocation componentId : staticComponentKeys) {
            /* generate the component type class */

            ClassNode componentTypeWriter = new ClassNode(CcaAsmHelper.ASM_VERSION);
            String componentTypeName = CcaAsmHelper.getComponentTypeName(componentId);
            componentTypeWriter.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, componentTypeName, null, CcaAsmHelper.COMPONENT_TYPE, null);

            MethodVisitor init = componentTypeWriter.visitMethod(Opcodes.ACC_PUBLIC, "<init>", COMPONENT_TYPE_INIT_DESC, null, null);
            init.visitCode();
            init.visitVarInsn(Opcodes.ALOAD, 0);
            init.visitVarInsn(Opcodes.ALOAD, 1);
            init.visitVarInsn(Opcodes.ALOAD, 2);
            init.visitMethodInsn(Opcodes.INVOKESPECIAL, CcaAsmHelper.COMPONENT_TYPE, "<init>", COMPONENT_TYPE_INIT_DESC, false);
            init.visitInsn(Opcodes.RETURN);
            init.visitEnd();

            MethodVisitor get = componentTypeWriter.visitMethod(Opcodes.ACC_PROTECTED, "getInternal", COMPONENT_TYPE_GET0_DESC, null, null);
            get.visitCode();
            get.visitVarInsn(Opcodes.ALOAD, 1);
            // stack: object
            get.visitTypeInsn(Opcodes.CHECKCAST, CcaAsmHelper.STATIC_COMPONENT_CONTAINER);
            // stack: generatedComponentContainer
            get.visitMethodInsn(Opcodes.INVOKEVIRTUAL, CcaAsmHelper.STATIC_COMPONENT_CONTAINER, CcaAsmHelper.getStaticStorageGetterName(componentId), CcaAsmHelper.STATIC_CONTAINER_GETTER_DESC, false);
            // stack: component
            get.visitInsn(Opcodes.ARETURN);
            get.visitEnd();

            @SuppressWarnings("unchecked") Class<? extends ComponentKey<?>> ct = (Class<? extends ComponentKey<?>>) CcaAsmHelper.generateClass(componentTypeWriter);
            generatedComponentTypes.put(componentId, ct);
        }
        return generatedComponentTypes;
    }

    /**
     * Generate the component container interface implemented by all component containers
     */
    private void spinStaticContainerItf(Set<ResourceLocation> staticComponentTypes) throws IOException {
        ClassNode staticContainerWriter = new ClassNode(CcaAsmHelper.ASM_VERSION);
        staticContainerWriter.visit(Opcodes.V1_8, Opcodes.ACC_ABSTRACT | Opcodes.ACC_PUBLIC, CcaAsmHelper.STATIC_COMPONENT_CONTAINER, null, CcaAsmHelper.DYNAMIC_COMPONENT_CONTAINER_IMPL, null);

        MethodVisitor init = staticContainerWriter.visitMethod(Opcodes.ACC_PUBLIC, "<init>", CcaAsmHelper.ABSTRACT_COMPONENT_CONTAINER_CTOR_DESC, null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, CcaAsmHelper.DYNAMIC_COMPONENT_CONTAINER_IMPL, "<init>", CcaAsmHelper.ABSTRACT_COMPONENT_CONTAINER_CTOR_DESC, false);
        init.visitInsn(Opcodes.RETURN);
        init.visitEnd();

        for (ResourceLocation componentId : staticComponentTypes) {
            MethodVisitor methodWriter = staticContainerWriter.visitMethod(Opcodes.ACC_PUBLIC, CcaAsmHelper.getStaticStorageGetterName(componentId), CcaAsmHelper.STATIC_CONTAINER_GETTER_DESC, null, null);
            methodWriter.visitInsn(Opcodes.ACONST_NULL);
            methodWriter.visitInsn(Opcodes.ARETURN);
            methodWriter.visitEnd();
        }

        staticContainerWriter.visitEnd();
        CcaAsmHelper.generateClass(staticContainerWriter);
    }

}
