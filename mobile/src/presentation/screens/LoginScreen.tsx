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
          <AppText variant="title" style={styles.title}>
            LOGIN
          </AppText>
          <ErrorBanner error={error} />
          <Field label="username" value={username} onChangeText={setUsername} />
          <Field label="password" value={password} onChangeText={setPassword} secureTextEntry />
          <AppButton loading={loading} onPress={() => execute({ username, password })}>
            Login
          </AppButton>
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
    marginBottom: spacing.lg,
    textAlign: "center",
  },
});
