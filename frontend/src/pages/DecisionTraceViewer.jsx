import JsonPanel from "../components/JsonPanel.jsx";
import SnapshotDiff from "../components/SnapshotDiff.jsx";
import TraceTree from "../components/TraceTree.jsx";

export default function DecisionTraceViewer() {
  const last = window.__UCON_LAST_DECISION__ || null;

  return (
    <div className="space-y-6">
      <header>
        <p className="text-sm uppercase tracking-[0.32em] text-clay">explainability</p>
        <h2 className="font-display text-4xl">Decision Trace Viewer</h2>
        <p className="mt-2 text-ink/60">
          Use the Register / Drop Simulator first, then inspect phase, predicate, policy result and snapshots here.
        </p>
      </header>
      <TraceTree trace={last?.decisionTrace} />
      <SnapshotDiff response={last} />
      <JsonPanel title="Latest decision JSON" data={last || { message: "No global trace yet. The simulator page shows trace immediately after submit." }} />
    </div>
  );
}
