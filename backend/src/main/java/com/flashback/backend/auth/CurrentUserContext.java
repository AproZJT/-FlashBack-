package com.flashback.backend.auth;

public final class CurrentUserContext {
    private static final ThreadLocal<String> USER_ID = new ThreadLocal<>();

    private CurrentUserContext() {}

    public static void set(String userId) {
        USER_ID.set(userId);
    }

    public static String get() {
        return USER_ID.get();
    }

    public static void clear() {
        USER_ID.remove();
    }
}
