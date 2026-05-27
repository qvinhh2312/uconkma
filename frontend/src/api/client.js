import axios from "axios";

export const api = axios.create({
  baseURL: "http://localhost:8080/api",
  timeout: 15000,
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
