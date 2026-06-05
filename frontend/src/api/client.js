import axios from "axios";
import { clearSession, getSession } from "../auth/session.js";

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

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const data = error.response?.data;
    if (isAuthenticationError(data)) {
      clearSession();
      if (window.location.pathname !== "/login") {
        window.location.assign("/login");
      }
    }
    return Promise.reject(error);
  },
);

function isAuthenticationError(data) {
  const message = String(data?.message || "").toLowerCase();
  const code = String(data?.errorCode || data?.code || "").toUpperCase();
  return (
    code === "AUTHENTICATION_REQUIRED" ||
    code === "UNAUTHORIZED" ||
    message.includes("authentication token is required") ||
    message.includes("invalid authentication token")
  );
}

export function normalizeApiError(error) {
  if (error.response?.data) {
    return error.response.data;
  }
  return {
    errorCode: "NETWORK_ERROR",
    message: error.message || "Cannot connect to backend",
  };
}
