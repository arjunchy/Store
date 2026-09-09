"use client";

import { createContext, useContext, ReactNode, useCallback, useEffect, useState } from "react";
import {
  apiClient,
  getStoredUser,
  setStoredUser,
  clearTokens,
  setTokens,
  getAccessToken,
  getRefreshToken,
} from "@/lib/api-client";
import { mergeGuestCartToServer } from "@/lib/guestCart";

type AuthUser = {
  id: string;
  username: string;
  email: string;
  userRole: string;
};

type AuthContextType = {
  user: AuthUser | null;
  loading: boolean;
  isAuthenticated: boolean;
  isAdmin: boolean;
  login: (email: string, password: string) => Promise<AuthUser>;
  register: (username: string, email: string, password: string) => Promise<AuthUser>;
  logout: () => Promise<void>;
};

const AuthContext = createContext<AuthContextType | null>(null);

function normalizeRole(role: string | undefined | null): string {
  if (!role) return "";
  return role.replace(/^ROLE_/, "").toUpperCase();
}

function isAdminRole(role: string | undefined | null): boolean {
  return normalizeRole(role) === "ADMIN";
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const onInvalid = () => {
      clearTokens();
      setStoredUser(null);
      setUser(null);
    };
    if (typeof window !== "undefined") {
      window.addEventListener("apexcommerce:auth:invalid", onInvalid);
      return () => window.removeEventListener("apexcommerce:auth:invalid", onInvalid);
    }
  }, []);

  useEffect(() => {
    let cancelled = false;
    const validateSession = async () => {
      const stored = getStoredUser();
      const token = getAccessToken();
      const refresh = getRefreshToken();
      // If no token at all, not authenticated
      if (!token && !refresh) {
        clearTokens();
        setStoredUser(null);
        if (!cancelled) {
          setUser(null);
          setLoading(false);
        }
        return;
      }
      // If token missing but refresh exists, try to use refresh via api-client on next call; keep stored user optimistic
      if (!token && refresh && stored) {
        // Try to refresh by fetching profile - api-client will auto-refresh on 401
        try {
          const profile = await apiClient.get<{
            id: string;
            username: string;
            email: string;
            userRole: string;
          }>("/users/me");
          if (!cancelled) {
            setStoredUser(profile);
            setUser(profile);
            try { await mergeGuestCartToServer(); } catch {}
          }
        } catch {
          // refresh failed
          clearTokens();
          setStoredUser(null);
          if (!cancelled) setUser(null);
        } finally {
          if (!cancelled) setLoading(false);
        }
        return;
      }
      if (!stored || !token) {
        if (!token) {
          // keep refresh token to allow refresh
          localStorage.removeItem("apexcommerce_access_token");
        }
        if (!cancelled) {
          setUser(null);
          setLoading(false);
        }
        return;
      }
      try {
        const profile = await apiClient.get<{
          id: string;
          username: string;
          email: string;
          userRole: string;
        }>("/users/me");
        if (!cancelled) {
          setStoredUser(profile);
          setUser(profile);
          try {
            await mergeGuestCartToServer();
          } catch {}
        }
      } catch {
        if (!cancelled) {
          // let api-client handle token refresh; don't clear on transient error
          // only clear if refresh also failed (handled via auth:invalid event)
          try {
            const stillHasToken = getAccessToken();
            if (!stillHasToken) {
              clearTokens();
              setStoredUser(null);
              setUser(null);
            } else {
              setUser(stored as any);
            }
          } catch {
            clearTokens();
            setStoredUser(null);
            setUser(null);
          }
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    };
    validateSession();
    return () => { cancelled = true; };
  }, []);

  const login = useCallback(async (email: string, password: string): Promise<AuthUser> => {
    const res = await apiClient.post<{
      accessToken: string;
      refreshToken: string;
      email: string;
      role: string;
    }>("/auth/login", { email, password }, { auth: false });

    setTokens(res.accessToken, res.refreshToken);
    await mergeGuestCartToServer();

    const profile = await apiClient.get<{ id: string; username: string; email: string; userRole: string }>(
      "/users/me"
    );
    setStoredUser(profile);
    setUser(profile);
    return profile;
  }, []);

  const register = useCallback(async (username: string, email: string, password: string): Promise<AuthUser> => {
    await apiClient.post<{ id: string; username: string; email: string; userRole: string }>(
      "/users",
      { username, email, password },
      { auth: false }
    );

    const loginRes = await apiClient.post<{
      accessToken: string;
      refreshToken: string;
      email: string;
      role: string;
    }>("/auth/login", { email, password }, { auth: false });

    setTokens(loginRes.accessToken, loginRes.refreshToken);
    await mergeGuestCartToServer();

    const profile = await apiClient.get<{ id: string; username: string; email: string; userRole: string }>(
      "/users/me"
    );
    setStoredUser(profile);
    setUser(profile);
    return profile;
  }, []);

  const logout = useCallback(async () => {
    try {
      const refreshToken = getRefreshToken();
      if (refreshToken) {
        await apiClient.post("/auth/logout", { refreshToken }).catch(() => {});
      }
    } catch {
    }
    clearTokens();
    setStoredUser(null);
    setUser(null);
    // clear guest/wishlist/checkout caches to prevent cross-user leak
    try {
      localStorage.removeItem("apexcommerce_guest_cart");
      localStorage.removeItem("apexcommerce_wishlist_cache");
      localStorage.removeItem("apexcommerce_last_order_snapshot");
      localStorage.removeItem("apexcommerce_last_order_id");
      localStorage.removeItem("apexcommerce_delivery_method");
      localStorage.removeItem("apexcommerce_v1_checkout_delivery");
      localStorage.removeItem("apexcommerce_selected_address");
      localStorage.removeItem("apexcommerce_payment_method");
    } catch {}
  }, []);

  const isAuthenticated = !!user && !!getAccessToken();
  const isAdmin = isAdminRole(user?.userRole);

  return (
    <AuthContext.Provider
      value={{
        user,
        loading,
        isAuthenticated,
        login,
        register,
        logout,
        isAdmin,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
