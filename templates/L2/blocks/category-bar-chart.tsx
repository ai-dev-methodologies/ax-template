/*
---
template_id: L2/blocks/category-bar-chart
layer: L2
provenance_class: internal_design
evidence:
  - source_type: internal
    rationale: "Codified from the community 21st.dev component ahmedmayara/category-bar-chart: cn inlined (clsx+tailwind-merge) and the shadcn Card/Select/Separator/Tooltip primitives MAPPED TO L1 — shipped self-contained as minimal module-scope Card/Separator + a native <select> + title-attribute tooltips, so the block imports no @/ alias (ax block self-containment). A fork may swap the inlined primitives for the full templates/L1/components/{card,select,separator,tooltip}.tsx. Passes all 7 ax/* own-block rules. Governed by practices-react/rules/ux-block-uses-design-tokens-and-a11y.md."
dependencies: [card, select, separator, tooltip]
imports_from: [L1]
imports_forbidden: [L4, app/, lib/]
---
*/
/**
 * @ax-codified-from 21st.dev/ahmedmayara/category-bar-chart
 * @ax-layer L2/blocks/chart
 */
import React from "react";
import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";
import { addDays, addMonths, format, startOfToday } from "date-fns";

// cn inlined (ax blocks avoid the lib alias); behavior-identical to the shadcn util
function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

// Minimal L1-equivalent primitives, inlined at module scope so the block is self-contained
// (maps to templates/L1/components/{card,separator}.tsx; a fork may swap in the full L1 versions).
function Card({ className, ...props }: React.ComponentProps<"div">) {
  return <div className={cn("rounded-xl border bg-card text-card-foreground", className)} {...props} />;
}
function CardHeader({ className, ...props }: React.ComponentProps<"div">) {
  return <div className={cn("flex flex-col gap-1.5", className)} {...props} />;
}
function CardTitle({ className, ...props }: React.ComponentProps<"div">) {
  return <div className={cn("font-semibold leading-none", className)} {...props} />;
}
function CardContent({ className, ...props }: React.ComponentProps<"div">) {
  return <div className={className} {...props} />;
}
function Separator({ className }: { className?: string }) {
  return <div role="separator" className={cn("bg-border h-px w-full shrink-0", className)} />;
}

type DateRange = {
  from: Date;
  to: Date;
};

const weeklyData = [
  { width: 29.41, color: "bg-blue-500", label: "Online" },
  { width: 19.61, color: "bg-cyan-500", label: "Retail" },
  { width: 16.34, color: "bg-indigo-500", label: "Wholesale" },
  { width: 13.07, color: "bg-orange-500", label: "Affiliate" },
  { width: 9.8, color: "bg-amber-500", label: "Direct" },
  { width: 6.53, color: "bg-emerald-500", label: "Partners" },
  { width: 5.23, color: "bg-emerald-300", label: "Other" },
];

const monthlyData = [
  { width: 34.5, color: "bg-blue-500", label: "Online" },
  { width: 22.1, color: "bg-cyan-500", label: "Retail" },
  { width: 15.4, color: "bg-indigo-500", label: "Wholesale" },
  { width: 10.8, color: "bg-orange-500", label: "Affiliate" },
  { width: 9.2, color: "bg-amber-500", label: "Direct" },
  { width: 4.0, color: "bg-emerald-500", label: "Partners" },
  { width: 4.0, color: "bg-emerald-300", label: "Other" },
];

