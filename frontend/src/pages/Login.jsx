import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { login } from "../api/authApi.js";
import { normalizeApiError } from "../api/client.js";
import { setSession } from "../auth/session.js";

export default function Login() {
  const navigate = useNavigate();
  const [payload, setPayload] = useState({ username: "sv001", password: "student123" });
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);

  async function submit(event) {
    event.preventDefault();
    setLoading(true);
    try {
      const data = await login(payload);
      setSession(data);
      setResult(data);
      navigate(data.role === "ADMIN" ? "/students" : "/me");
    } catch (error) {
      setResult(normalizeApiError(error));
    } finally {
      setLoading(false);
    }
  }

  function useAccount(username, password) {
    setPayload({ username, password });
  }

  return (
    <div className="flex min-h-[calc(100vh-4rem)] items-center justify-center">
      <section className="w-full max-w-md">
        <form onSubmit={submit} className="rounded-[2rem] bg-paper p-8 shadow-soft">
          <p className="text-sm uppercase tracking-[0.32em] text-clay">UCONKMA</p>
          <h2 className="mt-3 font-display text-5xl text-ink">Login</h2>
          {result ? (
            <div className="mt-4 rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm font-semibold text-red-700">
              {result.message || result.errorCode || "Login failed."}
            </div>
          ) : null}
          <label className="mb-4 block text-sm text-ink/60">
            username
            <input
              className="mt-1 w-full rounded-xl border border-ink/10 px-3 py-2"
              value={payload.username}
              onChange={(event) => setPayload((current) => ({ ...current, username: event.target.value }))}
            />
          </label>
          <label className="mb-5 block text-sm text-ink/60">
            password
            <input
              type="password"
              className="mt-1 w-full rounded-xl border border-ink/10 px-3 py-2"
              value={payload.password}
              onChange={(event) => setPayload((current) => ({ ...current, password: event.target.value }))}
            />
          </label>
          <button disabled={loading} className="w-full rounded-2xl bg-clay px-4 py-3 font-semibold text-paper shadow-soft disabled:opacity-60">
            {loading ? "Logging in..." : "Login"}
          </button>
          <div className="mt-5 grid gap-2 text-sm">
            <button type="button" className="rounded-xl bg-sand px-3 py-2 text-left" onClick={() => useAccount("sv001", "student123")}>
              Student demo: sv001 / student123
            </button>
            <button type="button" className="rounded-xl bg-sand px-3 py-2 text-left" onClick={() => useAccount("admin", "admin123")}>
              Admin demo: admin / admin123
            </button>
          </div>
        </form>
      </section>
    </div>
  );
}
