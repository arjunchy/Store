import { apiClient } from "./api-client";
import type { Category } from "./types";

type CategoryResponse = {
  id: string;
  name: string;
  parentId: string | null;
  createdAt: string;
};

function mapCategory(res: CategoryResponse): Category {
  return {
    id: res.id,
    name: res.name,
    parent_id: res.parentId,
    parentId: res.parentId,
  };
}

export async function getCategories(): Promise<Category[]> {
  const res = await apiClient.get<CategoryResponse[]>("/categories");
  return res.map(mapCategory);
}

export async function getCategoryTree(): Promise<Category[]> {
  const res = await apiClient.get<CategoryResponse[]>("/categories", {
    params: { topLevelOnly: true },
  });
  return res.map(mapCategory);
}

export async function createCategory(
  cat: Omit<Category, "id">
): Promise<Category> {
  const res = await apiClient.post<CategoryResponse>(
    "/categories",
    { name: cat.name, parentId: cat.parent_id },
    { auth: true }
  );
  return mapCategory(res);
}

export async function updateCategory(
  id: string,
  patch: Partial<Category>
): Promise<Category | null> {
  const res = await apiClient.put<CategoryResponse>(
    `/categories/${id}`,
    { name: patch.name, parentId: patch.parent_id },
    { auth: true }
  );
  return mapCategory(res);
}

export async function deleteCategory(id: string): Promise<void> {
  await apiClient.delete(`/categories/${id}`, { auth: true });
}
