package me.skywars2.game;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Exact, in-memory values for the two plain data schemas, never a server object graph. */
final class MatchValueSnapshot {
    private record ArrayValue(int[] original, int[] values) { }
    private record SetValue(Set<String> original, Set<String> values) { }
    private final Object original;
    private final Map<Field, Object> values = new LinkedHashMap<>();

    @SuppressWarnings("unchecked")
    MatchValueSnapshot(Object original) {
        if (!(original instanceof GameConfig) && !(original instanceof PlayerStore.Profile))
            throw new IllegalArgumentException("Only game settings and player profiles are value schemas");
        this.original = original;
        try {
            for (var field : original.getClass().getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                if (!Modifier.isPublic(field.getModifiers()) || Modifier.isFinal(field.getModifiers()))
                    throw new IllegalStateException("Review new checkpoint schema field: " + field);
                Object value = field.get(original);
                if (field.getType() == int[].class) {
                    if (value != null) value = new ArrayValue((int[]) value, ((int[]) value).clone());
                } else if (field.getType() == Set.class) {
                    if (value != null) {
                        for (Object entry : (Set<?>) value) if (!(entry instanceof String))
                            throw new IllegalStateException("Non-string checkpoint set: " + field);
                        value = new SetValue((Set<String>) value, new LinkedHashSet<>((Set<String>) value));
                    }
                } else if (field.getType() != int.class && field.getType() != boolean.class && field.getType() != String.class) {
                    throw new IllegalStateException("Review new checkpoint schema type: " + field);
                }
                values.put(field, value);
            }
        } catch (IllegalAccessException error) { throw new IllegalStateException(error); }
    }

    Object restore() {
        try {
            for (var entry : values.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof ArrayValue array) {
                    System.arraycopy(array.values, 0, array.original, 0, array.values.length);
                    value = array.original;
                } else if (value instanceof SetValue set) {
                    if (!set.original.equals(set.values)) { set.original.clear(); set.original.addAll(set.values); }
                    value = set.original;
                }
                entry.getKey().set(original, value);
            }
            return original;
        } catch (IllegalAccessException error) { throw new IllegalStateException(error); }
    }
}
