import { useState } from "react";
import { dropCourse, registerCourse } from "../api/registrationApi.js";
import { normalizeApiError } from "../api/client.js";
import DecisionBadge from "../components/DecisionBadge.jsx";
import JsonPanel from "../components/JsonPanel.jsx";
import SnapshotDiff from "../components/SnapshotDiff.jsx";
import TraceTree from "../components/TraceTree.jsx";

const defaultRegister = {
  requestId: "REQ-001",
  studentId: "SV001",
  classId: "CS102_01",
  confirmedRegistrationRule: false,
  adminOverride: false,
  overrideReason: "",
  sessionLeaseValid: true,
};

const defaultDrop = {
  requestId: "REQ-DROP-001",
  studentId: "SV001",
  classId: "CS102_01",
  sessionLeaseValid: true,
};

export default function RegisterDropSimulator() {
  const [mode, setMode] = useState("REGISTER");
  const [registerPayload, setRegisterPayload] = useState(defaultRegister);
  const [dropPayload, setDropPayload] = useState(defaultDrop);
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);

  const payload = mode === "REGISTER" ? registerPayload : dropPayload;
  const setPayload = mode === "REGISTER" ? setRegisterPayload : setDropPayload;

  async function submit(event) {
    event.preventDefault();
    setLoading(true);
    try {
      const data = mode === "REGISTER" ? await registerCourse(payload) : await dropCourse(payload);
      window.__UCON_LAST_DECISION__ = data;
      setResult(data);
    } catch (error) {
      const normalized = normalizeApiError(error);
      window.__UCON_LAST_DECISION__ = normalized;
      setResult(normalized);
    } finally {
      setLoading(false);
    }
  }

  function update(key, value) {
    setPayload((current) => ({ ...current, [key]: value }));
  }

  return (
    <div className="space-y-6">
      <header>
        <p className="text-sm uppercase tracking-[0.32em] text-clay">PEP request simulator</p>
        <h2 className="font-display text-4xl">Register / Drop Simulator</h2>
      </header>
      <section className="grid gap-6 xl:grid-cols-[26rem,1fr]">
        <form onSubmit={submit} className="rounded-3xl bg-paper p-6 shadow-soft">
          <div className="mb-5 grid grid-cols-2 gap-2 rounded-2xl bg-sand p-2">
            {["REGISTER", "DROP"].map((item) => (
              <button
                key={item}
                type="button"
                onClick={() => setMode(item)}
                className={`rounded-xl px-4 py-2 font-semibold ${mode === item ? "bg-ink text-paper" : "text-ink/65"}`}
              >
                {item}
              </button>
            ))}
          </div>
          {["requestId", "studentId", "classId"].map((key) => (
            <label key={key} className="mb-4 block text-sm text-ink/60">
              {key}
              <input className="mt-1 w-full rounded-xl border border-ink/10 px-3 py-2" value={payload[key]} onChange={(event) => update(key, event.target.value)} />
            </label>
          ))}
          {mode === "REGISTER" ? (
            <>
              <label className="mb-3 flex items-center gap-3 text-sm">
                <input type="checkbox" checked={payload.confirmedRegistrationRule} onChange={(event) => update("confirmedRegistrationRule", event.target.checked)} />
                confirmedRegistrationRule
              </label>
              <label className="mb-3 flex items-center gap-3 text-sm">
                <input type="checkbox" checked={payload.adminOverride} onChange={(event) => update("adminOverride", event.target.checked)} />
                adminOverride
              </label>
              <label className="mb-4 block text-sm text-ink/60">
                overrideReason
                <input className="mt-1 w-full rounded-xl border border-ink/10 px-3 py-2" value={payload.overrideReason} onChange={(event) => update("overrideReason", event.target.value)} />
              </label>
            </>
          ) : null}
          <label className="mb-5 flex items-center gap-3 text-sm">
            <input type="checkbox" checked={payload.sessionLeaseValid} onChange={(event) => update("sessionLeaseValid", event.target.checked)} />
            sessionLeaseValid
          </label>
          <button disabled={loading} className="w-full rounded-2xl bg-clay px-4 py-3 font-semibold text-paper shadow-soft disabled:opacity-60">
            {loading ? "Submitting..." : `Submit ${mode}`}
          </button>
          <p className="mt-4 text-sm text-ink/55">
            Tip: leave confirmedRegistrationRule unchecked to demonstrate P17 preB0 obligation deny.
          </p>
        </form>

        <div className="space-y-5">
          {result ? (
            <section className="rounded-3xl bg-paper p-6 shadow-soft">
              <div className="flex flex-wrap items-center gap-3">
                <DecisionBadge decision={result.decision || result.errorCode || "UNKNOWN"} />
                <span className="text-sm text-ink/55">phase: <b>{result.phase || "N/A"}</b></span>
                <span className="text-sm text-ink/55">predicate: <b>{result.predicate || "N/A"}</b></span>
                <span className="text-sm text-ink/55">session: <b>{result.sessionStatus || "N/A"}</b></span>
              </div>
              <div className="mt-4 grid gap-3 md:grid-cols-2">
                <p className="text-sm text-ink/60">failedPolicy: <b>{result.failedPolicy || "N/A"}</b></p>
                <p className="text-sm text-ink/60">denyReason: <b>{result.denyReason || result.errorCode || "N/A"}</b></p>
              </div>
            </section>
          ) : null}
          <TraceTree trace={result?.decisionTrace} />
          <SnapshotDiff response={result} />
          <JsonPanel title="API response" data={result || { message: "Submit a request to see the UCON decision." }} />
        </div>
      </section>
    </div>
  );
}
