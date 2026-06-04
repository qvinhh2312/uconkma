import { StyleSheet, Text, View } from "react-native";
import { colors, radius, spacing } from "@core/theme/theme";

export function DecisionBadge({ decision }: { decision?: string }) {
  const normalized = (decision ?? "UNKNOWN").toUpperCase();
  const tone = normalized === "ALLOW" || normalized === "PERMIT" || normalized === "COMMITTED" ? "permit" : "deny";
  return (
    <View style={[styles.badge, styles[tone]]}>
      <Text style={styles.text}>{normalized}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  badge: {
    alignSelf: "flex-start",
    borderRadius: radius.sm,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
  },
  permit: {
    backgroundColor: colors.moss,
  },
  deny: {
    backgroundColor: colors.red,
  },
  text: {
    color: colors.paper,
    fontSize: 12,
    fontWeight: "900",
  },
});
