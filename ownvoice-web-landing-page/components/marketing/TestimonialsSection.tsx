"use client";

export function TestimonialsSection() {
  const testimonials = [
    {
      quote: "I use it daily. So much faster than typing, and I love that I own it instead of paying every month.",
      name: "Rohan S.",
      role: "Founder",
      avatarBg: "bg-blue-600/30 text-blue-300",
      initials: "RS",
    },
    {
      quote: "The BYOK model is brilliant. I can use my own keys and choose the model I want.",
      name: "Priya M.",
      role: "Product Designer",
      avatarBg: "bg-purple-600/30 text-purple-300",
      initials: "PM",
    },
    {
      quote: "Perfect for notes, emails and content. Easily one of the most useful tools I've bought.",
      name: "Karan V.",
      role: "Entrepreneur",
      avatarBg: "bg-amber-600/30 text-amber-300",
      initials: "KV",
    },
  ];

  return (
    <section className="py-24 px-4 sm:px-6 relative border-t border-white/[0.04]">
      <div className="max-w-6xl mx-auto space-y-12">
        <div className="text-center space-y-3">
          <div className="inline-flex items-center px-3 py-1 rounded-full bg-blue-950/60 border border-blue-500/30 text-blue-400 text-xs font-mono tracking-wider uppercase">
            Loved by early users
          </div>
          <h2 className="text-3xl sm:text-4xl font-bold tracking-tight text-white">
            Simple. Powerful. A game changer.
          </h2>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {testimonials.map((item, idx) => (
            <div
              key={idx}
              className="rounded-2xl border border-white/[0.08] bg-[#0c0f16]/90 p-7 space-y-6 shadow-xl flex flex-col justify-between hover:border-blue-500/25 transition-all duration-300"
            >
              <p className="text-sm sm:text-base text-zinc-300 leading-relaxed font-normal">
                &ldquo;{item.quote}&rdquo;
              </p>

              <div className="flex items-center gap-3 pt-2 border-t border-white/[0.06]">
                <div className={`w-10 h-10 rounded-full ${item.avatarBg} flex items-center justify-center font-bold text-xs border border-white/10`}>
                  {item.initials}
                </div>
                <div>
                  <h4 className="text-sm font-semibold text-white">{item.name}</h4>
                  <p className="text-xs text-zinc-500">{item.role}</p>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
