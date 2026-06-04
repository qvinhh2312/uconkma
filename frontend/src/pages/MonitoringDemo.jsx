import { useState } from "react";
import { addStudentHold, changeClassStatus, recheckActiveSessions, setMaintenance } from "../api/monitorApi.js";
import { normalizeApiError } from "../api/client.js";
import MetricCard from "../components/MetricCard.jsx";

export default function MonitoringDemo() {
  const [classId, setClassId] = useState("CS102_01");
  const [status, setStatus] = useState("LOCKED");
  const [studentId, setStudentId] = useState("SV001");
  const [holdCode, setHoldCode] = useState("ACADEMIC_HOLD");
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);

  async function run(action) {
    setLoading(true);
    try {
      setResult(await action());
    } catch (error) {
      setResult(normalizeApiError(error));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="space-y-6">
      <header>
        <p className="text-sm uppercase tracking-[0.32em] text-clay">continuity</p>
        <h2 className="font-display text-4xl">Monitoring / Revoke Demo</h2>
      </header>

      {result ? (
        <section className="grid gap-4 md:grid-cols-3">
          <MetricCard label="checkedSessions" value={result.checkedSessions ?? "N/A"} />
          <MetricCard label="revokedSessions" value={result.revokedSessions ?? "N/A"} tone="clay" />
          <MetricCard label="action" value={result.action || result.errorCode || "N/A"} tone="ink" />
        </section>
      ) : null}
      {result?.message ? (
        <p className="rounded-2xl bg-paper px-4 py-3 text-sm font-semibold text-ink/70 shadow-soft">
          {result.message}
        </p>
      ) : null}

      <section className="grid gap-5 xl:grid-cols-4">
        <div className="rounded-3xl bg-paper p-5 shadow-soft">
          <h3 className="font-display text-2xl">Maintenance</h3>
          <div className="mt-5 grid gap-3">
            <button disabled={loading} onClick={() => run(() => setMaintenance(true))} className="rounded-2xl bg-clay px-4 py-3 font-semibold text-paper">
              Maintenance ON
            </button>
            <button disabled={loading} onClick={() => run(() => setMaintenance(false))} className="rounded-2xl bg-moss px-4 py-3 font-semibold text-paper">
              Maintenance OFF
            </button>
          </div>
        </div>

        <div className="rounded-3xl bg-paper p-5 shadow-soft">
          <h3 className="font-display text-2xl">Class Status</h3>
          <label className="mt-4 block text-sm text-ink/60">
            classId
            <input className="mt-1 w-full rounded-xl border border-ink/10 px-3 py-2" value={classId} onChange={(event) => setClassId(event.target.value)} />
          </label>
          <label className="mt-3 block text-sm text-ink/60">
            status
            <select className="mt-1 w-full rounded-xl border border-ink/10 px-3 py-2" value={status} onChange={(event) => setStatus(event.target.value)}>
              {["OPEN", "LOCKED", "CLOSED", "CANCELLED"].map((item) => <option key={item}>{item}</option>)}
            </select>
          </label>
          <button disabled={loading} onClick={() => run(() => changeClassStatus(classId, status))} className="mt-4 w-full rounded-2xl bg-ink px-4 py-3 font-semibold text-paper">
            Change status
          </button>
        </div>

        <div className="rounded-3xl bg-paper p-5 shadow-soft">
          <h3 className="font-display text-2xl">Student Hold</h3>
          <label className="mt-4 block text-sm text-ink/60">
            studentId
            <input className="mt-1 w-full rounded-xl border border-ink/10 px-3 py-2" value={studentId} onChange={(event) => setStudentId(event.target.value)} />
          </label>
          <label className="mt-3 block text-sm text-ink/60">
            holdCode
            <input className="mt-1 w-full rounded-xl border border-ink/10 px-3 py-2" value={holdCode} onChange={(event) => setHoldCode(event.target.value)} />
          </label>
          <button disabled={loading} onClick={() => run(() => addStudentHold(studentId, holdCode))} className="mt-4 w-full rounded-2xl bg-ink px-4 py-3 font-semibold text-paper">
            Add hold
          </button>
        </div>

        <div className="rounded-3xl bg-paper p-5 shadow-soft">
          <h3 className="font-display text-2xl">Manual Recheck</h3>
          <p className="mt-3 text-sm text-ink/60">Re-evaluate all ACTIVE sessions against ONGOING policies.</p>
          <button disabled={loading} onClick={() => run(recheckActiveSessions)} className="mt-5 w-full rounded-2xl bg-moss px-4 py-3 font-semibold text-paper">
            Recheck all active
          </button>
        </div>
      </section>
    </div>
  );
}
