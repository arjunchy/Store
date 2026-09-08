"use client";
import { createContext, useContext, useEffect, useState, ReactNode } from "react";
import { getAdminStats } from "@/lib/admin";

type Alerts = { pending: number; outOfStock: number; total: number; loading: boolean };
const AdminAlertsContext = createContext<Alerts>({ pending: 0, outOfStock: 0, total: 0, loading: true });

export function AdminAlertsProvider({ children }: { children: ReactNode }) {
  const [alerts, setAlerts] = useState<Alerts>({ pending: 0, outOfStock: 0, total: 0, loading: true });
  useEffect(() => {
    let cancelled = false;
    getAdminStats()
      .then((s) => {
        if (!cancelled && s) setAlerts({ pending: s.pendingOrders, outOfStock: s.outOfStockProducts, total: s.pendingOrders + s.outOfStockProducts, loading: false });
        else if (!cancelled) setAlerts((p) => ({ ...p, loading: false }));
      })
      .catch(() => { if (!cancelled) setAlerts((p) => ({ ...p, loading: false })); });
    return () => { cancelled = true; };
  }, []);
  return <AdminAlertsContext.Provider value={alerts}>{children}</AdminAlertsContext.Provider>;
}

export function useAdminAlerts() {
  return useContext(AdminAlertsContext);
}
