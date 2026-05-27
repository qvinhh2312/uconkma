function findSnapshot(response, key) {
  return response?.decisionTrace?.[key] || response?.[key] || response?.decisionTrace?.snapshot?.[key] || null;
}

export default function SnapshotDiff({ response }) {
  const before = findSnapshot(response, "snapshotBefore");
  const after = findSnapshot(response, "snapshotAfter");
  const keys = Array.from(new Set([...Object.keys(before || {}), ...Object.keys(after || {})]));
  const changed = keys.filter((key) => JSON.stringify(before?.[key]) !== JSON.stringify(after?.[key]));

  if (!before && !after) {
    return (
      <section className="rounded-3xl border border-dashed border-ink/20 bg-paper p-5 text-sm text-ink/55">
        Snapshot before/after not present in this response.
      </section>
    );
  }

  return (
    <section className="rounded-3xl border border-ink/10 bg-paper p-5 shadow-soft">
      <h3 className="font-display text-2xl">Mutable Attribute Diff</h3>
      <div className="mt-4 overflow-hidden rounded-2xl border border-ink/10">
        <table className="w-full text-left text-sm">
          <thead className="bg-ink text-paper">
            <tr>
              <th className="px-4 py-3">Attribute</th>
              <th className="px-4 py-3">Before</th>
              <th className="px-4 py-3">After</th>
            </tr>
          </thead>
          <tbody>
            {(changed.length ? changed : keys).map((key) => (
              <tr key={key} className="border-t border-ink/10">
                <td className="px-4 py-3 font-semibold">{key}</td>
                <td className="px-4 py-3 text-ink/65">{String(before?.[key] ?? "N/A")}</td>
                <td className="px-4 py-3 text-ink/65">{String(after?.[key] ?? "N/A")}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}
