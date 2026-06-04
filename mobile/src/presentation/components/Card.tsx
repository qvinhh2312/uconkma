import { ReactNode } from "react";
import { StyleSheet, View } from "react-native";
import { colors, radius, spacing } from "@core/theme/theme";

export function Card({ children }: { children: ReactNode }) {
  return <View style={styles.card}>{children}</View>;
}

const styles = StyleSheet.create({
  card: {
    backgroundColor: colors.paper,
    borderRadius: radius.lg,
    padding: spacing.lg,
    marginBottom: spacing.lg,
    borderWidth: 1,
    borderColor: "rgba(16,32,26,0.08)",
  },
});
