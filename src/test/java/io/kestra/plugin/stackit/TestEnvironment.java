package io.kestra.plugin.stackit;

import java.lang.reflect.Field;
import java.util.Map;

public final class TestEnvironment {
    private TestEnvironment() {
    }

    @SuppressWarnings("unchecked")
    public static void set(String name, String value) {
        try {
            Class<?> processEnvironment = Class.forName("java.lang.ProcessEnvironment");
            setMap(processEnvironment.getDeclaredField("theEnvironment"), name, value);
            setMap(processEnvironment.getDeclaredField("theCaseInsensitiveEnvironment"), name, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not set " + name + " for the test.", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private static void setMap(Field field, String name, String value) throws IllegalAccessException {
        field.setAccessible(true);
        Map<String, String> map = (Map<String, String>) field.get(null);
        map.put(name, value);
    }
}
