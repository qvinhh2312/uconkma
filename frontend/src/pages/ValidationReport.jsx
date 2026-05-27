import JsonPanel from "../components/JsonPanel.jsx";
import MetricCard from "../components/MetricCard.jsx";
import { validationSnapshot } from "../data/policyCatalog.js";

export default function ValidationReport() {
  return (
    <div className="space-y-6">
      <header>
        <p className="text-sm uppercase tracking-[0.32em] text-clay">verification evidence</p>
        <h2 className="font-display text-4xl">Validation Report</h2>
      </header>
      <section className="grid gap-4 md:grid-cols-4">
        <MetricCard label="DSL policies" value={validationSnapshot.dslPolicies} />
        <MetricCard label="XMI policies" value={validationSnapshot.xmiPolicies} />
        <MetricCard label="Engine tests" value={`${validationSnapshot.engineTests} pass`} tone="ink" />
        <MetricCard label="Line coverage" value={validationSnapshot.lineCoverage} tone="clay" />
      </section>
      <section className="rounded-3xl bg-paper p-6 shadow-soft">
        <h3 className="font-display text-2xl">Conformance checklist</h3>
        <div className="mt-4 grid gap-3 md:grid-cols-2">
          {[
            ["Missing DSL policies in XMI", validationSnapshot.missingDslPoliciesInXmi],
            ["Missing required attributes", validationSnapshot.missingRequiredAttributes],
            ["PolicySets", validationSnapshot.policySets],
            ["Branch coverage", validationSnapshot.branchCoverage],
          ].map(([label, value]) => (
            <div key={label} className="rounded-2xl border border-ink/10 bg-sand/60 p-4">
              <p className="text-sm text-ink/55">{label}</p>
              <p className="mt-1 font-display text-2xl">{value}</p>
            </div>
          ))}
        </div>
      </section>
      <JsonPanel title="Validation snapshot JSON" data={validationSnapshot} />
    </div>
  );
}
