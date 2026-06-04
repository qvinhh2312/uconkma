import { AxiosError } from "axios";
import { AppError, AppErrorCode } from "./AppError";

type BackendError = {
  errorCode?: string;
  code?: string;
  message?: string;
  error?: string;
  status?: number;
};

export function normalizeError(error: unknown): AppError {
  if (error instanceof AppError) {
    return error;
  }

  const axiosError = error as AxiosError<BackendError>;

  if (axiosError.response) {
    const data = axiosError.response.data ?? {};
    const code = data.errorCode ?? data.code ?? "BACKEND_ERROR";
    const message = data.message ?? data.error ?? `HTTP ${axiosError.response.status}`;
    return new AppError(mapCode(code, axiosError.response.status), message, axiosError.response.status, data);
  }

  if (axiosError.request) {
    return new AppError("NETWORK_ERROR", "Cannot connect to UCON backend. Check backend URL and network.");
  }

  if (error instanceof Error) {
    return new AppError("UNKNOWN_ERROR", error.message);
  }

  return new AppError("UNKNOWN_ERROR", "Unexpected application error.");
}

function mapCode(code: string, status?: number): AppErrorCode {
  if (status === 401) return "UNAUTHORIZED";
  if (status === 403) return "FORBIDDEN";
  if (code === "INVALID_ARGUMENT") return "INVALID_ARGUMENT";
  return "BACKEND_ERROR";
}
