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
import { getSession } from "./auth/session.js";

function RequireLogin({ children }) {
  return getSession() ? children : <Navigate to="/login" replace />;
}

export default function App() {
  return (
    <Layout>
      <Routes>
        <Route path="/" element={<Navigate to="/login" replace />} />
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/login" element={<Login />} />
        <Route path="/me" element={<RequireLogin><StudentPortal /></RequireLogin>} />
        <Route path="/students" element={<RequireLogin><AdminStudents /></RequireLogin>} />
        <Route path="/policies" element={<PolicyExplorer />} />
        <Route path="/simulate" element={<RegisterDropSimulator />} />
        <Route path="/trace" element={<DecisionTraceViewer />} />
        <Route path="/monitor" element={<MonitoringDemo />} />
        <Route path="/pap" element={<PapLifecycle />} />
        <Route path="/validation" element={<ValidationReport />} />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    </Layout>
  );
}
