package com.df.savingsagent.security;

public final class AuthContextHolder {
    private static final ThreadLocal<AuthContext> AUTH = new ThreadLocal<>();

    private AuthContextHolder() {
    }

    public static void set(AuthContext authContext) {
        AUTH.set(authContext);
    }

    public static AuthContext get() {
        AuthContext context = AUTH.get();
        if (context == null) {
            throw new IllegalStateException("No authenticated context is available");
        }
        return context;
    }

    public static void clear() {
        AUTH.remove();
    }
}
