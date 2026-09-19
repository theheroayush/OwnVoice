import type { Metadata, Viewport } from "next";
import { Plus_Jakarta_Sans, Caveat } from "next/font/google";
import "./globals.css";

const sans = Plus_Jakarta_Sans({
  subsets: ["latin"],
  variable: "--font-sans",
  display: "swap",
});

const caveat = Caveat({
  subsets: ["latin"],
  variable: "--font-handwriting",
  display: "swap",
});

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  maximumScale: 1,
};

export const metadata: Metadata = {
  title: "Own Voice — Voice-First AI Workspace | One-Time Purchase & BYOK",
  description:
    "Speak naturally, get finished work. Own Voice is the private, voice-first AI productivity application where you own the software and bring your own API keys.",
  keywords: [
    "AI voice typing",
    "voice to text AI",
    "AI dictation software",
    "Whisper alternative",
    "BYOK AI tool",
    "one time purchase AI",
  ],
  authors: [{ name: "Own Voice Team" }],
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" className="dark scroll-smooth">
      <body className={`${sans.variable} ${caveat.variable} font-sans min-h-screen bg-[#07080b] text-[#EDEDED] antialiased selection:bg-blue-500/20 selection:text-blue-200`}>
        {children}
      </body>
    </html>
  );
}

