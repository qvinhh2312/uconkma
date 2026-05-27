export default function JsonPanel({ title = "JSON", data }) {
  return (
    <section className="rounded-3xl border border-ink/10 bg-ink p-4 text-paper shadow-soft">
      <div className="mb-3 flex items-center justify-between">
        <h3 className="font-display text-lg">{title}</h3>
        <span className="text-xs uppercase tracking-[0.25em] text-paper/45">raw</span>
      </div>
      <pre className="max-h-[32rem] overflow-auto rounded-2xl bg-black/25 p-4 text-xs leading-5 text-paper/85">
        {JSON.stringify(data ?? {}, null, 2)}
      </pre>
    </section>
  );
}
