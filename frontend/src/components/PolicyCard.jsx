export default function PolicyCard({ policy }) {
  return (
    <article className="rounded-3xl border border-ink/10 bg-paper p-5 shadow-soft">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <h3 className="font-display text-xl text-ink">{policy.policyId}</h3>
        <span className="rounded-full bg-moss/10 px-3 py-1 text-xs font-semibold text-moss">{policy.variant}</span>
      </div>
      <div className="mt-4 grid grid-cols-2 gap-2 text-sm text-ink/70">
        <span>Predicate: <b>{policy.predicate}</b></span>
        <span>Phase: <b>{policy.phase}</b></span>
        <span>Update: <b>{policy.updateTiming}</b></span>
        <span>Action: <b>{policy.action}</b></span>
        <span>Effect: <b>{policy.effect}</b></span>
        <span>Status: <b>{policy.status}</b></span>
      </div>
      <p className="mt-4 text-sm text-ink/55">{policy.source} · v{policy.version}</p>
    </article>
  );
}
