import { apiClient, getStoredUser, setStoredUser } from "./api-client";
import type { User } from "./types";

type UserResponse = {
  id: string;
  username: string;
  email: string;
  userRole: string;
  createdAt: string;
};

function mapUser(res: UserResponse): User {
  return {
    id: res.id,
    username: res.username,
    email: res.email,
    role: res.userRole === "ADMIN" ? "ROLE_ADMIN" : "ROLE_USER",
    createdAt: res.createdAt,
  };
}

export async function getCurrentUser(): Promise<User | null> {
  try {
    const res = await apiClient.get<UserResponse>("/users/me", { auth: true });
    return mapUser(res);
  } catch {
    return null;
  }
}

export async function updateProfile(
  patch: Partial<Pick<User, "username" | "email"> & { password?: string }>
): Promise<User | null> {
  const res = await apiClient.put<UserResponse>(
    "/users/me",
    {
      username: patch.username,
      email: patch.email,
      password: patch.password || undefined,
    },
    { auth: true }
  );
  const user = mapUser(res);
  const stored = getStoredUser();
  if (stored) {
    setStoredUser({ id: user.id, username: user.username, email: user.email, userRole: user.role });
  }
  return user;
}

export async function deleteMyAccount(): Promise<void> {
  await apiClient.delete("/users/me", { auth: true });
}

export async function getAllUsers(): Promise<User[]> {
  const res = await apiClient.get<UserResponse[]>("/users", { auth: true });
  return res.map(mapUser);
}

export async function deleteUserHard(email: string): Promise<void> {
  await apiClient.delete(`/users/me`, { auth: true });
}

export function getJwt(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem("apexcommerce_access_token");
}
