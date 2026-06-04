import { useEffect } from "react";
import { StyleSheet, View } from "react-native";
import { dependencies } from "@app/di";
import { AppButton } from "@presentation/components/AppButton";
import { AppScreen } from "@presentation/components/AppScreen";
import { AppText } from "@presentation/components/AppText";
import { Card } from "@presentation/components/Card";
import { ErrorBanner } from "@presentation/components/ErrorBanner";
import { JsonPanel } from "@presentation/components/JsonPanel";
import { MetricCard } from "@presentation/components/MetricCard";
import { useAsyncAction } from "@presentation/hooks/useAsyncAction";
import { useSession } from "@app/providers/SessionProvider";
import { spacing } from "@core/theme/theme";

export function AdminDashboardScreen() {
  const { logout } = useSession();
  const dashboard = useAsyncAction(() => dependencies.admin.getDashboard());

  useEffect(() => {
    dashboard.execute().catch(() => undefined);
  }, []);

  const data = dashboard.result;

  return (
    <AppScreen>
      <AppText variant="label">admin portal</AppText>
      <AppText variant="title">Admin Dashboard</AppText>
      <ErrorBanner error={dashboard.error} />
      <View style={styles.actions}>
        <AppButton tone="secondary" onPress={() => dashboard.execute()}>
          Refresh
        </AppButton>
        <AppButton tone="secondary" onPress={logout}>
          Logout
        </AppButton>
      </View>
      {data ? (
        <>
          <View style={styles.metrics}>
            <MetricCard label="Policies ACTIVE" value={data.policySummary.ACTIVE ?? 0} />
            <MetricCard label="Students" value={data.domainSummary.students ?? 0} />
            <MetricCard label="Classes" value={data.domainSummary.classes ?? 0} />
            <MetricCard label="Registrations" value={data.domainSummary.registrations ?? 0} />
            <MetricCard label="Active sessions" value={data.runtimeSummary.activeSessions ?? 0} />
            <MetricCard label="Revoked sessions" value={data.runtimeSummary.revokedSessions ?? 0} />
          </View>
          <Card>
            <AppText variant="subtitle">UCON coverage</AppText>
            <AppText variant="body">Authorization: {data.uconCoverage.authorization}</AppText>
            <AppText variant="body">Obligation: {data.uconCoverage.obligation}</AppText>
            <AppText variant="body">Condition: {data.uconCoverage.condition}</AppText>
            <AppText variant="muted">{data.uconCoverage.variants.join(", ")}</AppText>
          </Card>
          <JsonPanel title="Environment" data={data.environment} />
          <JsonPanel title="Last recheck" data={data.lastRecheck} />
        </>
      ) : null}
    </AppScreen>
  );
}

const styles = StyleSheet.create({
  actions: {
    flexDirection: "row",
    gap: spacing.md,
    marginBottom: spacing.md,
  },
  metrics: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: spacing.md,
    marginBottom: spacing.lg,
  },
});
