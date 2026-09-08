import Navbar from "@/components/Navbar";
import Hero from "@/components/Hero";
import CategoryBento from "@/components/CategoryBento";
import NewArrivals from "@/components/NewArrivals";
import PromoBanner from "@/components/PromoBanner";
import Benefits from "@/components/Benefits";
import Footer from "@/components/Footer";

export default function HomePage() {
  return (
    <div className="min-h-screen flex flex-col bg-[#fafaf9]">
      <Navbar />
      <main className="flex-grow">
        <Hero />
        <CategoryBento />
        <NewArrivals />
        <PromoBanner />
        <Benefits />
      </main>
      <Footer />
    </div>
  );
}
