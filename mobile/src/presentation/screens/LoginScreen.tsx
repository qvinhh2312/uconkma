import { useState } from "react";
import { KeyboardAvoidingView, Platform, StyleSheet, View } from "react-native";
import { colors, spacing } from "@core/theme/theme";
import { useSession } from "@app/providers/SessionProvider";
import { AppButton } from "@presentation/components/AppButton";
import { AppText } from "@presentation/components/AppText";
import { Card } from "@presentation/components/Card";
import { ErrorBanner } from "@presentation/components/ErrorBanner";
import { Field } from "@presentation/components/Field";
import { useAsyncAction } from "@presentation/hooks/useAsyncAction";

export function LoginScreen() {
  const { login } = useSession();
  const [username, setUsername] = useState("sv001");
  const [password, setPassword] = useState("student123");
  const { execute, loading, error } = useAsyncAction(login);

  return (
    <KeyboardAvoidingView behavior={Platform.OS === "ios" ? "padding" : undefined} style={styles.root}>
      <View style={styles.center}>
        <Card>
          <AppText variant="label">UCONKMA mobile</AppText>
          <AppText variant="title" style={styles.title}>
            Login
          </AppText>
          <AppText variant="muted" style={styles.copy}>
            Admin xem danh sach sinh vien/diem. Sinh vien chi xem ho so cua minh va gui REGISTER/DROP qua UCON.
          </AppText>
          <ErrorBanner error={error} />
          <Field label="username" value={username} onChangeText={setUsername} />
          <Field label="password" value={password} onChangeText={setPassword} secureTextEntry />
          <AppButton loading={loading} onPress={() => execute({ username, password })}>
            Login
          </AppButton>
          <View style={styles.demo}>
            <AppText variant="muted">Admin: admin/admin123</AppText>
            <AppText variant="muted">Student: sv001..sv010/student123</AppText>
          </View>
        </Card>
      </View>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: colors.sand,
  },
  center: {
    flex: 1,
    justifyContent: "center",
    padding: spacing.lg,
  },
  title: {
    marginTop: spacing.sm,
  },
  copy: {
    marginBottom: spacing.lg,
    marginTop: spacing.sm,
  },
  demo: {
    gap: spacing.xs,
    marginTop: spacing.lg,
  },
});
