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
      {last ? (
        <>
          <TraceTree trace={last.decisionTrace} />
          <SnapshotDiff response={last} />
        </>
      ) : (
        <section className="rounded-3xl bg-paper p-6 shadow-soft">
          <h3 className="font-display text-2xl">No decision selected</h3>
          <p className="mt-2 text-ink/60">
            Submit a REGISTER or DROP request in the simulator to inspect the latest UCON decision trace here.
          </p>
        </section>
      )}
    </div>
  );
}
