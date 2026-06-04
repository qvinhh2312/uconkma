import { TextInput, TextInputProps, StyleSheet, View } from "react-native";
import { AppText } from "./AppText";
import { colors, radius, spacing } from "@core/theme/theme";

export function Field({ label, ...props }: TextInputProps & { label: string }) {
  return (
    <View style={styles.wrapper}>
      <AppText variant="label">{label}</AppText>
      <TextInput
        autoCapitalize="none"
        placeholderTextColor="rgba(16,32,26,0.42)"
        style={styles.input}
        {...props}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  wrapper: {
    gap: spacing.sm,
    marginBottom: spacing.md,
  },
  input: {
    backgroundColor: colors.white,
    borderColor: "rgba(16,32,26,0.12)",
    borderRadius: radius.md,
    borderWidth: 1,
    color: colors.ink,
    fontSize: 16,
    minHeight: 48,
    paddingHorizontal: spacing.md,
  },
});
