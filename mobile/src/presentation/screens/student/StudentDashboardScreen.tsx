import { useEffect } from "react";
import { StyleSheet, View } from "react-native";
import { dependencies } from "@app/di";
import { useSession } from "@app/providers/SessionProvider";
import { AppButton } from "@presentation/components/AppButton";
import { AppScreen } from "@presentation/components/AppScreen";
import { AppText } from "@presentation/components/AppText";
import { Card } from "@presentation/components/Card";
import { ErrorBanner } from "@presentation/components/ErrorBanner";
import { MetricCard } from "@presentation/components/MetricCard";
import { useAsyncAction } from "@presentation/hooks/useAsyncAction";
import { spacing } from "@core/theme/theme";

export function StudentDashboardScreen() {
  const { logout } = useSession();
  const action = useAsyncAction(() => dependencies.students.getMyDashboard());

  useEffect(() => {
    action.execute().catch(() => undefined);
  }, []);

  const profile = action.result?.profile;
  const revoked = action.result?.sessions.find((session) => session.status === "REVOKED");

  return (
    <AppScreen>
      <AppText variant="label">student dashboard</AppText>
      <AppText variant="title">Dashboard ca nhan</AppText>
      <ErrorBanner error={action.error} />
      {revoked ? (
        <Card>
          <AppText variant="subtitle">Phien cua ban da bi thu hoi</AppText>
          <AppText variant="body">Ly do: {revoked.revokeReason || "ONGOING policy failed"}</AppText>
        </Card>
      ) : null}
      {profile ? (
        <>
          <Card>
            <AppText variant="subtitle">{profile.fullName}</AppText>
            <Info label="Ma sinh vien" value={profile.studentId} />
            <Info label="Email" value={profile.email} />
            <Info label="Nganh" value={profile.major} />
            <Info label="Khoa" value={profile.cohort} />
            <AppButton tone="secondary" onPress={logout} style={styles.logout}>
              Logout
            </AppButton>
          </Card>
          <View style={styles.metrics}>
            <MetricCard label="Tin chi hien tai" value={profile.currentCredits} />
            <MetricCard label="Tin chi hoan thanh" value={profile.completedCredits ?? 0} />
            <MetricCard label="Cong no hoc phi" value={`${formatCurrency(profile.tuitionDebt)} VND`} />
            <MetricCard label="So lan register" value={profile.registerAttemptCount ?? 0} />
            <MetricCard label="So lan drop" value={profile.dropCountForSemester ?? 0} />
            <MetricCard label="Lop da dang ky" value={action.result?.registeredClasses.length ?? 0} />
            <MetricCard label="Lop co the dang ky" value={action.result?.availableClasses.length ?? 0} />
          </View>
          <Card>
            <AppText variant="subtitle">Trang thai hold</AppText>
            <AppText variant="body">{profile.holds || "Khong co hold/canh bao."}</AppText>
          </Card>
        </>
      ) : null}
    </AppScreen>
  );
}

function Info({ label, value }: { label: string; value: string | number }) {
  return (
    <View style={styles.info}>
      <AppText variant="muted">{label}</AppText>
      <AppText variant="body" style={styles.strong}>
        {value}
      </AppText>
    </View>
  );
}

function formatCurrency(value: number) {
  return new Intl.NumberFormat("vi-VN").format(value);
}

const styles = StyleSheet.create({
  metrics: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: spacing.md,
    marginBottom: spacing.lg,
  },
  info: {
    paddingVertical: spacing.xs,
  },
  strong: {
    fontWeight: "800",
  },
  logout: {
    marginTop: spacing.md,
  },
});
