import Link from "next/link";

export default function NotFound() {
  return (
    <div className="min-h-[70vh] flex flex-col items-center justify-center px-4 text-center bg-[#fafaf9]">
      <div className="w-24 h-24 rounded-full bg-[#fafaf9] flex items-center justify-center mb-6">
        <span className="material-symbols-outlined text-[#b45309] text-[44px]">search_off</span>
      </div>
      <h1 className="font-bold text-[36px] text-[#1c1917] tracking-tight">404 — Page not found</h1>
      <p className="text-[14px] text-[#57534e] mt-2 max-w-md">
        The page you&apos;re looking for doesn&apos;t exist, was moved, or you don&apos;t have permission. Check the URL or head back home.
      </p>
      <div className="flex gap-3 mt-6">
        <Link href="/" className="px-6 py-3 bg-[#b45309] text-white rounded-xl font-semibold text-[14px] hover:bg-[#92400e]">
          Go Home
        </Link>
        <Link href="/catalog" className="px-6 py-3 border border-[#d6d3d1] rounded-xl font-semibold text-[14px] hover:bg-[#fafaf9]">
          Browse Catalog
        </Link>
      </div>
      <p className="text-[12px] text-[#57534e] mt-8 font-mono">Try /catalog • /cart • /orders • /admin</p>
    </div>
  );
}
