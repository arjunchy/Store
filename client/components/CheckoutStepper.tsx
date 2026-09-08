"use client";

import Link from "next/link";

export type CheckoutStep = 1 | 2 | 3 | 4;

const STEPS = [
  { n: 1, label: "Address", href: "/checkout/shipping" },
  { n: 2, label: "Delivery", href: "/checkout/delivery" },
  { n: 3, label: "Payment", href: "/checkout/payment" },
  { n: 4, label: "Review", href: "/checkout/review" },
] as const;


export function CheckoutStepper({
  currentStep,
  completed = false,
}: {
  currentStep: CheckoutStep;
  completed?: boolean;
}) {
  return (
    <nav aria-label="Checkout Progress" className="w-full">
      <ol className="flex items-start w-full">
        {STEPS.map((step, idx) => {
          const isCompleted = completed ? true : step.n < currentStep;
          const isCurrent = completed ? false : step.n === currentStep;
          const isUpcoming = completed ? false : step.n > currentStep;
          const isLast = idx === STEPS.length - 1;

          return (
            <li
              key={step.n}
              className={`flex items-center ${!isLast ? "flex-1" : "flex-shrink-0"}`}
              aria-current={isCurrent ? "step" : undefined}
            >
              <div className="flex flex-col items-center gap-1.5 relative flex-shrink-0">
                {isCompleted ? (
                  <Link
                    href={step.href}
                    aria-label={`Go to ${step.label} step`}
                    className="w-8 h-8 rounded-full flex items-center justify-center bg-[#b45309] text-white hover:bg-[#92400e] transition-colors shadow-sm"
                    title={`${step.label} — completed, click to edit`}
                  >
                    <span className="material-symbols-outlined text-[16px]">check</span>
                  </Link>
                ) : (
                  <div
                    className={`w-8 h-8 rounded-full flex items-center justify-center font-semibold text-[13px] transition-all ${
                      isCurrent
                        ? "bg-[#b45309] text-white ring-4 ring-primary-fixed shadow-[0_0_0_1px_rgba(53,37,205,0.15)]"
                        : "bg-[#fafaf9] text-[#57534e] border border-[#d6d3d1]"
                    }`}
                  >
                    {step.n}
                  </div>
                )}
                <span
                  className={`absolute top-10 text-[11px] font-medium whitespace-nowrap tracking-wide ${
                    isCurrent
                      ? "text-[#b45309] font-bold"
                      : isCompleted
                      ? "text-[#b45309]"
                      : "text-[#57534e]"
                  }`}
                >
                  {step.label}
                </span>
              </div>

              {!isLast && (
                <div
                  className={`flex-1 h-0.5 mx-2 md:mx-4 mb-6 transition-colors duration-300 ${
                    step.n < currentStep ? "bg-[#b45309]" : "bg-[#fafaf9]"
                  }`}
                  aria-hidden="true"
                />
              )}
            </li>
          );
        })}
      </ol>
    </nav>
  );
}


