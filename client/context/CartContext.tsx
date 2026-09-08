"use client";

import { createContext, useContext, ReactNode } from "react";
import { useCart as useCartHook } from "@/hooks/useCart";

type CartContextType = ReturnType<typeof useCartHook>;

const CartContext = createContext<CartContextType | null>(null);

export function CartProvider({ children }: { children: ReactNode }) {
  const cart = useCartHook();
  return <CartContext.Provider value={cart}>{children}</CartContext.Provider>;
}

export function useCart() {
  const ctx = useContext(CartContext);
  if (!ctx) throw new Error("useCart must be used within CartProvider");
  return ctx;
}