import { useEffect, useMemo, useState } from "react";
import { getDemoState } from "../api/demoApi.js";
import JsonPanel from "../components/JsonPanel.jsx";
import MetricCard from "../components/MetricCard.jsx";
import { policyCatalog, uconVariants, validationSnapshot } from "../data/policyCatalog.js";

export default function Dashboard() {
  const [state, setState] = useState(null);
  const [error, setError] = useState(null);

  const counts = useMemo(() => ({
    authorization: policyCatalog.filter((p) => p.predicate === "AUTHORIZATION").length,
    obligation: policyCatalog.filter((p) => p.predicate === "OBLIGATION").length,
    condition: policyCatalog.filter((p) => p.predicate === "CONDITION").length,
  }), []);

  useEffect(() => {
    getDemoState()
      .then(setState)
      .catch((err) => setError(err.response?.data || { message: err.message }));
  }, []);

  return (
    <div className="space-y-7">
      <section className="rounded-[2rem] bg-paper p-8 shadow-soft">
        <p className="text-sm uppercase tracking-[0.32em] text-clay">UCON demo app</p>
        <h2 className="mt-3 font-display text-5xl text-ink">Course Registration Policy Enforcement</h2>
        <p className="mt-4 max-w-3xl text-ink/65">
          This dashboard demonstrates PEP → PDP evaluation, PRE / ONGOING / POST phases,
          A/B/C predicates, mutable updates, rollback hooks, session status and explainable traces.
        </p>
      </section>

      <section className="grid gap-4 md:grid-cols-4">
        <MetricCard label="Total policies" value={policyCatalog.length} detail="All ACTIVE in runtime PDP" />
        <MetricCard label="Authorization" value={counts.authorization} detail="A policies" tone="ink" />
        <MetricCard label="Obligation" value={counts.obligation} detail="B policies" tone="clay" />
        <MetricCard label="Condition" value={counts.condition} detail="C policies" />
      </section>

      <section className="rounded-3xl border border-ink/10 bg-paper p-6 shadow-soft">
        <h3 className="font-display text-2xl">Covered UCON Variants</h3>
        <div className="mt-4 flex flex-wrap gap-2">
          {uconVariants.map((variant) => (
            <span key={variant} className="rounded-full border border-moss/20 bg-moss/10 px-3 py-1 text-sm font-semibold text-moss">
              {variant}
            </span>
          ))}
        </div>
      </section>

      <section className="grid gap-4 lg:grid-cols-2">
        <div className="rounded-3xl border border-ink/10 bg-paper p-6 shadow-soft">
          <h3 className="font-display text-2xl">Verification Snapshot</h3>
          <dl className="mt-4 grid grid-cols-2 gap-3 text-sm">
            <dt className="text-ink/55">Engine tests</dt>
            <dd className="font-semibold">{validationSnapshot.engineTests} pass</dd>
            <dt className="text-ink/55">DSL tests</dt>
            <dd className="font-semibold">{validationSnapshot.dslTests} pass</dd>
            <dt className="text-ink/55">Line coverage</dt>
            <dd className="font-semibold">{validationSnapshot.lineCoverage}</dd>
            <dt className="text-ink/55">Branch coverage</dt>
            <dd className="font-semibold">{validationSnapshot.branchCoverage}</dd>
          </dl>
        </div>
        <JsonPanel title={error ? "Backend state error" : "Backend demo state"} data={error || state || { message: "Loading backend state..." }} />
      </section>
    </div>
  );
}
