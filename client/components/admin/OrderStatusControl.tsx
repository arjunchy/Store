"use client";

import { useEffect, useRef, useState, useLayoutEffect } from "react";
import { createPortal } from "react-dom";
import { isValidOrderTransition, isValidDeliveryTransition, getDisabledReason } from "@/lib/statusConfig";

type Props = {
  orderId: string;
  current: string;
  allowed: string[];
  allStatuses: string[];
  onChange: (orderId: string, newStatus: string) => Promise<void>;
  updatingId: string | null;
  variant: "order" | "delivery";
};

export function StatusControl({ orderId, current, allowed, allStatuses, onChange, updatingId, variant }: Props) {
  const [open, setOpen] = useState(false);
  const [confirmCancel, setConfirmCancel] = useState(false);
  const buttonRef = useRef<HTMLButtonElement>(null);
  const menuRef = useRef<HTMLDivElement>(null);
  const [pos, setPos] = useState<{ top: number; left: number; width: number; placement: "top" | "bottom" }>({ top: 0, left: 0, width: 190, placement: "bottom" });
  const [mounted, setMounted] = useState(false);
  const isUpdating = updatingId === orderId;

  useEffect(() => setMounted(true), []);

  const isTerminal = allowed.length === 0;
  const isOrder = variant === "order";

  const badgeColor =
    current === "PROCESSING" || current === "PENDING"
      ? "bg-[#ffedd5] text-[#b45309] border-[#ffedd5]"
      : current === "CONFIRMED" || current === "SHIPPING"
      ? "bg-[#fef3c7] text-[#b45309] border-[#b45309]/20"
      : current === "COMPLETED" || current === "COLLECTED" || current === "PAID"
      ? "bg-[#15803d]/15 text-[#15803d] border-[#15803d]/20"
      : current === "CANCELED" || current === "CANCELLED" || current === "EXPIRED" || current === "RETURNED"
      ? "bg-[#fee2e2] text-[#b91c1c] border-[#fecaca]"
      : current === "PLACED"
      ? "bg-[#fafaf9] text-[#57534e] border-[#d6d3d1]"
      : "bg-[#fafaf9] text-[#57534e] border-[#d6d3d1]";

  const updatePosition = () => {
    if (!buttonRef.current || !open) return;
    const rect = buttonRef.current.getBoundingClientRect();
    const menuWidth = 210;
    const headerH = 56;
    const footerH = 28;
    const itemH = 34;
    const visibleCount = confirmCancel ? 0 : 1 + allStatuses.length;
    const estimatedH = confirmCancel ? 160 : headerH + visibleCount * itemH + footerH + 8;
    const spaceBelow = window.innerHeight - rect.bottom;
    const spaceAbove = rect.top;
    const placeBelow = spaceBelow >= estimatedH + 8 || spaceBelow >= spaceAbove;
    let top = placeBelow ? rect.bottom + 6 : rect.top - estimatedH - 6;
    let left = rect.left;
    if (left + menuWidth > window.innerWidth - 8) {
      left = rect.right - menuWidth;
    }
    if (left < 8) left = 8;
    if (top < 8) top = 8;
    if (top + estimatedH > window.innerHeight - 8) {
      top = Math.max(8, window.innerHeight - estimatedH - 8);
    }
    setPos({ top, left, width: menuWidth, placement: placeBelow ? "bottom" : "top" });

    if (menuRef.current) {
      const actualH = menuRef.current.offsetHeight;
      if (Math.abs(actualH - estimatedH) > 20) {
        let correctedTop = placeBelow ? rect.bottom + 6 : rect.top - actualH - 6;
        if (correctedTop < 8) correctedTop = 8;
        if (correctedTop + actualH > window.innerHeight - 8) correctedTop = window.innerHeight - actualH - 8;
        setPos({ top: correctedTop, left, width: menuWidth, placement: placeBelow ? "bottom" : "top" });
      }
    }
  };

  useLayoutEffect(() => {
    if (open) {
      updatePosition();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, confirmCancel, allStatuses.length]);

  useEffect(() => {
    if (!open) return;
    const onScroll = () => updatePosition();
    const onResize = () => updatePosition();
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        setOpen(false);
        setConfirmCancel(false);
      }
    };
    window.addEventListener("scroll", onScroll, true);
    window.addEventListener("resize", onResize);
    document.addEventListener("keydown", onKey);
    return () => {
      window.removeEventListener("scroll", onScroll, true);
      window.removeEventListener("resize", onResize);
      document.removeEventListener("keydown", onKey);
    };
  }, [open]);

  useEffect(() => {
    if (!open) return;
    const handler = (e: MouseEvent) => {
      const target = e.target as Node;
      const inButton = buttonRef.current?.contains(target);
      const inMenu = menuRef.current?.contains(target);
      if (!inButton && !inMenu) {
        setOpen(false);
        setConfirmCancel(false);
      }
    };
    const t = setTimeout(() => document.addEventListener("mousedown", handler), 0);
    return () => {
      clearTimeout(t);
      document.removeEventListener("mousedown", handler);
    };
  }, [open]);

  const handleSelect = async (next: string) => {
    if (next === current) {
      setOpen(false);
      return;
    }
    if (next === "CANCELED" || next === "CANCELLED") {
      setConfirmCancel(true);
      return;
    }
    const prevOpen = open;
    setOpen(false);
    try {
      await onChange(orderId, next);
    } catch {
    }
  };

  const handleConfirmCancel = async () => {
    setConfirmCancel(false);
    setOpen(false);
    await onChange(orderId, "CANCELED");
  };

  return (
    <>
      <button
        ref={buttonRef}
        onClick={() => !isUpdating && setOpen((v) => !v)}
        disabled={isUpdating}
        className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full border text-[11px] font-semibold transition-colors disabled:opacity-50 ${badgeColor} ${open ? "ring-2 ring-primary/20" : ""}`}
        aria-haspopup="menu"
        aria-expanded={open}
        title={isTerminal ? "Terminal — click to see all statuses" : "Click to see all statuses"}
      >
        {isUpdating ? <span className="material-symbols-outlined animate-spin text-[12px]">progress_activity</span> : null}
        {current}
        <span className={`material-symbols-outlined text-[14px] transition-transform ${open ? "rotate-180" : ""}`}>expand_more</span>
      </button>

      {mounted && open && createPortal(
        <div
          ref={menuRef}
          style={{
            position: "fixed",
            top: pos.top,
            left: pos.left,
            width: pos.width,
            zIndex: 9999,
          }}
          className="bg-white border border-[#d6d3d1] shadow-xl rounded-xl overflow-hidden animate-fade-in-up"
          role="menu"
        >
          <div className="px-3 py-2 border-b border-[#e7e5e4] bg-[#fafaf9]">
            <p className="text-[11px] font-semibold tracking-widest text-[#57534e] uppercase">{isOrder ? "Order status" : "Delivery status"}</p>
            <p className="text-[11px] text-[#57534e]">Current: <span className="font-semibold text-[#1c1917]">{current}</span></p>
          </div>

          {!confirmCancel ? (
            <>
              <div className="py-1 max-h-[260px] overflow-y-auto">
                {allStatuses.map((s) => {
                  const isCurrent = s === current;
                  const isValid = isCurrent || (isOrder ? isValidOrderTransition(current, s) : isValidDeliveryTransition(current, s));
                  const isDestructive = s === "CANCELED" || s === "CANCELLED" || s === "RETURNED";
                  const reason = getDisabledReason(current, s);
                  return (
                    <button
                      key={s}
                      onClick={() => !isCurrent && isValid && handleSelect(s)}
                      disabled={!isValid && !isCurrent}
                      title={!isValid && !isCurrent ? reason : undefined}
                      className={`w-full text-left px-3 py-2 text-[12px] flex items-center gap-2 transition-colors ${
                        isCurrent
                          ? "bg-[#fef3c7]/20 text-[#b45309]"
                          : isValid
                            ? isDestructive
                              ? "text-[#b91c1c] hover:bg-[#fee2e2]/20"
                              : "text-[#1c1917] hover:bg-[#fafaf9]"
                            : "text-[#57534e]/40 cursor-not-allowed"
                      }`}
                      role="menuitem"
                    >
                      <span className="material-symbols-outlined text-[14px]">
                        {isCurrent ? "check" : isValid ? (isDestructive ? "cancel" : "arrow_forward") : "block"}
                      </span>
                      <span className="flex-1 font-medium">{s}</span>
                      {isCurrent && <span className="text-[10px] bg-[#b45309] text-white px-1.5 py-0.5 rounded-full">current</span>}
                      {!isValid && !isCurrent && <span className="text-[10px] text-[#57534e]/60">disabled</span>}
                    </button>
                  );
                })}
              </div>
              <div className="px-3 py-1.5 bg-[#fafaf9] border-t border-[#e7e5e4] text-[10px] text-[#57534e]">
                Valid next: {allowed.length ? allowed.join(", ") : "— terminal"}
              </div>
            </>
          ) : (
            <div className="p-3 bg-[#fee2e2]/10">
              <p className="text-[12px] font-semibold text-[#b91c1c]">Cancel this order?</p>
              <p className="text-[11px] text-[#57534e] mt-1">Order <span className="font-mono text-[#1c1917]">{orderId.slice(0, 8)}</span> — This cannot be undone. Payment will move to REFUNDING.</p>
              <div className="flex gap-2 mt-3">
                <button onClick={() => setConfirmCancel(false)} className="flex-1 px-3 py-1.5 rounded-lg bg-white border border-[#d6d3d1] text-[12px] font-medium">
                  Keep
                </button>
                <button onClick={handleConfirmCancel} className="flex-1 px-3 py-1.5 rounded-lg bg-error text-on-error text-[12px] font-semibold">
                  Cancel Order
                </button>
              </div>
            </div>
          )}
        </div>,
        document.body
      )}
    </>
  );
}
