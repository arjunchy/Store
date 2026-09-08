import "../polyfill-localstorage.js";
import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";
import { Suspense } from "react";
import { CartProvider } from "@/context/CartContext";
import { AuthProvider } from "@/context/AuthContext";
import { WishlistProvider } from "@/context/WishlistContext";
import AuthGate from "@/components/AuthGate";

const inter = Inter({
  subsets: ["latin"],
  weight: ["400", "500", "600", "700", "800", "900"],
  display: "swap",
  variable: "--font-inter",
});

export const metadata: Metadata = {
  title: "ApexCommerce - Discover Products You'll Love",
  description:
    "Shop quality products at the best prices. Experience premium e-commerce tailored for you.",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" className={`light ${inter.variable}`}>
      <head>
        <link
          href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:wght,FILL@100..700,0..1&display=swap"
          rel="stylesheet"
        />
      </head>
      <body
        className={`${inter.className} bg-[#fafaf9] text-[#1c1917] min-h-screen flex flex-col antialiased`}
      >
        <AuthProvider>
          <CartProvider>
            <WishlistProvider>
              <Suspense
                fallback={
                  <div className="min-h-screen flex flex-col items-center justify-center bg-[#fafaf9] gap-4">
                    <div className="w-9 h-9 rounded-full border-[2.5px] border-[#d6d3d1] border-t-[#b45309] animate-spin" />
                    <p className="text-[11px] tracking-[0.14em] font-semibold text-[#a8a29e] uppercase">ApexCommerce</p>
                  </div>
                }
              >
                <AuthGate>{children}</AuthGate>
              </Suspense>
            </WishlistProvider>
          </CartProvider>
        </AuthProvider>
      </body>
    </html>
  );
}
