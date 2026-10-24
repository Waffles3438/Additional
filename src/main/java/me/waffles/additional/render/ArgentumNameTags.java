package me.waffles.additional.render;

import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

/**
 * Optional bridge to Argentum's nametag batch.
 *
 * Argentum collects labels drawn during its entity pass and submits them in a
 * few draw calls. The ESP pass runs after that batch has been flushed, so each
 * label would otherwise fall back to separate background and text draws.
 * Argentum's classes are internal, so they are resolved reflectively and the
 * bridge turns itself off if they are missing or change.
 */
final class ArgentumNameTags {
    private static final Logger LOGGER = LogManager.getLogger("Additional");
    private static final MethodType SUPPLIER = MethodType.methodType(Object.class);
    private static final MethodType FUNCTION = MethodType.methodType(Object.class, Object.class);
    private static final MethodType ACTION = MethodType.methodType(void.class, Object.class);

    private static MethodHandle renderer, batch, matrices, modelView, active, begin, end, discard;
    private static boolean available;

    static {
        if (FabricLoader.getInstance().isModLoaded("argentum")) {
            try {
                ClassLoader loader = ArgentumNameTags.class.getClassLoader();
                Class<?> rendererClass = Class.forName(
                        "dev.rdh.argentum.impl.render.terrain.ArgentumWorldRenderer", false, loader);
                Class<?> batchClass = Class.forName(
                        "dev.rdh.argentum.impl.render.entity.NameTagBatch", false, loader);
                Method matricesMethod = rendererClass.getMethod("createChunkRenderMatrices");
                Method modelViewMethod = matricesMethod.getReturnType().getMethod("modelView");
                MethodHandles.Lookup lookup = MethodHandles.publicLookup();

                renderer = lookup.unreflect(rendererClass.getMethod("instanceNullable")).asType(SUPPLIER);
                batch = lookup.unreflect(rendererClass.getMethod("getNameTagBatch")).asType(FUNCTION);
                matrices = lookup.unreflect(matricesMethod).asType(FUNCTION);
                modelView = lookup.unreflect(modelViewMethod).asType(FUNCTION);
                active = lookup.unreflect(batchClass.getMethod("active")).asType(SUPPLIER);
                // The camera matrix type comes from Argentum's own JOML copy.
                begin = lookup.unreflect(batchClass.getMethod("begin", modelViewMethod.getReturnType()))
                        .asType(MethodType.methodType(void.class, Object.class, Object.class));
                end = lookup.unreflect(batchClass.getMethod("end")).asType(ACTION);
                discard = lookup.unreflect(batchClass.getMethod("discard")).asType(ACTION);
                available = true;
            } catch (Throwable t) {
                LOGGER.warn("Argentum nametag batching is unavailable; ESP labels will draw individually.", t);
            }
        }
    }

    private ArgentumNameTags() {
    }

    /**
     * Opens Argentum's batch for the ESP pass.
     *
     * @return the batch to pass to {@link #end}, or null when labels should draw directly
     */
    static Object begin() {
        if (!available) return null;
        try {
            // An already-open batch will capture these labels on its own.
            if ((Object) active.invokeExact() != null) return null;
            Object worldRenderer = (Object) renderer.invokeExact();
            if (worldRenderer == null) return null;
            Object nameTags = (Object) batch.invokeExact(worldRenderer);
            Object camera = (Object) modelView.invokeExact((Object) matrices.invokeExact(worldRenderer));
            begin.invokeExact(nameTags, camera);
            return nameTags;
        } catch (Throwable t) {
            disable(t);
            return null;
        }
    }

    /** Draws and closes a batch opened by {@link #begin}. */
    static void end(Object nameTags) {
        if (nameTags == null) return;
        try {
            end.invokeExact(nameTags);
        } catch (Throwable t) {
            try {
                discard.invokeExact(nameTags);
            } catch (Throwable ignored) {
                // The original failure is reported below.
            }
            disable(t);
        }
    }

    private static void disable(Throwable t) {
        // Stop after the first failure so a changed Argentum build cannot
        // throw on every frame.
        available = false;
        LOGGER.warn("Argentum nametag batching failed; ESP labels will draw individually.", t);
    }
}
