import { notFound } from "next/navigation";
import ProductClient from "./ProductClient";
import { getProductById } from "@/lib/product";

export const dynamic = "force-dynamic";

export default async function ProductPage({ params }: { params: Promise<{ slug: string }> }) {
  const { slug } = await params;
  const product = await getProductById(slug);
  if (!product) notFound();
  return <ProductClient product={product} />;
}
