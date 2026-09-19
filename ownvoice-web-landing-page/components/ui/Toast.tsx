"use client";

import React, { useEffect } from "react";
import { useAppStore } from "@/lib/store";
import { CheckCircle2, AlertCircle, Info, X } from "lucide-react";

export function Toast() {
  const { toast, hideToast } = useAppStore();

  useEffect(() => {
    if (!toast) return;
    const timer = setTimeout(() => {
      hideToast();
    }, 4000);
    return () => clearTimeout(timer);
  }, [toast, hideToast]);

  if (!toast) return null;

  return (
    <div className="fixed bottom-6 right-6 z-50 flex items-center gap-3 px-4 py-3 rounded-xl bg-[#18181b] border border-white/10 text-white shadow-2xl animate-fade-in">
      {toast.type === "success" && <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0" />}
      {toast.type === "error" && <AlertCircle className="w-5 h-5 text-rose-400 shrink-0" />}
      {toast.type === "info" && <Info className="w-5 h-5 text-blue-400 shrink-0" />}
      <span className="text-sm font-medium pr-2">{toast.message}</span>
      <button
        onClick={hideToast}
        className="text-zinc-400 hover:text-white p-1 rounded-md transition-colors"
      >
        <X className="w-4 h-4" />
      </button>
    </div>
  );
}
