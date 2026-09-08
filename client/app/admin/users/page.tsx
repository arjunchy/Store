"use client";

import { useEffect, useState, useCallback } from "react";
import { getAdminUsersPaginated, changeUserRole, toggleUserStatus, hardDeleteUser } from "@/lib/admin";
import type { User } from "@/lib/types";

export default function AdminUsersPage() {
  const [users, setUsers] = useState<User[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [page, setPage] = useState(0);
  const PAGE_SIZE = 20;
  const [loading, setLoading] = useState(true);
  const [searchInput, setSearchInput] = useState("");
  const [search, setSearch] = useState("");
  const [roleFilter, setRoleFilter] = useState<string>("ALL");
  const [includeDeleted, setIncludeDeleted] = useState(false);
  const [acting, setActing] = useState<string | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    const t = setTimeout(() => setSearch(searchInput.trim()), 400);
    return () => clearTimeout(t);
  }, [searchInput]);

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const res = await getAdminUsersPaginated({ page, size: PAGE_SIZE, search: search || undefined, includeDeleted });
      setUsers(res.content as User[]);
      setTotalElements(res.totalElements);
      setTotalPages(res.totalPages);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to load users");
    } finally {
      setLoading(false);
    }
  }, [page, search, includeDeleted]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    setPage(0);
  }, [search, includeDeleted, roleFilter]);

  const filtered = users.filter((u) => roleFilter === "ALL" || u.role === roleFilter);

  const isSystemAdmin = (u: User) => u.email.toLowerCase() === "ecommerce@gmail.com";

  const handleRoleChange = async (user: User, newRole: string) => {
    if (isSystemAdmin(user)) { setError("Cannot change role of system admin (ecommerce@gmail.com)"); return; }
    if (user.isDeleted) { setError("Deactivated user — activate first"); return; }
    setActing(user.id);
    setError("");
    try {
      await changeUserRole(user.id, newRole as "ADMIN" | "USER");
      await load();
    } catch (e) {
      const msg = e instanceof Error ? e.message : "Role change failed";
      setError(msg);
    } finally {
      setActing(null);
    }
  };

  const handleToggleActivateDirect = async (user: User, activate: boolean) => {
    if (isSystemAdmin(user)) { setError("Cannot modify system admin account"); return; }
    if (activate && !user.isDeleted) { setError("User already active"); return; }
    if (!activate && user.isDeleted) { setError("User already deactivated"); return; }
    setActing(user.id);
    try {
      await toggleUserStatus(user.id, activate);
      await load();
    } catch (e) {
      const msg = e instanceof Error ? e.message : "Status change failed";
      setError(msg);
    } finally {
      setActing(null);
    }
  };

  const handleHardDelete = async (user: User) => {
    if (isSystemAdmin(user)) { setError("Cannot delete system admin account (ecommerce@gmail.com)"); return; }
    if (user.isDeleted === false || user.isDeleted === undefined) {
    }
    if (!confirm(`Hard delete user ${user.email}? This will cascade delete all related data (orders, reviews, addresses, carts, wishlists). Continue?`)) return;
    setActing(user.id);
    try {
      await hardDeleteUser(user.id);
      await load();
    } catch (e: any) {
      const msg = e?.data?.message ?? e?.message ?? "Delete failed";
      setError(msg);
    } finally {
      setActing(null);
    }
  };

  return (
    <div className="space-y-5">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h1 className="font-bold text-[24px] text-[#1c1917] tracking-tight flex items-center gap-2">
            <span className="material-symbols-outlined text-[#b45309]">group</span>
            Users
            <span className="text-[11px] font-semibold bg-[#fafaf9] border border-[#d6d3d1] px-2 py-1 rounded-full text-[#57534e]">{totalElements} total</span>
            {totalPages > 1 && <span className="text-[11px] font-semibold bg-[#fef3c7] text-[#b45309] px-2 py-1 rounded-full">Page {page + 1}/{totalPages}</span>}
          </h1>
          <p className="text-[13px] text-[#57534e]">Manage users • Change roles • Activate/deactivate (soft) • Hard delete zero-dependency only (ADMIN).</p>
        </div>
        <div className="bg-white border border-[#d6d3d1] rounded-xl px-3 py-2 flex items-center gap-2">
          <span className="material-symbols-outlined text-[#57534e] text-[18px]">shield_person</span>
          <span className="text-[12px] font-semibold text-[#1c1917]">{totalElements} total</span>
          <span className="w-px h-4 bg-outline-variant" />
          <span className="text-[11px] text-[#57534e]">{users.filter((u) => u.role === "ROLE_ADMIN").length} admins on page</span>
          <label className="ml-2 flex items-center gap-1 text-[11px] cursor-pointer">
            <input type="checkbox" checked={includeDeleted} onChange={(e) => setIncludeDeleted(e.target.checked)} className="w-3 h-3" />
            Include deactivated
          </label>
        </div>
      </div>

      {error && (
        <div className="bg-[#fef2f2] border border-[#fecaca] rounded-xl px-3 py-2 flex items-center gap-2 text-[#b91c1c] text-[13px]">
          <span className="material-symbols-outlined text-[18px]">error</span> {error}
          <button onClick={() => setError("")} className="ml-auto text-[11px] underline">Dismiss</button>
        </div>
      )}

      <div className="bg-white rounded-xl border border-[#d6d3d1] shadow-sm p-4 flex flex-col lg:flex-row gap-3">
        <div className="relative flex-1">
          <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-[#a8a29e] text-[18px]">search</span>
          <input
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            placeholder="Search by name, email or ID… (server-filtered)"
            className="w-full bg-[#fafaf9] border border-[#d6d3d1] rounded-xl pl-10 pr-4 py-2.5 text-[13px] focus:border-[#b45309] focus:ring-1 focus:ring-[#b45309] outline-none"
          />
        </div>
        <div className="flex gap-1 overflow-x-auto">
          {["ALL", "ROLE_USER", "ROLE_ADMIN"].map((r) => (
            <button
              key={r}
              onClick={() => setRoleFilter(r)}
              className={`px-3 py-2 rounded-xl text-[12px] font-semibold border transition-colors whitespace-nowrap ${
                roleFilter === r ? "bg-[#b45309] text-white border-[#b45309]" : "bg-[#fafaf9] text-[#57534e] border-[#d6d3d1] hover:text-[#1c1917]"
              }`}
            >
              {r === "ALL" ? "All" : r === "ROLE_ADMIN" ? "Admins" : "Users"}
            </button>
          ))}
        </div>
      </div>

      <div className="bg-white rounded-xl border border-[#d6d3d1] shadow-sm overflow-hidden">
        {loading ? (
          <div className="flex items-center justify-center py-16">
            <span className="material-symbols-outlined animate-spin text-[#b45309] text-[28px]">progress_activity</span>
          </div>
        ) : filtered.length === 0 ? (
          <div className="py-16 text-center">
            <span className="material-symbols-outlined text-[#a8a29e] text-[48px]">person_search</span>
            <p className="text-[14px] text-[#57534e] mt-2">No users match your filters.</p>
          </div>
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full text-left">
                <thead className="bg-[#b45309] text-[11px] font-semibold tracking-widest uppercase text-[#fafaf9]">
                  <tr>
                    <th className="px-4 py-3">User</th>
                    <th className="px-4 py-3">Email</th>
                    <th className="px-4 py-3">Role</th>
                    <th className="px-4 py-3">Status</th>
                    <th className="px-4 py-3">Created</th>
                    <th className="px-4 py-3 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#e7e5e4]">
                  {filtered.map((u) => {
                    const displayName = (u.username ?? u.email?.split("@")[0] ?? "?").toString();
                    const safeId = u.id ?? (u as unknown as { userId?: string }).userId ?? "";
                    const isDeleted = !!(u as any).isDeleted || !!(u as any).deletedAt;
                    const isSystemAdminRow = isSystemAdmin(u);
                    return (
                      <tr key={safeId || displayName} className={`hover:bg-[#fafaf9] transition-colors ${isDeleted ? "opacity-60 bg-[#fee2e2]/5" : ""} ${isSystemAdminRow ? "bg-amber-50/50" : ""}`}>
                        <td className="px-4 py-3">
                          <div className="flex items-center gap-3">
                            <div className={`w-8 h-8 rounded-full flex items-center justify-center shrink-0 ${isDeleted ? "bg-[#fee2e2] text-[#b91c1c]" : "bg-[#fef3c7] text-[#b45309]"}`}>
                              <span className="font-semibold text-[12px]">{displayName.slice(0, 2).toUpperCase()}</span>
                            </div>
                            <div>
                              <div className="font-medium text-[13px] text-[#1c1917] flex items-center gap-1">
                                {displayName} {isDeleted && <span className="text-[10px] bg-error text-on-error px-1 py-0.5 rounded">Deactivated</span>}
                              </div>
                              <div className="font-mono text-[11px] text-[#57534e] truncate max-w-[120px]">{safeId ? safeId.slice(0, 12) : "—"}</div>
                            </div>
                          </div>
                        </td>
                        <td className="px-4 py-3 text-[13px] text-[#57534e]">{u.email}</td>
                        <td className="px-4 py-3">
                          {isSystemAdminRow ? (
                            <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full bg-[#0c0a09] text-white text-[11px] font-bold border border-[#292524]">
                              <span className="material-symbols-outlined text-[12px]">shield</span> System Admin
                            </span>
                          ) : (
                            <>
                              <select
                                value={u.role}
                                onChange={(e) => handleRoleChange(u, e.target.value)}
                                disabled={acting === u.id || isDeleted}
                                className="bg-[#fafaf9] border border-[#d6d3d1] rounded-lg px-2 py-1.5 text-[11px] font-semibold focus:border-[#b45309] outline-none disabled:opacity-40"
                                title={isDeleted ? "Activate user first" : "Change role"}
                              >
                                <option value="ROLE_USER">ROLE_USER</option>
                                <option value="ROLE_ADMIN">ROLE_ADMIN</option>
                              </select>
                              {acting === u.id && <span className="ml-1 material-symbols-outlined animate-spin text-[12px] text-[#b45309]">progress_activity</span>}
                            </>
                          )}
                        </td>
                        <td className="px-4 py-3">
                          <span className={`text-[11px] font-semibold px-2 py-1 rounded-full border ${isDeleted ? "bg-[#fee2e2] text-[#b91c1c] border-[#fecaca]" : "bg-[#15803d]/15 text-[#15803d] border-[#15803d]/20"}`}>{isDeleted ? "Deactivated" : "Active"}</span>
                        </td>
                        <td className="px-4 py-3 text-[12px] text-[#57534e]">{u.createdAt ? new Date(u.createdAt).toLocaleDateString() : "—"}</td>
                        <td className="px-4 py-3">
                          <div className="flex items-center justify-end gap-1">
                            <button
                              onClick={() => handleToggleActivateDirect(u, false)}
                              disabled={acting === u.id || isDeleted || isSystemAdminRow}
                              className="p-1.5 rounded-lg hover:bg-[#ffedd5]/30 text-[#57534e] hover:text-[#b45309] transition-colors disabled:opacity-30"
                              title={isSystemAdminRow ? "Cannot deactivate system admin" : "Deactivate (soft delete, revokes tokens)"}
                            >
                              <span className="material-symbols-outlined text-[18px]">block</span>
                            </button>
                            <button
                              onClick={() => handleToggleActivateDirect(u, true)}
                              disabled={acting === u.id || !isDeleted || isSystemAdminRow}
                              className="p-1.5 rounded-lg hover:bg-[#15803d]/15 text-[#57534e] hover:text-[#15803d] transition-colors disabled:opacity-30"
                              title={isSystemAdminRow ? "System admin cannot be deactivated" : "Activate (restore)"}
                            >
                              <span className="material-symbols-outlined text-[18px]">check_circle</span>
                            </button>
                            <button
                              onClick={() => handleHardDelete(u)}
                              disabled={acting === u.id || isSystemAdminRow}
                              className="p-1.5 rounded-lg hover:bg-[#fef2f2] text-[#57534e] hover:text-[#b91c1c] transition-colors disabled:opacity-30 disabled:cursor-not-allowed"
                              title={isSystemAdminRow ? "Cannot delete system admin account" : "Hard delete — cascade delete all related data"}
                            >
                              <span className="material-symbols-outlined text-[18px]">delete_forever</span>
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
            {totalPages > 1 && (
              <div className="flex items-center justify-between px-4 py-3 border-t border-[#e7e5e4] bg-[#fafaf9]">
                <button onClick={() => setPage((p) => Math.max(0, p - 1))} disabled={page <= 0} className="px-3 py-1.5 rounded-lg border border-[#d6d3d1] bg-white text-[12px] font-semibold disabled:opacity-40">← Prev</button>
                <span className="text-[12px] text-[#57534e]">Page <span className="font-semibold text-[#1c1917]">{page + 1}</span> of {totalPages} • {totalElements} users</span>
                <button onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))} disabled={page + 1 >= totalPages} className="px-3 py-1.5 rounded-lg border border-[#d6d3d1] bg-white text-[12px] font-semibold disabled:opacity-40">Next →</button>
              </div>
            )}
          </>
        )}
      </div>

      <div className="bg-[#fafaf9] rounded-xl p-4 flex items-start gap-2">
        <span className="material-symbols-outlined text-[#b45309] text-[20px]">info</span>
        <div className="text-[12px] text-[#57534e] leading-relaxed">
          <p>
            <span className="font-semibold text-[#1c1917]">Endpoints:</span> <span className="font-mono text-[#b45309]">GET /api/users/all?includeDeleted&search&page&size&paged=true</span> (ADMIN) • Deactivate revokes refresh tokens • Hard delete 409 if user has orders/reviews/addresses.
          </p>
          <p className="mt-1">Role change disabled for deactivated users — activate first • Status pill shows soft-delete • Pagination 20/page.</p>
        </div>
      </div>
    </div>
  );
}
