export default function MetricCard({ label, value, detail, tone = "moss" }) {
  const tones = {
    moss: "border-moss/20 bg-moss/10 text-moss",
    clay: "border-clay/20 bg-clay/10 text-clay",
    ink: "border-ink/15 bg-ink/5 text-ink",
  };

  return (
    <div className="rounded-3xl border border-ink/10 bg-paper p-5 shadow-soft">
      <p className="text-sm text-ink/55">{label}</p>
      <div className={`mt-3 inline-flex rounded-2xl border px-4 py-2 font-display text-3xl ${tones[tone]}`}>
        {value}
      </div>
      {detail ? <p className="mt-3 text-sm text-ink/60">{detail}</p> : null}
    </div>
  );
}
