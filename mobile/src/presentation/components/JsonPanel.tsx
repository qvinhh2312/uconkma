import { StyleSheet, View } from "react-native";
import { colors, radius, spacing } from "@core/theme/theme";
import { AppText } from "./AppText";

export function JsonPanel({ title, data }: { title: string; data: unknown }) {
  return (
    <View style={styles.panel}>
      <AppText variant="subtitle">{title}</AppText>
      <View style={styles.code}>
        <AppText style={styles.text}>{JSON.stringify(data, null, 2)}</AppText>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  panel: {
    backgroundColor: colors.ink,
    borderRadius: radius.lg,
    marginBottom: spacing.lg,
    padding: spacing.md,
  },
  code: {
    backgroundColor: "rgba(0,0,0,0.24)",
    borderRadius: radius.md,
    marginTop: spacing.md,
    padding: spacing.md,
  },
  text: {
    color: colors.paper,
    fontFamily: "monospace",
    fontSize: 12,
  },
});
