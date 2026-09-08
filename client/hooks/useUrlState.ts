"use client";
import { useMemo, useCallback, useRef } from "react";
import { useRouter, useSearchParams } from "next/navigation";

export function useUrlState<T extends Record<string, string>>(
  initialState: T
): [T, (updates: Partial<T>) => void] {
  const router = useRouter();
  const searchParams = useSearchParams();
  const searchParamsRef = useRef(searchParams);
  searchParamsRef.current = searchParams;

  const state = useMemo(() => {
    const s = { ...initialState } as T;
    (Object.keys(initialState) as (keyof T)[]).forEach((key) => {
      const value = searchParamsRef.current.get(key as string);
      if (value !== null) s[key] = value as T[keyof T];
    });
    return s;
  }, [searchParams, initialState]);

  const setState = useCallback((updates: Partial<T>) => {
    const params = new URLSearchParams(searchParamsRef.current.toString());
    Object.entries(updates).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== "") params.set(key, value as string);
      else params.delete(key);
    });
    router.replace(`?${params.toString()}` as any);
  }, [router]);

  return [state, setState];
}
