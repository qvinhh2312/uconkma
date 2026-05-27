import { api } from "./client.js";

export async function registerCourse(payload) {
  const response = await api.post("/register", payload);
  return response.data;
}

export async function dropCourse(payload) {
  const response = await api.post("/drop", payload);
  return response.data;
}
