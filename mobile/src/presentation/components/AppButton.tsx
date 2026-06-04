import { ReactNode } from "react";
import { ActivityIndicator, Pressable, StyleSheet, Text, ViewStyle } from "react-native";
import { colors, radius, spacing } from "@core/theme/theme";

export function AppButton({
  children,
  onPress,
  loading = false,
  disabled = false,
  tone = "primary",
  style,
}: {
  children: ReactNode;
  onPress: () => void;
  loading?: boolean;
  disabled?: boolean;
  tone?: "primary" | "secondary" | "danger";
  style?: ViewStyle;
}) {
  return (
    <Pressable
      disabled={disabled || loading}
      onPress={onPress}
      style={({ pressed }) => [
        styles.button,
        styles[tone],
        (pressed || disabled || loading) && styles.pressed,
        style,
      ]}
    >
      {loading ? <ActivityIndicator color={tone === "secondary" ? colors.ink : colors.paper} /> : null}
      <Text style={[styles.text, tone === "secondary" && styles.secondaryText]}>{children}</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  button: {
    alignItems: "center",
    borderRadius: radius.md,
    flexDirection: "row",
    gap: spacing.sm,
    justifyContent: "center",
    minHeight: 48,
    paddingHorizontal: spacing.lg,
  },
  primary: {
    backgroundColor: colors.ink,
  },
  secondary: {
    backgroundColor: colors.paper,
    borderColor: "rgba(16,32,26,0.16)",
    borderWidth: 1,
  },
  danger: {
    backgroundColor: colors.red,
  },
  pressed: {
    opacity: 0.68,
  },
  text: {
    color: colors.paper,
    fontSize: 15,
    fontWeight: "800",
  },
  secondaryText: {
    color: colors.ink,
  },
});
