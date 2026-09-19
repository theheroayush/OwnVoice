"use client";

import React from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { Mic, History, Settings, ShieldAlert, LogOut, Sparkles } from "lucide-react";

interface SidebarProps {
  userRole?: string;
  userEmail?: string;
}

export function Sidebar({ userRole, userEmail }: SidebarProps) {
  const pathname = usePathname();
  const router = useRouter();

  const handleLogout = async () => {
    try {
      await fetch("/api/auth/logout", { method: "POST" });
      router.push("/login");
      router.refresh();
    } catch (err) {
      console.error("Logout error:", err);
    }
  };

  const navItems = [
    { label: "New Voice", href: "/app", icon: Mic },
    { label: "History", href: "/app/history", icon: History },
    { label: "Settings", href: "/app/settings", icon: Settings },
  ];

  if (userRole === "ADMIN") {
    navItems.push({ label: "Admin Panel", href: "/admin", icon: ShieldAlert });
  }

  return (
    <>
      {/* Desktop Sidebar */}
      <aside className="hidden md:flex flex-col justify-between w-60 shrink-0 h-screen border-r border-white/5 bg-[#0a0a0d] p-4">
        <div className="space-y-6">
          {/* Logo */}
          <Link href="/app" className="flex items-center gap-2.5 px-2 py-1 group">
            <div className="w-8 h-8 rounded-lg bg-blue-600/20 border border-blue-500/30 flex items-center justify-center text-blue-400 group-hover:bg-blue-600/30 transition-all">
              <Mic className="w-4 h-4" />
            </div>
            <div>
              <span className="font-semibold text-sm tracking-tight text-white block">Own Voice</span>
              <span className="text-[10px] text-zinc-500 font-mono block">Personal Edition</span>
            </div>
          </Link>

          {/* Navigation Links */}
          <nav className="space-y-1">
            {navItems.map((item) => {
              const Icon = item.icon;
              const isActive = pathname === item.href || (item.href !== "/app" && pathname.startsWith(item.href));
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  className={`flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-all ${
                    isActive
                      ? "bg-blue-600/15 text-blue-400 border border-blue-500/30"
                      : "text-zinc-400 hover:text-white hover:bg-white/5"
                  }`}
                >
                  <Icon className="w-4 h-4" />
                  {item.label}
                </Link>
              );
            })}
          </nav>
        </div>

        {/* User Info & Sign Out */}
        <div className="pt-4 border-t border-white/5 space-y-2">
          {userEmail && (
            <div className="px-3 py-1.5 text-xs text-zinc-500 truncate">
              Signed in as:
              <span className="block text-zinc-300 font-medium truncate">{userEmail}</span>
            </div>
          )}
          <button
            onClick={handleLogout}
            className="w-full flex items-center gap-2.5 px-3 py-2 rounded-lg text-xs font-medium text-zinc-400 hover:text-rose-400 hover:bg-rose-500/10 transition-colors"
          >
            <LogOut className="w-3.5 h-3.5" />
            Sign out
          </button>
        </div>
      </aside>

      {/* Mobile Bottom Navigation (PRD Section 22) */}
      <nav className="md:hidden fixed bottom-0 left-0 right-0 z-40 bg-[#0c0c0f]/95 border-t border-white/10 backdrop-blur-md px-4 py-2 flex items-center justify-around">
        <Link
          href="/app"
          className={`flex flex-col items-center gap-1 text-[11px] font-medium py-1 px-3 rounded-lg ${
            pathname === "/app" ? "text-blue-400" : "text-zinc-400"
          }`}
        >
          <Mic className="w-5 h-5" />
          <span>Speak</span>
        </Link>
        <Link
          href="/app/history"
          className={`flex flex-col items-center gap-1 text-[11px] font-medium py-1 px-3 rounded-lg ${
            pathname.startsWith("/app/history") ? "text-blue-400" : "text-zinc-400"
          }`}
        >
          <History className="w-5 h-5" />
          <span>History</span>
        </Link>
        <Link
          href="/app/settings"
          className={`flex flex-col items-center gap-1 text-[11px] font-medium py-1 px-3 rounded-lg ${
            pathname.startsWith("/app/settings") ? "text-blue-400" : "text-zinc-400"
          }`}
        >
          <Settings className="w-5 h-5" />
          <span>Settings</span>
        </Link>
        {userRole === "ADMIN" && (
          <Link
            href="/admin"
            className={`flex flex-col items-center gap-1 text-[11px] font-medium py-1 px-3 rounded-lg ${
              pathname.startsWith("/admin") ? "text-blue-400" : "text-zinc-400"
            }`}
          >
            <ShieldAlert className="w-5 h-5" />
            <span>Admin</span>
          </Link>
        )}
      </nav>
    </>
  );
}
