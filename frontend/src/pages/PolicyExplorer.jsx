import { useMemo, useState } from "react";
import PolicyCard from "../components/PolicyCard.jsx";
import { policyCatalog } from "../data/policyCatalog.js";

const all = "ALL";

function unique(key) {
  return [all, ...Array.from(new Set(policyCatalog.map((policy) => policy[key]))).sort()];
}

export default function PolicyExplorer() {
  const [filters, setFilters] = useState({
    predicate: all,
    phase: all,
    action: all,
    variant: all,
    status: "ACTIVE",
    search: "",
  });

  const policies = useMemo(() => policyCatalog.filter((policy) => {
    return ["predicate", "phase", "action", "variant", "status"].every((key) => filters[key] === all || policy[key] === filters[key])
      && policy.policyId.toLowerCase().includes(filters.search.toLowerCase());
  }), [filters]);

  const update = (key, value) => setFilters((current) => ({ ...current, [key]: value }));

  return (
    <div className="space-y-6">
      <header>
        <p className="text-sm uppercase tracking-[0.32em] text-clay">Policy catalog</p>
        <h2 className="font-display text-4xl">Policy Explorer</h2>
      </header>
      <section className="grid gap-3 rounded-3xl bg-paper p-5 shadow-soft md:grid-cols-3 lg:grid-cols-6">
        {["predicate", "phase", "action", "variant", "status"].map((key) => (
          <label key={key} className="text-sm text-ink/60">
            {key}
            <select className="mt-1 w-full rounded-xl border border-ink/10 bg-white px-3 py-2" value={filters[key]} onChange={(event) => update(key, event.target.value)}>
              {unique(key).map((value) => <option key={value}>{value}</option>)}
            </select>
          </label>
        ))}
        <label className="text-sm text-ink/60">
          search
          <input className="mt-1 w-full rounded-xl border border-ink/10 bg-white px-3 py-2" value={filters.search} onChange={(event) => update("search", event.target.value)} placeholder="P20, PreA0..." />
        </label>
      </section>
      <div className="grid gap-4 xl:grid-cols-2">
        {policies.map((policy) => <PolicyCard key={policy.policyId} policy={policy} />)}
      </div>
    </div>
  );
}
