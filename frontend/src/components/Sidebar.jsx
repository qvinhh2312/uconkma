import {
  Activity,
  BookOpen,
  FileCheck2,
  GitBranch,
  Home,
  LogIn,
  LogOut,
  Radar,
  ScrollText,
  Shield,
  User,
} from "lucide-react";
import { useEffect, useState } from "react";
import { NavLink } from "react-router-dom";
import { logout } from "../api/authApi.js";
import { clearSession, getSession } from "../auth/session.js";

const items = [
  { to: "/", label: "Dashboard", icon: Home },
  { to: "/login", label: "Login", icon: LogIn },
  { to: "/me", label: "My Student Portal", icon: User },
  { to: "/students", label: "Admin Students", icon: Shield },
  { to: "/policies", label: "Policy Explorer", icon: BookOpen },
  { to: "/simulate", label: "Register / Drop", icon: Activity },
  { to: "/trace", label: "Decision Trace", icon: GitBranch },
  { to: "/monitor", label: "Monitoring Demo", icon: Radar },
  { to: "/pap", label: "PAP Lifecycle", icon: ScrollText },
  { to: "/validation", label: "Validation Report", icon: FileCheck2 },
];

export default function Sidebar() {
  const [session, setSession] = useState(getSession());

  useEffect(() => {
    const sync = () => setSession(getSession());
    window.addEventListener("ucon-session-changed", sync);
    window.addEventListener("storage", sync);
    return () => {
      window.removeEventListener("ucon-session-changed", sync);
      window.removeEventListener("storage", sync);
    };
  }, []);

  async function handleLogout() {
    try {
      await logout();
    } catch {
      // Local logout is enough for this demo if backend token is already gone.
    }
    clearSession();
  }

  return (
    <aside className="sticky top-0 flex h-screen w-72 flex-col border-r border-ink/10 bg-ink px-5 py-6 text-paper">
      <div className="mb-8">
        <p className="text-xs uppercase tracking-[0.35em] text-paper/50">UCONKMA</p>
        <h1 className="mt-2 font-display text-3xl leading-tight">Policy Demo Console</h1>
      </div>
      <nav className="space-y-2">
        {items.map((item) => {
          const Icon = item.icon;
          return (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                [
                  "flex items-center gap-3 rounded-2xl px-4 py-3 text-sm transition",
                  isActive ? "bg-paper text-ink shadow-soft" : "text-paper/72 hover:bg-paper/10 hover:text-paper",
                ].join(" ")
              }
            >
              <Icon size={18} />
              {item.label}
            </NavLink>
          );
        })}
      </nav>
      <div className="mt-6 rounded-3xl border border-paper/15 bg-paper/10 p-4 text-sm text-paper/70">
        {session ? (
          <>
            <p className="font-semibold text-paper">{session.displayName}</p>
            <p>{session.role}{session.studentId ? ` / ${session.studentId}` : ""}</p>
            <button onClick={handleLogout} className="mt-3 flex items-center gap-2 rounded-xl bg-paper px-3 py-2 font-semibold text-ink">
              <LogOut size={16} /> Logout
            </button>
          </>
        ) : (
          <p>Login as `sv001` or `admin` to demo SQL-backed role views.</p>
        )}
      </div>
      <div className="mt-auto rounded-3xl border border-paper/15 bg-paper/10 p-4 text-sm text-paper/70">
        PRE → ONGOING → POST with Authorization, Obligation, Condition, mutable updates and trace.
      </div>
    </aside>
  );
}
