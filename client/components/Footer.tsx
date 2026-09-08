import Link from "next/link";

const footerLinks = [
  { label: "About Us", href: "/catalog" },
  { label: "Terms of Service", href: "/catalog" },
  { label: "Privacy Policy", href: "/catalog" },
  { label: "Customer Service", href: "/catalog" },
  { label: "Contact", href: "/catalog" },
  { label: "Shipping Info", href: "/catalog" },
];

export default function Footer() {
  return (
    <footer className="bg-[#0c0a09] border-t border-[#b45309]/30 mt-auto">
      <div className="w-full px-margin-mobile md:px-margin-desktop py-10 md:py-xl max-w-[1440px] mx-auto flex flex-col md:flex-row justify-between gap-6 md:gap-8">
        <div className="shrink-0">
          <div className="font-bold text-[18px] text-[#b45309] mb-2 tracking-tight">
            ApexCommerce
          </div>
          <p className="font-body-sm text-[12px] text-[#a8a29e]">
            © 2024 ApexCommerce. All rights reserved.
          </p>
        </div>

        <nav className="flex flex-wrap gap-x-5 gap-y-3 items-center md:justify-end content-start">
          {footerLinks.map((link) => (
            <Link
              key={link.label}
              href={link.href}
              className="font-body-sm text-[12px] text-[#a8a29e] hover:text-[#b45309] underline underline-offset-2 decoration-[#44403c] hover:decoration-[#b45309] transition-colors"
            >
              {link.label}
            </Link>
          ))}
        </nav>
      </div>
    </footer>
  );
}
