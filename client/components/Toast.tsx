"use client";

import Link from "next/link";
import { useEffect } from "react";

export type ToastNotice = {
  type: "success" | "error";
  message: string;
  actionHref?: string;
  actionLabel?: string;
};

type Props = {
  notice: ToastNotice | null;
  onClose: () => void;
  duration?: number;
};

/**
 * Shared bottom-right toast for success + error notifications
 * (e.g. added-to-cart confirmations, out-of-stock errors).
 * Auto-dismisses; errors expose role="alert" for assistive tech.
 */
export default function Toast({ notice, onClose, duration = 3000 }: Props) {
  useEffect(() => {
    if (!notice) return;
    const t = setTimeout(onClose, notice.type === "error" ? Math.max(duration, 3500) : duration);
    return () => clearTimeout(t);
  }, [notice, duration, onClose]);

  const isError = notice?.type === "error";

  return (
    <div
      aria-live="polite"
      className={`fixed bottom-6 right-6 z-50 transition-all duration-300 ${
        notice ? "translate-y-0 opacity-100" : "translate-y-6 opacity-0 pointer-events-none"
      }`}
    >
      {notice && (
        <div
          role={isError ? "alert" : "status"}
          className={`rounded-xl px-4 py-3 shadow-xl flex items-center gap-3 text-[13px] font-medium max-w-[340px] ${
            isError ? "bg-[#b91c1c] text-white" : "bg-[#0c0a09] text-white"
          }`}
        >
          <span className="material-symbols-outlined text-[18px] shrink-0">
            {isError ? "error" : "check_circle"}
          </span>
          <span className="line-clamp-2 flex-1">{notice.message}</span>
          {notice.actionHref && (
            <Link
              href={notice.actionHref}
              className="ml-1 underline decoration-white/40 hover:decoration-white shrink-0"
            >
              {notice.actionLabel ?? "View"}
            </Link>
          )}
          <button
            onClick={onClose}
            aria-label="Dismiss notification"
            className="shrink-0 opacity-70 hover:opacity-100 transition-opacity"
          >
            <span className="material-symbols-outlined text-[16px]">close</span>
          </button>
        </div>
      )}
    </div>
  );
}
