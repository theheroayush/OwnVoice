"use client";

import React from "react";
import Link from "next/link";
import { KeyRound, ShieldCheck, AlertTriangle } from "lucide-react";

interface AppNavbarProps {
  hasProvider: boolean;
  activeProviderName?: string;
  hasLicense: boolean;
}

export function AppNavbar({ hasProvider, activeProviderName, hasLicense }: AppNavbarProps) {
  return (
    <header className="h-14 border-b border-white/5 px-4 sm:px-6 flex items-center justify-between bg-[#09090b]/50 backdrop-blur-sm">
      <div className="flex items-center gap-3 text-xs">
        {hasProvider ? (
          <div className="flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-emerald-950/40 border border-emerald-500/20 text-emerald-400 font-medium">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
            <span>AI Connected: {activeProviderName?.toUpperCase() || "BYOK"}</span>
          </div>
        ) : (
          <Link
            href="/app/settings"
            className="flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-amber-950/40 border border-amber-500/30 text-amber-400 font-medium hover:bg-amber-900/40 transition-colors"
          >
            <AlertTriangle className="w-3.5 h-3.5" />
            <span>No AI Provider Connected (Click to set up)</span>
          </Link>
        )}
      </div>

      <div className="flex items-center gap-3 text-xs">
        {hasLicense ? (
          <span className="hidden sm:flex items-center gap-1.5 text-zinc-400 font-mono text-[11px]">
            <ShieldCheck className="w-3.5 h-3.5 text-blue-400" />
            Lifetime License Active
          </span>
        ) : (
          <Link
            href="/pricing"
            className="flex items-center gap-1 text-xs font-semibold px-3 py-1 rounded-lg bg-blue-600 hover:bg-blue-500 text-white transition-colors"
          >
            Activate License
          </Link>
        )}
      </div>
    </header>
  );
}
