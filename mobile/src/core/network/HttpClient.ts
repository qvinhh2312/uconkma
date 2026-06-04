import axios, { AxiosInstance } from "axios";
import { environment } from "@core/config/environment";
import { TokenStorage } from "@core/storage/TokenStorage";

export function createHttpClient(tokenStorage: TokenStorage): AxiosInstance {
  const client = axios.create({
    baseURL: environment.apiBaseUrl,
    timeout: environment.requestTimeoutMs,
  });

  client.interceptors.request.use(async (config) => {
    const session = await tokenStorage.getSession();
    if (session?.token) {
      config.headers.Authorization = `Bearer ${session.token}`;
    }
    return config;
  });

  return client;
}
