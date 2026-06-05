import { useEffect } from "react";
import { StyleSheet, View } from "react-native";
import { dependencies } from "@app/di";
import { AppButton } from "@presentation/components/AppButton";
import { AppScreen } from "@presentation/components/AppScreen";
import { AppText } from "@presentation/components/AppText";
import { Card } from "@presentation/components/Card";
import { ErrorBanner } from "@presentation/components/ErrorBanner";
import { MetricCard } from "@presentation/components/MetricCard";
import { useAsyncAction } from "@presentation/hooks/useAsyncAction";
import { spacing } from "@core/theme/theme";

export function AdminDashboardScreen() {
  const dashboard = useAsyncAction(() => dependencies.admin.getDashboard());

  useEffect(() => {
    dashboard.execute().catch(() => undefined);
  }, []);

  const data = dashboard.result;

  return (
    <AppScreen>
      <View style={styles.header}>
        <AppText variant="title">Dashboard</AppText>
        <AppButton tone="secondary" onPress={() => dashboard.execute()}>
          Refresh
        </AppButton>
      </View>
      <ErrorBanner error={dashboard.error} />
      {data ? (
        <>
          <View style={styles.metrics}>
            <MetricCard label="Students" value={data.domainSummary.students ?? 0} />
            <MetricCard label="Classes" value={data.domainSummary.classes ?? 0} />
            <MetricCard label="Registrations" value={data.domainSummary.registrations ?? 0} />
            <MetricCard label="Active sessions" value={data.runtimeSummary.activeSessions ?? 0} />
            <MetricCard label="Revoked" value={data.runtimeSummary.revokedSessions ?? 0} />
            <MetricCard label="Số lớp còn mở" value={data.domainSummary.openClasses ?? 0} />
          </View>
          <Card>
            <AppText variant="subtitle">UCON coverage</AppText>
            <AppText variant="body">Authorization: {data.uconCoverage.authorization}</AppText>
            <AppText variant="body">Obligation: {data.uconCoverage.obligation}</AppText>
            <AppText variant="body">Condition: {data.uconCoverage.condition}</AppText>
            <AppText variant="muted">{data.uconCoverage.variants.join(", ")}</AppText>
          </Card>
        </>
      ) : null}
    </AppScreen>
  );
}

const styles = StyleSheet.create({
  header: {
    alignItems: "center",
    flexDirection: "row",
    justifyContent: "space-between",
    marginBottom: spacing.md,
  },
  metrics: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: spacing.md,
    marginBottom: spacing.lg,
  },
});
