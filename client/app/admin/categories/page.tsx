"use client";

import { useEffect, useState } from "react";
import { getCategories, createCategory, updateCategory, deleteCategory } from "@/lib/category";
import type { Category } from "@/lib/types";

export default function AdminCategoriesPage() {
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editing, setEditing] = useState<Category | null>(null);
  const [name, setName] = useState("");
  const [parentId, setParentId] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [search, setSearch] = useState("");

  const load = async () => {
    setLoading(true);
    try {
      const data = await getCategories().catch(() => [] as Category[]);
      setCategories(data);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const filtered = categories.filter((c) => !search || c.name.toLowerCase().includes(search.toLowerCase()));

  const openCreate = () => {
    setEditing(null);
    setName("");
    setParentId("");
    setError("");
    setShowModal(true);
  };

  const openEdit = (c: Category) => {
    setEditing(c);
    setName(c.name);
    setParentId(c.parent_id ?? c.parentId ?? "");
    setError("");
    setShowModal(true);
  };

  const handleSave = async () => {
    if (!name.trim()) {
      setError("Category name is required");
      return;
    }
    setSaving(true);
    setError("");
    try {
      if (editing) {
        await updateCategory(editing.id, { name: name.trim(), parent_id: parentId || null, parentId: parentId || null });
      } else {
        await createCategory({ name: name.trim(), parent_id: parentId || null, parentId: parentId || null } as Category);
      }
      setShowModal(false);
      await load();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to save category");
    } finally {
      setSaving(false);
    }
  };

  const getDescendantIds = (startId: string, all: Category[]): Set<string> => {
    const map = new Map<string, string | null>();
    all.forEach((c) => map.set(c.id, c.parent_id ?? c.parentId ?? null));
    const descendants = new Set<string>();
    const queue = [startId];
    while (queue.length) {
      const cur = queue.pop()!;
      for (const [cid, pid] of map) {
        if (pid === cur && !descendants.has(cid)) {
          descendants.add(cid);
          queue.push(cid);
        }
      }
    }
    return descendants;
  };

  const handleDelete = async (id: string) => {
    const cat = categories.find((c) => c.id === id);
    const childCount = categories.filter((c) => (c.parent_id ?? c.parentId) === id).length;
    const msg = `Delete "${cat?.name ?? id.slice(0, 8)}"?` + (childCount ? `\n${childCount} sub-category(ies) will be orphaned (moved to top-level).` : "") + `\nProducts in this category will be moved to Uncategorized.`;
    if (!confirm(msg)) return;
    try {
      await deleteCategory(id);
      await load();
    } catch (e: unknown) {
      alert(e instanceof Error ? e.message : "Delete failed");
    }
  };

  return (
    <div className="space-y-5">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h1 className="font-bold text-[24px] text-[#1c1917] tracking-tight flex items-center gap-2">
            <span className="material-symbols-outlined text-[#b45309]">category</span>
            Categories
          </h1>
          <p className="text-[13px] text-[#57534e]">Organize products with categories and sub-categories.</p>
        </div>
        <button onClick={openCreate} className="inline-flex items-center gap-1.5 px-4 py-2.5 bg-[#b45309] text-white rounded-xl text-[13px] font-semibold hover:bg-[#92400e] transition-colors shadow-sm">
          <span className="material-symbols-outlined text-[18px]">add</span>
          Add Category
        </button>
      </div>

      <div className="bg-white rounded-xl border border-[#d6d3d1] shadow-sm p-4">
        <div className="relative max-w-md">
          <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-[#a8a29e] text-[18px]">search</span>
          <input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search categories..."
            className="w-full bg-[#fafaf9] border border-[#d6d3d1] rounded-xl pl-10 pr-4 py-2.5 text-[13px] focus:border-[#b45309] focus:ring-1 focus:ring-[#b45309] outline-none"
          />
        </div>
      </div>

      <div className="bg-white rounded-xl border border-[#d6d3d1] shadow-sm overflow-hidden">
        {loading ? (
          <div className="flex items-center justify-center py-16">
            <span className="material-symbols-outlined animate-spin text-[#b45309] text-[28px]">progress_activity</span>
          </div>
        ) : filtered.length === 0 ? (
          <div className="py-16 text-center">
            <span className="material-symbols-outlined text-[#a8a29e] text-[48px]">category</span>
            <p className="text-[14px] text-[#57534e] mt-2">No categories found.</p>
          </div>
        ) : (
          <div className="divide-y divide-[#e7e5e4]">
            {filtered.map((c) => {
              const isChild = !!(c.parent_id ?? c.parentId);
              const parentName = isChild ? categories.find((p) => p.id === (c.parent_id ?? c.parentId))?.name : null;
              return (
                <div key={c.id} className="px-5 py-4 flex items-center gap-4 hover:bg-[#fafaf9] transition-colors">
                  <div className={`w-10 h-10 rounded-xl flex items-center justify-center shrink-0 ${isChild ? "bg-[#e7e5e4] text-[#57534e]" : "bg-[#fef3c7] text-[#b45309]"}`}>
                    <span className="material-symbols-outlined text-[20px]">{isChild ? "subdirectory_arrow_right" : "folder"}</span>
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="font-medium text-[13px] text-[#1c1917] flex items-center gap-2">
                      {c.name}
                      {isChild && <span className="text-[11px] bg-[#fafaf9] px-1.5 py-0.5 rounded text-[#57534e]">child of {parentName ?? "parent"}</span>}
                    </div>
                    <div className="text-[11px] font-mono text-[#57534e]">{c.id.slice(0, 12)}</div>
                  </div>
                  <div className="flex items-center gap-1 shrink-0">
                    <button onClick={() => openEdit(c)} className="p-1.5 rounded-lg hover:bg-[#fafaf9] text-[#57534e] hover:text-[#b45309] transition-colors" title="Edit">
                      <span className="material-symbols-outlined text-[18px]">edit</span>
                    </button>
                    <button onClick={() => handleDelete(c.id)} className="p-1.5 rounded-lg hover:bg-[#fef2f2] text-[#57534e] hover:text-[#b91c1c] transition-colors" title="Delete">
                      <span className="material-symbols-outlined text-[18px]">delete</span>
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black/40 backdrop-blur-sm" onClick={() => setShowModal(false)} />
          <div className="relative bg-white rounded-2xl shadow-xl w-full max-w-md overflow-hidden">
            <div className="px-6 py-4 border-b border-[#d6d3d1] flex items-center justify-between">
              <h3 className="font-semibold text-[16px] text-[#1c1917]">{editing ? "Edit Category" : "Add Category"}</h3>
              <button onClick={() => setShowModal(false)} className="p-1.5 rounded-full hover:bg-[#fafaf9] text-[#57534e]">
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>

            <div className="px-6 py-5 space-y-4">
              {error && (
                <div className="bg-[#fef2f2] border border-[#fecaca] rounded-xl px-3 py-2 flex items-center gap-2 text-[#b91c1c] text-[13px]">
                  <span className="material-symbols-outlined text-[18px]">error</span> {error}
                </div>
              )}

              <div>
                <label className="block text-[12px] font-semibold text-[#1c1917] mb-1">Category Name *</label>
                <input
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="e.g. Electronics"
                  className="w-full bg-[#fafaf9] border border-[#d6d3d1]/60 rounded-xl px-3 py-2.5 text-[13px] focus:border-[#b45309] focus:ring-1 focus:ring-[#b45309] outline-none"
                />
              </div>

              <div>
                <label className="block text-[12px] font-semibold text-[#1c1917] mb-1">Parent Category (optional)</label>
                <select
                  value={parentId}
                  onChange={(e) => setParentId(e.target.value)}
                  className="w-full bg-[#fafaf9] border border-[#d6d3d1]/60 rounded-xl px-3 py-2.5 text-[13px] focus:border-[#b45309] outline-none"
                >
                  <option value="">— Top-level —</option>
                  {categories
                    .filter((c) => {
                      if (!editing) return true;
                      if (c.id === editing.id) return false;
                      const desc = getDescendantIds(editing.id, categories);
                      return !desc.has(c.id);
                    })
                    .map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.name}
                      </option>
                    ))}
                </select>
                <p className="text-[11px] text-[#57534e] mt-1">Leave empty for top-level. Moving under own descendant is blocked (cycle guard).</p>
              </div>
            </div>

            <div className="px-6 py-4 border-t border-[#d6d3d1] flex justify-end gap-2">
              <button onClick={() => setShowModal(false)} className="px-4 py-2 rounded-xl border border-[#d6d3d1] hover:bg-[#fafaf9] text-[13px] font-semibold">
                Cancel
              </button>
              <button
                onClick={handleSave}
                disabled={saving}
                className="px-5 py-2 rounded-xl bg-[#b45309] text-white hover:bg-[#92400e] disabled:opacity-50 text-[13px] font-semibold flex items-center gap-1.5"
              >
                {saving && <span className="material-symbols-outlined animate-spin text-[16px]">progress_activity</span>}
                {editing ? "Update" : "Create"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
