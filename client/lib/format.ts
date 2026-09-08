export function formatNPR(n: number): string {
  if (typeof n !== "number" || isNaN(n)) return "Rs. 0.00";
  try {
    return new Intl.NumberFormat("en-NP", {
      style: "currency",
      currency: "NPR",
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(n);
  } catch {
    return `Rs. ${n.toFixed(2)}`;
  }
}

export const formatUSD = formatNPR;
export const formatPrice = formatNPR;
