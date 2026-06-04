import { StyleSheet, View } from "react-native";
import { colors, radius, spacing } from "@core/theme/theme";
import { AppText } from "./AppText";

export function MetricCard({ label, value }: { label: string; value: string | number }) {
  return (
    <View style={styles.card}>
      <AppText variant="muted">{label}</AppText>
      <AppText variant="subtitle" style={styles.value}>
        {value}
      </AppText>
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    backgroundColor: colors.paper,
    borderRadius: radius.lg,
    flex: 1,
    minWidth: "46%",
    padding: spacing.lg,
  },
  value: {
    marginTop: spacing.sm,
  },
});
