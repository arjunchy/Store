import Image from "next/image";
import Link from "next/link";

export default function Hero() {
  return (
    <section className="relative w-full max-w-[1440px] mx-auto px-margin-mobile md:px-margin-desktop py-xl md:py-xxl overflow-hidden">
      <div className="absolute top-6 -left-12 w-[420px] h-[420px] bg-[#d6d3d1]/60 rounded-full blur-[60px] pointer-events-none" />
      <div className="absolute -bottom-24 right-12 w-[360px] h-[360px] bg-[#ffedd5]/40 rounded-full blur-[50px] pointer-events-none" />
      <div className="grid grid-cols-1 md:grid-cols-12 gap-gutter items-center min-h-[420px] md:min-h-[60vh] relative">
        <div className="md:col-span-5 z-10 relative md:-mr-12 lg:-mr-16 animate-fade-in-up">
          <div className="bg-white rounded-[20px] p-8 md:p-10 relative z-20 shadow-[0_16px_40px_rgba(12,10,9,0.08)] border border-[#d6d3d1] rotate-[0.15deg]">
            <p className="text-[11px] tracking-[0.14em] font-semibold text-[#a8a29e] uppercase mb-3">New season · curated</p>
            <h1 className="font-bold text-[36px] leading-[0.95] md:text-[46px] text-[#0c0a09] tracking-[-0.03em]">
              Discover
              <br />
              <span className="font-light italic tracking-tight">products you’ll</span>
              <br />
              truly love
            </h1>
            <p className="text-[14.5px] leading-relaxed text-[#57534e] mt-3.5 max-w-[34ch]">
              Thoughtfully sourced essentials — built to last, priced fairly, and chosen like a favourite shopkeeper would.
            </p>
            <div className="flex flex-wrap gap-3 mt-6">
              <Link href="/new-arrivals" className="inline-flex items-center justify-center bg-[#b45309] text-[#fafaf9] font-medium text-[13.5px] py-3 px-7 rounded-full hover:bg-[#92400e] transition-colors shadow-[0_8px_16px_rgba(180,83,9,0.25)]">
                Shop now
              </Link>
              <Link href="/catalog" className="inline-flex items-center justify-center bg-white text-[#0c0a09] border border-[#d6d3d1] font-medium text-[13.5px] py-3 px-7 rounded-full hover:border-[#b45309] hover:text-[#b45309] transition-colors">
                Browse categories
              </Link>
            </div>
            <div className="flex items-center gap-3 mt-5 text-[11px] text-[#a8a29e]">
              <span className="flex items-center gap-1.5"><span className="w-1.5 h-1.5 rounded-full bg-[#15803d]" /> In stock & ready</span>
              <span>·</span>
              <span>Free 30-day returns</span>
            </div>
          </div>
        </div>
        <div className="md:col-span-7 h-[380px] md:h-[620px] relative rounded-[20px] overflow-hidden bg-[#d6d3d1] order-first md:order-last animate-fade-in-up shadow-[0_16px_40px_rgba(12,10,9,0.10)] border border-[#b45309]/10" style={{ animationDelay: '0.12s' }}>
          <div className="absolute inset-0 bg-gradient-to-t from-black/20 via-transparent to-transparent z-10 pointer-events-none" />
          <Image
            src="https://images.unsplash.com/photo-1441986300917-64674bd600d8?q=80&w=1200&auto=format&fit=crop"
            alt="Curated lifestyle products"
            fill
            unoptimized
            priority
            className="object-cover"
            sizes="(max-width: 768px) 100vw, 58vw"
          />
          <div className="absolute bottom-3 left-3 z-20 flex items-center gap-2 bg-white/92 backdrop-blur rounded-full pl-1 pr-3 py-1 shadow-sm border border-[#d6d3d1]">
            <span className="w-7 h-7 rounded-full bg-[#b45309] text-white flex items-center justify-center"><span className="material-symbols-outlined text-[14px]" style={{ fontVariationSettings: "'FILL' 1" }}>star</span></span>
            <span className="text-[11px] font-semibold text-[#0c0a09]">4.9 · 2,400+ reviews</span>
          </div>
        </div>
      </div>
    </section>
  );
}
