import { api } from "./client.js";

export async function login(payload) {
  const response = await api.post("/auth/login", payload);
  return response.data;
}

export async function logout() {
  const response = await api.post("/auth/logout");
  return response.data;
}

export async function getCurrentUser() {
  const response = await api.get("/auth/me");
  return response.data;
}

