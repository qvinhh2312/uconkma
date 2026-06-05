import { Navigate, Route, Routes } from "react-router-dom";
import Layout from "./components/Layout.jsx";
import Dashboard from "./pages/Dashboard.jsx";
import DecisionTraceViewer from "./pages/DecisionTraceViewer.jsx";
import Login from "./pages/Login.jsx";
import MonitoringDemo from "./pages/MonitoringDemo.jsx";
import PapLifecycle from "./pages/PapLifecycle.jsx";
import PolicyExplorer from "./pages/PolicyExplorer.jsx";
import RegisterDropSimulator from "./pages/RegisterDropSimulator.jsx";
import StudentPortal from "./pages/StudentPortal.jsx";
import AdminStudents from "./pages/AdminStudents.jsx";
import ValidationReport from "./pages/ValidationReport.jsx";
import { getSession, isAdmin, isStudent } from "./auth/session.js";
import { useEffect, useState } from "react";

function RequireLogin({ children }) {
  const session = useSessionState();
  return session ? children : <Navigate to="/login" replace />;
}

function RequireAdmin({ children }) {
  const session = useSessionState();
  if (!session) return <Navigate to="/login" replace />;
  return isAdmin(session) ? children : <Navigate to="/dashboard" replace />;
}

function RequireStudent({ children }) {
  const session = useSessionState();
  if (!session) return <Navigate to="/login" replace />;
  return isStudent(session) ? children : <Navigate to="/dashboard" replace />;
}

function useSessionState() {
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
  return session;
}

export default function App() {
  return (
    <Layout>
      <Routes>
        <Route path="/" element={<Navigate to="/login" replace />} />
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/login" element={<Login />} />
        <Route path="/me" element={<RequireStudent><StudentPortal /></RequireStudent>} />
        <Route path="/students" element={<RequireAdmin><AdminStudents /></RequireAdmin>} />
        <Route path="/policies" element={<PolicyExplorer />} />
        <Route path="/simulate" element={<RegisterDropSimulator />} />
        <Route path="/trace" element={<DecisionTraceViewer />} />
        <Route path="/monitor" element={<MonitoringDemo />} />
        <Route path="/pap" element={<RequireAdmin><PapLifecycle /></RequireAdmin>} />
        <Route path="/validation" element={<ValidationReport />} />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    </Layout>
  );
}
