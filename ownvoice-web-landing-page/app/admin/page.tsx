"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import { ShieldCheck, Users, CreditCard, Sparkles, AlertCircle, RefreshCw, KeyRound, ArrowLeft } from "lucide-react";
import { useAppStore } from "@/lib/store";

interface AdminStats {
  totalUsers: number;
  activeLicenses: number;
  totalPurchases: number;
  grossRevenue: number;
  totalGenerations: number;
  totalRecordings: number;
  recentUsers: any[];
  providerStats: any[];
}

export default function AdminPage() {
  const { showToast } = useAppStore();
  const [stats, setStats] = useState<AdminStats | null>(null);
  const [users, setUsers] = useState<any[]>([]);
  const [licenses, setLicenses] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<"overview" | "users" | "licenses">("overview");

  const loadAdminData = async () => {
    setLoading(true);
    try {
      const [statsRes, usersRes, licensesRes] = await Promise.all([
        fetch("/api/admin/stats"),
        fetch("/api/admin/users"),
        fetch("/api/admin/licenses"),
      ]);

      const [statsData, usersData, licensesData] = await Promise.all([
        statsRes.json(),
        usersRes.json(),
        licensesRes.json(),
      ]);

      if (statsData.success) setStats(statsData.stats);
      if (usersData.success) setUsers(usersData.users);
      if (licensesData.success) setLicenses(licensesData.licenses);
    } catch (err) {
      console.error("Admin load error:", err);
      showToast("Failed to load admin data.", "error");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAdminData();
  }, []);

  const handleToggleUserStatus = async (userId: string, currentStatus: string) => {
    const nextStatus = currentStatus === "ACTIVE" ? "SUSPENDED" : "ACTIVE";
    try {
      const res = await fetch("/api/admin/users", {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ userId, status: nextStatus }),
      });
      if (res.ok) {
        showToast(`User ${nextStatus.toLowerCase()}.`, "info");
        await loadAdminData();
      }
    } catch {
      showToast("Failed to update user.", "error");
    }
  };

  const handleToggleLicenseStatus = async (licenseId: string, status: string) => {
    try {
      const res = await fetch("/api/admin/licenses", {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ licenseId, status }),
      });
      if (res.ok) {
        showToast(`License marked as ${status}.`, "info");
        await loadAdminData();
      }
    } catch {
      showToast("Failed to update license.", "error");
    }
  };

  return (
    <div className="min-h-screen bg-[#09090b] text-white p-6 sm:p-10 space-y-8">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="space-y-1">
          <Link href="/app" className="inline-flex items-center gap-1.5 text-xs text-zinc-400 hover:text-white mb-2">
            <ArrowLeft className="w-3.5 h-3.5" />
            Back to Voice Workspace
          </Link>
          <h1 className="text-2xl font-bold tracking-tight flex items-center gap-2.5">
            <ShieldCheck className="w-6 h-6 text-blue-400" />
            Admin Operations Panel
          </h1>
          <p className="text-xs text-zinc-400">
            System overview, users, license statuses, and revenue telemetry.
          </p>
        </div>

        {/* Tab Navigation */}
        <div className="flex p-1 rounded-xl bg-zinc-900 border border-white/10 text-xs font-semibold">
          {(["overview", "users", "licenses"] as const).map((tab) => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              className={`px-3.5 py-1.5 rounded-lg capitalize transition-all ${
                activeTab === tab ? "bg-blue-600 text-white shadow-sm" : "text-zinc-400 hover:text-white"
              }`}
            >
              {tab}
            </button>
          ))}
        </div>
      </div>

      {/* KPI Overview Cards (PRD Section 32) */}
      {stats && activeTab === "overview" && (
        <div className="space-y-8">
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
            <div className="p-5 rounded-2xl bg-[#121215] border border-white/10 space-y-2">
              <span className="text-xs font-semibold text-zinc-400 uppercase tracking-wider flex items-center gap-2">
                <Users className="w-4 h-4 text-blue-400" />
                Total Users
              </span>
              <div className="text-2xl font-extrabold text-white">{stats.totalUsers}</div>
            </div>

            <div className="p-5 rounded-2xl bg-[#121215] border border-white/10 space-y-2">
              <span className="text-xs font-semibold text-zinc-400 uppercase tracking-wider flex items-center gap-2">
                <ShieldCheck className="w-4 h-4 text-emerald-400" />
                Active Licenses
              </span>
              <div className="text-2xl font-extrabold text-emerald-400">{stats.activeLicenses}</div>
            </div>

            <div className="p-5 rounded-2xl bg-[#121215] border border-white/10 space-y-2">
              <span className="text-xs font-semibold text-zinc-400 uppercase tracking-wider flex items-center gap-2">
                <CreditCard className="w-4 h-4 text-amber-400" />
                Gross Purchases
              </span>
              <div className="text-2xl font-extrabold text-white">
                ${(stats.grossRevenue / 100).toFixed(2)}
              </div>
            </div>

            <div className="p-5 rounded-2xl bg-[#121215] border border-white/10 space-y-2">
              <span className="text-xs font-semibold text-zinc-400 uppercase tracking-wider flex items-center gap-2">
                <Sparkles className="w-4 h-4 text-purple-400" />
                Total Generations
              </span>
              <div className="text-2xl font-extrabold text-white">{stats.totalGenerations}</div>
            </div>
          </div>

          {/* Provider Breakdown */}
          <div className="p-6 rounded-2xl bg-[#121215] border border-white/10 space-y-4">
            <h3 className="text-sm font-semibold text-white uppercase tracking-wider">
              BYOK Provider Invocations
            </h3>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
              {stats.providerStats.map((item: any) => (
                <div key={item.providerUsed} className="p-4 rounded-xl bg-zinc-900/50 border border-white/5">
                  <span className="text-xs font-mono uppercase text-zinc-400 block">{item.providerUsed}</span>
                  <span className="text-xl font-bold text-white">{item._count.id} runs</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* Users Tab */}
      {activeTab === "users" && (
        <div className="rounded-2xl bg-[#121215] border border-white/10 overflow-hidden shadow-2xl">
          <div className="p-4 border-b border-white/5 font-semibold text-sm">Registered Users</div>
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-zinc-300">
              <thead className="bg-zinc-900/50 text-zinc-400 uppercase font-mono text-[10px]">
                <tr>
                  <th className="p-3.5">User</th>
                  <th className="p-3.5">Status</th>
                  <th className="p-3.5">Role</th>
                  <th className="p-3.5">Licenses</th>
                  <th className="p-3.5">Generations</th>
                  <th className="p-3.5">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5">
                {users.map((u) => (
                  <tr key={u.id} className="hover:bg-white/[0.02]">
                    <td className="p-3.5 font-medium text-white">{u.email}</td>
                    <td className="p-3.5">
                      <span
                        className={`px-2 py-0.5 rounded-full text-[10px] font-mono ${
                          u.status === "ACTIVE"
                            ? "bg-emerald-950/60 text-emerald-400 border border-emerald-500/30"
                            : "bg-rose-950/60 text-rose-400 border border-rose-500/30"
                        }`}
                      >
                        {u.status}
                      </span>
                    </td>
                    <td className="p-3.5 font-mono text-[11px] text-zinc-400">{u.role}</td>
                    <td className="p-3.5 font-mono text-[11px] text-zinc-400">{u.licenses?.length || 0}</td>
                    <td className="p-3.5 font-mono text-[11px] text-zinc-400">{u._count?.generations || 0}</td>
                    <td className="p-3.5">
                      <button
                        onClick={() => handleToggleUserStatus(u.id, u.status)}
                        className="text-[11px] px-2.5 py-1 rounded bg-zinc-800 hover:bg-zinc-700 text-zinc-200"
                      >
                        {u.status === "ACTIVE" ? "Suspend" : "Activate"}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Licenses Tab */}
      {activeTab === "licenses" && (
        <div className="rounded-2xl bg-[#121215] border border-white/10 overflow-hidden shadow-2xl">
          <div className="p-4 border-b border-white/5 font-semibold text-sm">Issued Lifetime Licenses</div>
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-zinc-300">
              <thead className="bg-zinc-900/50 text-zinc-400 uppercase font-mono text-[10px]">
                <tr>
                  <th className="p-3.5">License Key</th>
                  <th className="p-3.5">Assigned User</th>
                  <th className="p-3.5">Status</th>
                  <th className="p-3.5">Activated At</th>
                  <th className="p-3.5">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5">
                {licenses.map((lic) => (
                  <tr key={lic.id} className="hover:bg-white/[0.02]">
                    <td className="p-3.5 font-mono font-medium text-white">{lic.licenseKey}</td>
                    <td className="p-3.5 text-zinc-400">{lic.user?.email || "Unassigned"}</td>
                    <td className="p-3.5">
                      <span
                        className={`px-2 py-0.5 rounded-full text-[10px] font-mono ${
                          lic.status === "ACTIVE"
                            ? "bg-emerald-950/60 text-emerald-400 border border-emerald-500/30"
                            : "bg-rose-950/60 text-rose-400 border border-rose-500/30"
                        }`}
                      >
                        {lic.status}
                      </span>
                    </td>
                    <td className="p-3.5 font-mono text-[11px] text-zinc-500">
                      {new Date(lic.activatedAt).toLocaleDateString()}
                    </td>
                    <td className="p-3.5 flex items-center gap-1.5">
                      {lic.status === "ACTIVE" ? (
                        <button
                          onClick={() => handleToggleLicenseStatus(lic.id, "REVOKED")}
                          className="text-[11px] px-2 py-0.5 rounded bg-rose-950/60 text-rose-400 border border-rose-500/30"
                        >
                          Revoke
                        </button>
                      ) : (
                        <button
                          onClick={() => handleToggleLicenseStatus(lic.id, "ACTIVE")}
                          className="text-[11px] px-2 py-0.5 rounded bg-emerald-950/60 text-emerald-400 border border-emerald-500/30"
                        >
                          Restore
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
