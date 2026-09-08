"use client";
import { useEffect, useState, useCallback } from "react";

export type Page<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
};

export function usePaginatedData<T>(
  fetcher: (params: any) => Promise<Page<T>>,
  initialParams: Record<string, any> = {}
) {
  const [data, setData] = useState<T[]>([]);
  const [page, setPage] = useState(initialParams.page ?? 0);
  const [size, setSize] = useState(initialParams.size ?? 20);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [params, setParams] = useState<Record<string, any>>(initialParams);

  const fetchData = useCallback(async () => {
    let mounted = true;
    setLoading(true);
    setError(null);
    try {
      const res = await fetcher({ page, size, ...params });
      if (!mounted) return;
      setData(res.content ?? []);
      setTotalElements(res.totalElements ?? 0);
      setTotalPages(res.totalPages ?? 0);
    } catch (err: any) {
      if (!mounted) return;
      setError(err?.message || "Failed to load");
    } finally {
      if (mounted) setLoading(false);
    }
    return () => { mounted = false; };
  }, [fetcher, page, size, params]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  return {
    data, setData,
    page, setPage,
    size, setSize,
    totalElements, totalPages,
    loading, error,
    params, setParams,
    refresh: fetchData,
  };
}
