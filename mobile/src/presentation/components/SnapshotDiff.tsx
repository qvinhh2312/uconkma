import { StyleSheet, View } from "react-native";
import { ApiDecisionResponse } from "@domain/entities/Decision";
import { colors, radius, spacing } from "@core/theme/theme";
import { AppText } from "./AppText";

export function SnapshotDiff({ response }: { response?: ApiDecisionResponse | null }) {
  const trace = response?.decisionTrace;
  const before = trace?.snapshotBefore ?? {};
  const after = trace?.snapshotAfter ?? {};
  const keys = Array.from(new Set([...Object.keys(before), ...Object.keys(after)])).filter(
    (key) => JSON.stringify(before[key]) !== JSON.stringify(after[key]),
  );

  if (!keys.length) {
    return (
      <View style={styles.empty}>
        <AppText variant="muted">No mutable attribute diff in this decision.</AppText>
      </View>
    );
  }

  return (
    <View style={styles.wrapper}>
      {keys.map((key) => (
        <View key={key} style={styles.row}>
          <AppText variant="body" style={styles.key}>
            {key}
          </AppText>
          <AppText variant="muted">
            {String(before[key] ?? "-")} {"->"} {String(after[key] ?? "-")}
          </AppText>
        </View>
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  wrapper: {
    gap: spacing.sm,
  },
  row: {
    backgroundColor: colors.white,
    borderRadius: radius.md,
    padding: spacing.md,
  },
  key: {
    fontWeight: "800",
  },
  empty: {
    backgroundColor: colors.white,
    borderRadius: radius.md,
    padding: spacing.md,
  },
});
