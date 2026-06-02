import axios from "axios";
import { getSession } from "../auth/session.js";

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api",
  timeout: 15000,
});

api.interceptors.request.use((config) => {
  const session = getSession();
  if (session?.token) {
    config.headers.Authorization = `Bearer ${session.token}`;
  }
  return config;
});

export function normalizeApiError(error) {
  if (error.response?.data) {
    return error.response.data;
  }
  return {
    errorCode: "NETWORK_ERROR",
    message: error.message || "Cannot connect to backend",
  };
}
