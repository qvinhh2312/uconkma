import Constants from "expo-constants";
import { Platform } from "react-native";

type ExtraConfig = {
  apiBaseUrl?: string;
};

const extra = (Constants.expoConfig?.extra ?? {}) as ExtraConfig;

export const environment = {
  apiBaseUrl:
    extra.apiBaseUrl ??
    (Platform.OS === "android" ? "http://10.0.2.2:8080/api" : "http://localhost:8080/api"),
  requestTimeoutMs: 15000,
};
