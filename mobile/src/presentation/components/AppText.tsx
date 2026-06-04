import { ReactNode } from "react";
import { StyleSheet, Text, TextStyle } from "react-native";
import { colors } from "@core/theme/theme";

export function AppText({
  children,
  variant = "body",
  style,
}: {
  children: ReactNode;
  variant?: "title" | "subtitle" | "body" | "label" | "muted";
  style?: TextStyle;
}) {
  return <Text style={[styles[variant], style]}>{children}</Text>;
}

const styles = StyleSheet.create({
  title: {
    color: colors.ink,
    fontSize: 28,
    fontWeight: "800",
    letterSpacing: -0.6,
  },
  subtitle: {
    color: colors.ink,
    fontSize: 20,
    fontWeight: "700",
  },
  body: {
    color: colors.ink,
    fontSize: 15,
    lineHeight: 22,
  },
  label: {
    color: colors.clay,
    fontSize: 11,
    fontWeight: "800",
    letterSpacing: 1.8,
    textTransform: "uppercase",
  },
  muted: {
    color: colors.inkSoft,
    fontSize: 13,
    lineHeight: 19,
  },
});
