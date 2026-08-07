package me.waffles.additional.render;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class PolyNametagCompat {

    private static final boolean AVAILABLE;
    private static final Object INSTANCE;
    private static final Method SET_DRAWING_TAGS;

    static {
        boolean available = false;
        Object instance = null;
        Method setter = null;
        try {
            Class<?> clazz = Class.forName("org.polyfrost.polynametag.PolyNametag");
            Field inst = clazz.getField("INSTANCE");
            instance = inst.get(null);
            setter = clazz.getMethod("setDrawingTags", boolean.class);
            available = true;
        } catch (Throwable ignored) {
        }
        AVAILABLE = available;
        INSTANCE = instance;
        SET_DRAWING_TAGS = setter;
    }

    public static void usingDirectRender(boolean direct) {
        if (AVAILABLE) {
            try {
                SET_DRAWING_TAGS.invoke(INSTANCE, direct);
            } catch (Throwable ignored) {
            }
        }
    }
}