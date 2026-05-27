export default function DecisionBadge({ decision }) {
  const normalized = String(decision || "UNKNOWN").toUpperCase();
  const positive = ["ALLOW", "PERMIT", "COMMITTED", "SUCCESS"].includes(normalized);
  const revoked = normalized.includes("REVOKED");
  const classes = positive
    ? "border-emerald-700/20 bg-emerald-100 text-emerald-800"
    : revoked
      ? "border-orange-700/20 bg-orange-100 text-orange-800"
      : "border-red-700/20 bg-red-100 text-red-800";

  return <span className={`inline-flex rounded-full border px-3 py-1 text-sm font-semibold ${classes}`}>{normalized}</span>;
}
