import { api } from "./client.js";

export async function getDemoState(studentId = "SV001", classId = "CS102_01") {
  const response = await api.get("/demo/state", {
    params: { studentId, classId },
  });
  return response.data;
}
