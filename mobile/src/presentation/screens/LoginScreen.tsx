import { useState } from "react";
import { KeyboardAvoidingView, Platform, Pressable, StyleSheet, View } from "react-native";
import { colors, radius, spacing } from "@core/theme/theme";
import { useSession } from "@app/providers/SessionProvider";
import { AppButton } from "@presentation/components/AppButton";
import { AppText } from "@presentation/components/AppText";
import { Card } from "@presentation/components/Card";
import { ErrorBanner } from "@presentation/components/ErrorBanner";
import { Field } from "@presentation/components/Field";
import { useAsyncAction } from "@presentation/hooks/useAsyncAction";

export function LoginScreen() {
  const { login } = useSession();
  const [roleMode, setRoleMode] = useState<"STUDENT" | "ADMIN">("STUDENT");
  const [username, setUsername] = useState("sv001");
  const [password, setPassword] = useState("student123");
  const { execute, loading, error } = useAsyncAction(login);

  function chooseRole(nextRole: "STUDENT" | "ADMIN") {
    setRoleMode(nextRole);
    if (nextRole === "ADMIN") {
      setUsername("admin");
      setPassword("admin123");
    } else {
      setUsername("sv001");
      setPassword("student123");
    }
  }

  return (
    <KeyboardAvoidingView behavior={Platform.OS === "ios" ? "padding" : undefined} style={styles.root}>
      <View style={styles.center}>
        <Card>
          <AppText variant="label">UCONKMA mobile</AppText>
          <AppText variant="title" style={styles.title}>
            Chon vai tro dang nhap
          </AppText>
          <AppText variant="muted" style={styles.copy}>
            Sinh vien xem ho so ca nhan va dang ky hoc phan. Admin kiem tra du lieu, sinh vien, lop va mo/dong dot dang ky.
          </AppText>
          <View style={styles.roleGrid}>
            <RoleCard
              active={roleMode === "STUDENT"}
              title="1. Sinh vien"
              description="Xem ho so, tin chi, du no, lop da dang ky va gui REGISTER/DROP."
              onPress={() => chooseRole("STUDENT")}
            />
            <RoleCard
              active={roleMode === "ADMIN"}
              title="2. Admin"
              description="Xem sinh vien/lop, kiem tra du lieu va mo/dong thoi gian dang ky."
              onPress={() => chooseRole("ADMIN")}
            />
          </View>
          <ErrorBanner error={error} />
          <Field label="username" value={username} onChangeText={setUsername} />
          <Field label="password" value={password} onChangeText={setPassword} secureTextEntry />
          <AppButton loading={loading} onPress={() => execute({ username, password })}>
            Login
          </AppButton>
          <AppText variant="muted" style={styles.demo}>
            Demo: sinh vien `sv001`..`sv010` / `student123`; admin `admin` / `admin123`.
          </AppText>
        </Card>
      </View>
    </KeyboardAvoidingView>
  );
}

function RoleCard({
  active,
  title,
  description,
  onPress,
}: {
  active: boolean;
  title: string;
  description: string;
  onPress: () => void;
}) {
  return (
    <Pressable onPress={onPress} style={[styles.roleCard, active && styles.roleCardActive]}>
      <AppText variant="subtitle" style={active ? styles.roleTitleActive : undefined}>
        {title}
      </AppText>
      <AppText variant="muted" style={active ? styles.roleCopyActive : undefined}>
        {description}
      </AppText>
    </Pressable>
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
  roleGrid: {
    gap: spacing.md,
    marginBottom: spacing.lg,
  },
  roleCard: {
    backgroundColor: colors.white,
    borderColor: "rgba(16,32,26,0.12)",
    borderRadius: radius.lg,
    borderWidth: 1,
    padding: spacing.md,
  },
  roleCardActive: {
    backgroundColor: colors.ink,
    borderColor: colors.ink,
  },
  roleTitleActive: {
    color: colors.paper,
  },
  roleCopyActive: {
    color: "rgba(255,249,237,0.78)",
  },
  demo: {
    marginTop: spacing.lg,
  },
});
