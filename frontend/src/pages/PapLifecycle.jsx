import { useEffect, useState } from "react";
import { getPapSummary, listPolicies, reloadPolicies, transitionPolicy } from "../api/papApi.js";
import { normalizeApiError } from "../api/client.js";
import JsonPanel from "../components/JsonPanel.jsx";

const transitions = ["DRAFT", "VALIDATED", "ACTIVE", "DEPRECATED", "ARCHIVED"];

export default function PapLifecycle() {
  const [policies, setPolicies] = useState([]);
  const [summary, setSummary] = useState(null);
  const [result, setResult] = useState(null);

  async function refresh() {
    try {
      const [policyData, summaryData] = await Promise.all([listPolicies(), getPapSummary()]);
      setPolicies(policyData);
      setSummary(summaryData);
    } catch (error) {
      setResult(normalizeApiError(error));
    }
  }

  useEffect(() => {
    refresh();
  }, []);

  async function transition(policyId, targetStatus) {
    try {
      setResult(await transitionPolicy(policyId, targetStatus));
      await refresh();
    } catch (error) {
      setResult(normalizeApiError(error));
    }
  }

  async function reload() {
    try {
      setResult(await reloadPolicies());
      await refresh();
    } catch (error) {
      setResult(normalizeApiError(error));
    }
  }

  return (
    <div className="space-y-6">
      <header className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <p className="text-sm uppercase tracking-[0.32em] text-clay">policy administration point</p>
          <h2 className="font-display text-4xl">PAP Lifecycle</h2>
        </div>
        <button onClick={reload} className="rounded-2xl bg-ink px-4 py-3 font-semibold text-paper">Reload policy model</button>
      </header>
      <JsonPanel title="Runtime active summary" data={summary || { message: "Loading PAP summary..." }} />
      <section className="overflow-hidden rounded-3xl bg-paper shadow-soft">
        <table className="w-full text-left text-sm">
          <thead className="bg-ink text-paper">
            <tr>
              <th className="px-4 py-3">Policy</th>
              <th className="px-4 py-3">Status</th>
              <th className="px-4 py-3">Variant</th>
              <th className="px-4 py-3">Version</th>
              <th className="px-4 py-3">Transition</th>
            </tr>
          </thead>
          <tbody>
            {policies.map((policy) => (
              <tr key={policy.policyId} className="border-t border-ink/10">
                <td className="px-4 py-3 font-semibold">{policy.policyId}</td>
                <td className="px-4 py-3">{policy.status}</td>
                <td className="px-4 py-3">{policy.uconVariant}</td>
                <td className="px-4 py-3">{policy.version}</td>
                <td className="px-4 py-3">
                  <select className="rounded-xl border border-ink/10 px-3 py-2" defaultValue="" onChange={(event) => event.target.value && transition(policy.policyId, event.target.value)}>
                    <option value="">Choose...</option>
                    {transitions.map((item) => <option key={item}>{item}</option>)}
                  </select>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
      <JsonPanel title="PAP action response" data={result || { message: "No PAP action yet." }} />
    </div>
  );
}
