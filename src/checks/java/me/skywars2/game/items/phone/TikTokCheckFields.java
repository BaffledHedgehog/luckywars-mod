package me.skywars2.game.items.phone;

/** Existing phone fixture observations kept in the test source set. */
final class TikTokCheckFields {
    private TikTokCheckFields() {}

    static Object read(Object target, String name) {
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            try {
                var field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException missing) {
                // Look on the superclass as the original test helper does.
            } catch (IllegalAccessException error) {
                throw new IllegalStateException(error);
            }
        }
        throw new IllegalStateException(name);
    }
}
