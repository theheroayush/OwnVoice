import React from "react";
import { getCurrentUser } from "@/lib/auth";
import prisma from "@/lib/db";
import { VoiceRecorder } from "@/components/app/VoiceRecorder";
import { IntentSelector } from "@/components/app/IntentSelector";
import { OutputDisplay } from "@/components/app/OutputDisplay";
import Link from "next/link";
import { Clock, ArrowUpRight, Sparkles, MessageSquare, Mail } from "lucide-react";

export const dynamic = "force-dynamic";

export default async function AppPage() {
  const user = await getCurrentUser();

  // Fetch recent generations
  const recentGenerations = user
    ? await prisma.generation.findMany({
        where: { userId: user.id },
        orderBy: { createdAt: "desc" },
        take: 4,
        select: {
          id: true,
          title: true,
          intent: true,
          wordCount: true,
          createdAt: true,
        },
      })
    : [];

  const formatTimestamp = (date: Date) => {
    const d = new Date(date);
    return d.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
  };

  return (
    <div className="flex flex-col items-center justify-center space-y-8 py-4 sm:py-8">
      {/* Title & Intent Picker */}
      <div className="text-center space-y-2">
        <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-white">
          What do you want to say?
        </h1>
        <p className="text-xs sm:text-sm text-zinc-400">
          Speak naturally in your own voice. Own Voice handles formatting, cleanup, and tone.
        </p>
      </div>

      <IntentSelector />

      {/* Primary Voice Recorder */}
      <div className="w-full flex justify-center py-4">
        <VoiceRecorder />
      </div>

      {/* Output Display Card */}
      <OutputDisplay />

      {/* Recent Activity (PRD Section 21) */}
      {recentGenerations.length > 0 && (
        <div className="w-full max-w-2xl mx-auto space-y-3 pt-6 border-t border-white/5">
          <div className="flex items-center justify-between text-xs text-zinc-400">
            <span className="font-semibold uppercase tracking-wider text-zinc-300 flex items-center gap-1.5">
              <Clock className="w-3.5 h-3.5 text-zinc-500" />
              Recent Voice Notes
            </span>
            <Link
              href="/app/history"
              className="hover:text-blue-400 flex items-center gap-0.5 transition-colors font-medium"
            >
              View all
              <ArrowUpRight className="w-3 h-3" />
            </Link>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
            {recentGenerations.map((gen) => (
              <Link
                key={gen.id}
                href={`/app/history?id=${gen.id}`}
                className="flex items-center justify-between p-3 rounded-xl bg-zinc-900/40 border border-white/5 hover:border-white/15 hover:bg-zinc-800/50 transition-all group"
              >
                <div className="truncate pr-2">
                  <span className="text-xs font-medium text-zinc-200 group-hover:text-blue-400 block truncate transition-colors">
                    {gen.title}
                  </span>
                  <span className="text-[11px] text-zinc-500 font-mono">
                    {gen.intent} • {gen.wordCount} words
                  </span>
                </div>
                <span className="text-[11px] font-mono text-zinc-500 shrink-0">
                  {formatTimestamp(gen.createdAt)}
                </span>
              </Link>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