export const Component = ({
  className,
  ...props
}: React.ComponentProps<"div">) => {
  const [selectedPeriod, setSelectedPeriod] = React.useState<string>("weekly");

  const today = startOfToday();
  const weeklyRange: DateRange = {
    from: today,
    to: addDays(today, 7),
  };
  const monthlyRange: DateRange = {
    from: today,
    to: addMonths(today, 1),
  };

  const period = selectedPeriod === "weekly" ? weeklyRange : monthlyRange;
  const salesData = selectedPeriod === "weekly" ? weeklyData : monthlyData;
  const totalSales = selectedPeriod === "weekly" ? 246 : 1024;

  return (
    <Card
      className={cn(
        "flex h-full w-full flex-col gap-0 p-6 shadow-none",
        className,
      )}
      {...props}
    >
      <CardHeader className="flex flex-row items-center justify-between p-0">
        <div className="flex flex-row items-center gap-1">
          <CardTitle className="text-base font-medium text-muted-foreground">
            Sales Channels
          </CardTitle>
          <span
            className="inline-flex"
            title="Compare weekly performance across your sales channels. Understand trends, evaluate growth rates, and identify key contributors to your overall sales."
          >
            <svg
              width={20}
              height={20}
              viewBox="0 0 20 20"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
              className="size-5 text-muted-foreground/50"
              aria-hidden="true"
            >
              <path
                fillRule="evenodd"
                clipRule="evenodd"
                d="M10 16.25a6.25 6.25 0 100-12.5 6.25 6.25 0 000 12.5zm1.116-3.041l.1-.408a1.709 1.709 0 01-.25.083 1.176 1.176 0 01-.308.048c-.193 0-.329-.032-.407-.095-.079-.064-.118-.184-.118-.359a3.514 3.514 0 01.118-.672l.373-1.318c.037-.121.062-.255.075-.4a3.73 3.73 0 00.02-.304.866.866 0 00-.292-.678c-.195-.174-.473-.26-.833-.26-.2 0-.412.035-.636.106-.224.07-.459.156-.704.256l-.1.409c.073-.028.16-.057.262-.087.101-.03.2-.045.297-.045.198 0 .331.034.4.1.07.066.105.185.105.354 0 .093-.01.197-.034.31a6.216 6.216 0 01-.084.36l-.374 1.325c-.033.14-.058.264-.073.374-.015.11-.022.22-.022.325 0 .272.1.496.301.673.201.177.483.265.846.265.236 0 .443-.03.621-.092s.417-.152.717-.27zM11.05 7.85a.772.772 0 00.26-.587.78.78 0 00-.26-.59.885.885 0 00-.628-.244.893.893 0 00-.63.244.778.778 0 00-.264.59c0 .23.088.426.263.587a.897.897 0 00.63.243.888.888 0 00.629-.243z"
                fill="currentColor"
              />
            </svg>
          </span>
        </div>
        <select
          value={selectedPeriod}
          onChange={(e) => setSelectedPeriod(e.target.value)}
          aria-label="Select period"
          className="h-8 w-full gap-2 rounded-md border bg-transparent px-2 text-sm md:w-auto"
        >
          <option value="weekly">Weekly</option>
          <option value="monthly">Monthly</option>
        </select>
      </CardHeader>

      <CardContent className="flex flex-col gap-4 p-0">
        <div className="flex items-center gap-3">
          <span className="text-3xl font-medium leading-none tracking-tight tabular-nums">
            {totalSales}
          </span>
          {selectedPeriod === "weekly" ? (
            <p className="text-sm text-green-500 dark:text-green-600">
              +2.1%{" "}
              <span className="text-muted-foreground">from last week</span>
            </p>
          ) : (
            <p className="text-sm text-green-500 dark:text-green-600">
              +5.4%{" "}
              <span className="text-muted-foreground">from last month</span>
            </p>
          )}
        </div>

        <div className="flex flex-col gap-2">
          <div className="flex items-baseline justify-between">
            <p className="text-sm font-normal text-muted-foreground">
              {period?.from ? format(period.from, "MMM dd, yyyy") : null}
            </p>
            <p className="text-sm font-normal text-muted-foreground">
              {period?.to ? format(period.to, "MMM dd, yyyy") : null}
            </p>
          </div>

          <div className="flex gap-1">
            {salesData.map((item, index) => (
              <div
                key={index}
                title={`${item.label} - ${item.width}%`}
                className="h-[42px] rounded-sm transition-all"
                style={{ width: `${item.width}%` }}
              >
                <div className={cn("h-full rounded-sm", item.color)} />
              </div>
            ))}
          </div>
        </div>

        <Separator />

        <p className="text-xs text-muted-foreground">
          This chart shows the distribution of your total sales across different
          channels. Use this breakdown to understand where most of your revenue
          is coming from, which channels are underperforming, and where to focus
          your next campaign.
        </p>
      </CardContent>
    </Card>
  );
};
