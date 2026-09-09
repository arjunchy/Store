const API_BASE = "/api";

function isBrowser() {
  return typeof window !== "undefined";
}

function getAccessToken(): string | null {
  if (!isBrowser()) return null;
  return localStorage.getItem("apexcommerce_access_token");
}

function getRefreshToken(): string | null {
  if (!isBrowser()) return null;
  return localStorage.getItem("apexcommerce_refresh_token");
}

function setTokens(access: string, refresh: string) {
  if (!isBrowser()) return;
  localStorage.setItem("apexcommerce_access_token", access);
  localStorage.setItem("apexcommerce_refresh_token", refresh);
}

function clearTokens() {
  if (!isBrowser()) return;
  localStorage.removeItem("apexcommerce_access_token");
  localStorage.removeItem("apexcommerce_refresh_token");
  localStorage.removeItem("apexcommerce_user");
  // also clear stale checkout snapshot on auth invalid
  try {
    localStorage.removeItem("apexcommerce_last_order_snapshot");
    localStorage.removeItem("apexcommerce_last_order_id");
  } catch {}
}

/** Notify the app that the session is no longer valid (e.g. refresh failed). */
function notifyAuthInvalid() {
  if (isBrowser()) {
    window.dispatchEvent(new Event("apexcommerce:auth:invalid"));
  }
}

export function getStoredUser() {
  if (!isBrowser()) return null;
  const raw = localStorage.getItem("apexcommerce_user");
  if (!raw) return null;
  try {
    return JSON.parse(raw) as { id: string; username: string; email: string; userRole: string };
  } catch {
    return null;
  }
}

export function setStoredUser(user: { id: string; username: string; email: string; userRole: string } | null) {
  if (!isBrowser()) return;
  if (user) {
    localStorage.setItem("apexcommerce_user", JSON.stringify(user));
  } else {
    localStorage.removeItem("apexcommerce_user");
  }
}

let refreshPromise: Promise<string | null> | null = null;

async function refreshAccessToken(): Promise<string | null> {
  const refreshToken = getRefreshToken();
  if (!refreshToken) return null;
  if (refreshPromise) return refreshPromise;

  refreshPromise = (async () => {
    try {
      const res = await fetch(`${API_BASE}/auth/refresh`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ refreshToken }),
      });
      if (!res.ok) {
        clearTokens();
        notifyAuthInvalid();
        return null;
      }
      const data = await res.json();
      if (!data?.accessToken || !data?.refreshToken) {
        clearTokens();
        notifyAuthInvalid();
        return null;
      }
      setTokens(data.accessToken, data.refreshToken);
      return data.accessToken as string;
    } catch {
      clearTokens();
      notifyAuthInvalid();
      return null;
    } finally {
      refreshPromise = null;
    }
  })();

  return refreshPromise;
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}

class ApiClient {
  private async request<T>(
    method: string,
    path: string,
    body?: unknown,
    options?: { auth?: boolean; params?: Record<string, string | number | boolean | undefined> }
  ): Promise<T> {
    const isServer = !isBrowser();
    const base = isServer
      ? process.env.BACKEND_URL || process.env.NEXT_PUBLIC_API_URL || "http://localhost:8081"
      : window.location.origin;
    const url = new URL(`${API_BASE}${path}`, base);
    if (options?.params) {
      Object.entries(options.params).forEach(([k, v]) => {
        if (v !== undefined && v !== null && v !== "") {
          url.searchParams.set(k, String(v));
        }
      });
    }

    const headers: Record<string, string> = {};
    const accessToken = getAccessToken();
    if (accessToken && options?.auth !== false) {
      headers["Authorization"] = `Bearer ${accessToken}`;
    }
    const isFormData = typeof FormData !== "undefined" && body instanceof FormData;
    if (body !== undefined && !isFormData) {
      headers["Content-Type"] = "application/json";
    }

    let res = await fetch(url.toString(), {
      method,
      headers,
      body: body !== undefined ? (isFormData ? (body as FormData) : JSON.stringify(body)) : undefined,
    });

    if (res.status === 401 && accessToken && options?.auth !== false) {
      const getBody = (): BodyInit | undefined => {
        if (body === undefined) return undefined;
        if (isFormData) return body as FormData;
        return JSON.stringify(body);
      };
      const newToken = await refreshAccessToken();
      if (!newToken) {
        return handleResponse<T>(res);
      }
      headers["Authorization"] = `Bearer ${newToken}`;
      res = await fetch(url.toString(), {
        method,
        headers,
        body: getBody(),
      });
    }

    return handleResponse<T>(res);
  }

  async get<T>(path: string, options?: { auth?: boolean; params?: Record<string, string | number | boolean | undefined> }): Promise<T> {
    return this.request<T>("GET", path, undefined, options);
  }

  async post<T>(path: string, body?: unknown, options?: { auth?: boolean }): Promise<T> {
    return this.request<T>("POST", path, body, options);
  }

  async put<T>(path: string, body?: unknown, options?: { auth?: boolean }): Promise<T> {
    return this.request<T>("PUT", path, body, options);
  }

  async patch<T>(path: string, body?: unknown, options?: { auth?: boolean }): Promise<T> {
    return this.request<T>("PATCH", path, body, options);
  }

  async delete<T = void>(path: string, options?: { auth?: boolean }): Promise<T> {
    return this.request<T>("DELETE", path, undefined, options);
  }
}

async function handleResponse<T>(res: Response): Promise<T> {
  if (res.status === 204) return undefined as T;
  const text = await res.text();
  if (!res.ok) {
    let parsed: ApiError | string;
    try {
      parsed = text ? JSON.parse(text) : res.statusText;
    } catch {
      parsed = text || res.statusText || `HTTP ${res.status}`;
    }
    const message = typeof parsed === "string" ? parsed : (parsed as ApiError).message || (parsed as any).error || "Request failed";
    const err = new Error(message) as Error & { status: number; data: ApiError | string };
    err.status = res.status;
    err.data = parsed;
    throw err;
  }
  if (!text) return undefined as T;
  try {
    return JSON.parse(text) as T;
  } catch {
    return text as unknown as T;
  }
}

export const apiClient = new ApiClient();
export { clearTokens, setTokens, getAccessToken, getRefreshToken };
