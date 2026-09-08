const benefits = [
  {
    icon: "local_shipping",
    title: "Free Shipping",
    desc: "On all orders over Rs. 5,000.",
  },
  {
    icon: "security",
    title: "Secure Payment",
    desc: "100% secure checkout.",
  },
  {
    icon: "sync",
    title: "Easy Returns",
    desc: "30 days return policy.",
  },
];

export default function Benefits() {
  return (
    <section className="w-full max-w-[1440px] mx-auto px-margin-mobile md:px-margin-desktop py-8 md:py-lg mb-4 md:mb-xxl border-t border-[#d6d3d1]">
      <div className="grid grid-cols-1 md:grid-cols-3 gap-2 md:gap-lg text-center divide-y md:divide-y-0 md:divide-x divide-[#e7e5e4]">
        {benefits.map((b) => (
          <div key={b.title} className="flex flex-col items-center py-6 md:py-sm px-md">
            <div className="w-14 h-14 md:w-16 md:h-16 rounded-full bg-[#fef3c7] border border-[#b45309]/20 flex items-center justify-center text-[#b45309] mb-3">
              <span className="material-symbols-outlined text-[28px] md:text-[30px]">{b.icon}</span>
            </div>
            <h3 className="font-bold text-[13px] text-[#1c1917] mb-1 tracking-wide">
              {b.title}
            </h3>
            <p className="font-body-sm text-[12px] text-[#57534e]">{b.desc}</p>
          </div>
        ))}
      </div>
    </section>
  );
}
