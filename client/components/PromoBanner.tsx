import Link from "next/link";

export default function PromoBanner() {
  return (
    <section className="w-full max-w-[1440px] mx-auto px-margin-mobile md:px-margin-desktop py-xl">
      <div className="relative rounded-xl overflow-hidden bg-[#0c0a09] text-[#fafaf9] flex flex-col md:flex-row items-center justify-between p-8 md:px-10 md:py-10 lg:px-12 lg:py-12 shadow-md isolate border border-[#b45309]/20">
        <div className="absolute top-0 right-0 w-64 h-64 bg-[#b45309] rounded-full mix-blend-multiply blur-[40px] opacity-20 translate-x-1/3 -translate-y-1/3 pointer-events-none" />
        <div className="absolute bottom-0 left-0 w-48 h-48 bg-[#b45309] rounded-full mix-blend-multiply blur-[30px] opacity-10 -translate-x-1/4 translate-y-1/4 pointer-events-none" />
        <div className="absolute inset-0 bg-gradient-to-br from-[#0c0a09] via-[#0c0a09] to-[#1c1917] -z-10" />

        <div className="z-10 text-center md:text-left mb-6 md:mb-0">
          <h2 className="font-bold text-[26px] md:text-[30px] lg:text-[32px] leading-tight mb-1 tracking-tight">
            Summer Collection
          </h2>
          <p className="font-medium text-[14px] md:text-[16px] text-[#a8a29e]">
            Up to 40% Off Selected Items
          </p>
        </div>

        <div className="z-10">
          <Link
            href="/new-arrivals"
            className="inline-flex items-center justify-center bg-[#b45309] text-[#fafaf9] font-bold text-[13px] tracking-wide py-3 px-7 rounded-full hover:bg-[#92400e] transition-colors shadow-sm whitespace-nowrap"
          >
            Shop Collection
          </Link>
        </div>
      </div>
    </section>
  );
}
