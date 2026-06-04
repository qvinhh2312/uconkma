import { StyleSheet, View } from "react-native";
import { AppError } from "@core/errors/AppError";
import { colors, radius, spacing } from "@core/theme/theme";
import { AppText } from "./AppText";

export function ErrorBanner({ error }: { error?: AppError | null }) {
  if (!error) return null;
  return (
    <View style={styles.container}>
      <AppText variant="subtitle" style={styles.title}>
        {error.code}
      </AppText>
      <AppText style={styles.message}>{error.message}</AppText>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    backgroundColor: "#FBE8E8",
    borderColor: "rgba(181,60,60,0.25)",
    borderRadius: radius.md,
    borderWidth: 1,
    marginBottom: spacing.lg,
    padding: spacing.md,
  },
  title: {
    color: colors.red,
    fontSize: 15,
  },
  message: {
    color: colors.red,
    marginTop: spacing.xs,
  },
});
