import { api } from "./client.js";

export async function setMaintenance(active) {
  const response = await api.post("/demo/monitor/maintenance", null, {
    params: { active },
  });
  return response.data;
}

export async function changeClassStatus(classId, status) {
  const response = await api.post("/demo/monitor/class-status", null, {
    params: { classId, status },
  });
  return response.data;
}

export async function addStudentHold(studentId, holdCode) {
  const response = await api.post("/demo/monitor/student-hold", null, {
    params: { studentId, holdCode },
  });
  return response.data;
}

export async function recheckActiveSessions() {
  const response = await api.post("/demo/monitor/recheck");
  return response.data;
}
