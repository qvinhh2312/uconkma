import DecisionBadge from "./DecisionBadge.jsx";

function phaseEntries(trace) {
  if (!trace) return [];
  if (Array.isArray(trace.phases)) return trace.phases;
  if (Array.isArray(trace.phaseTraces)) return trace.phaseTraces;
  if (Array.isArray(trace.traces)) return trace.traces;
  return [];
}

function policiesOf(phase) {
  return phase.policies || phase.policyResults || phase.entries || [];
}

function policyStatus(policy) {
  return policy.result || policy.decision || policy.status || (policy.conditionResult === false ? "FAIL" : "PASS");
}

export default function TraceTree({ trace }) {
  const phases = phaseEntries(trace);
  if (!phases.length) {
    return (
      <div className="rounded-3xl border border-dashed border-ink/20 bg-paper p-5 text-sm text-ink/55">
        No structured trace found. Use the raw JSON panel for this response.
      </div>
    );
  }

  return (
    <section className="rounded-3xl border border-ink/10 bg-paper p-5 shadow-soft">
      <h3 className="font-display text-2xl">Decision Trace</h3>
      <div className="mt-5 space-y-4">
        {phases.map((phase, index) => (
          <div key={`${phase.phase}-${phase.predicate}-${index}`} className="rounded-2xl border border-ink/10 bg-sand/60 p-4">
            <div className="flex flex-wrap items-center gap-3">
              <span className="font-display text-xl">{phase.phase || "UNKNOWN"}</span>
              <span className="rounded-full bg-ink/5 px-3 py-1 text-xs font-semibold text-ink/65">
                {phase.predicate || "PREDICATE"}
              </span>
              <DecisionBadge decision={phase.decision || phase.result || "PASS"} />
            </div>
            <div className="mt-3 space-y-2">
              {policiesOf(phase).map((policy, policyIndex) => (
                <div
                  key={`${policy.policyId || policy.id}-${policyIndex}`}
                  className="flex flex-wrap items-center justify-between gap-2 rounded-xl bg-paper px-3 py-2 text-sm"
                >
                  <div>
                    <b>{policy.policyId || policy.id || "policy"}</b>
                    {policy.uconVariant ? <span className="ml-2 text-ink/45">({policy.uconVariant})</span> : null}
                  </div>
                  <div className="flex items-center gap-2">
                    {policy.denyReason ? <span className="text-xs text-ink/45">{policy.denyReason}</span> : null}
                    <DecisionBadge decision={policyStatus(policy)} />
                  </div>
                </div>
              ))}
              {!policiesOf(phase).length ? <p className="text-sm text-ink/50">No policy rows for this phase.</p> : null}
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}
