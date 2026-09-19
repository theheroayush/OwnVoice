import React from "react";
import { redirect } from "next/navigation";
import { getCurrentUser } from "@/lib/auth";
import prisma from "@/lib/db";
import { Sidebar } from "@/components/app/Sidebar";
import { AppNavbar } from "@/components/app/AppNavbar";
import { Toast } from "@/components/ui/Toast";

export default async function AppLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const user = await getCurrentUser();

  if (!user) {
    redirect("/login");
  }

  // Check connected providers
  const userProvider = await prisma.userProvider.findFirst({
    where: { userId: user.id, isActive: true },
  });

  return (
    <div className="flex h-screen w-screen overflow-hidden bg-[#09090b] text-[#EDEDED]">
      <Sidebar userRole={user.role} userEmail={user.email} />
      <div className="flex-1 flex flex-col min-w-0 h-full overflow-y-auto pb-16 md:pb-0">
        <AppNavbar
          hasProvider={!!userProvider}
          activeProviderName={userProvider?.provider}
          hasLicense={user.hasActiveLicense}
        />
        <main className="flex-1 p-4 sm:p-8 max-w-5xl w-full mx-auto">
          {children}
        </main>
      </div>
      <Toast />
    </div>
  );
}
