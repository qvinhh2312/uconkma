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

export function AdminReportsScreen() {
  const validation = useAsyncAction(() => dependencies.admin.getValidationReport());
  const analyzer = useAsyncAction(() => dependencies.admin.getAnalyzerReport());
  const benchmark = useAsyncAction(() => dependencies.admin.getBenchmarkReport());

  useEffect(() => {
    refresh().catch(() => undefined);
  }, []);

  async function refresh() {
    await Promise.all([
      validation.execute().catch(() => undefined),
      analyzer.execute().catch(() => undefined),
      benchmark.execute().catch(() => undefined),
    ]);
  }

  return (
    <AppScreen>
      <AppText variant="title">Reports</AppText>
      <ErrorBanner error={validation.error ?? analyzer.error ?? benchmark.error} />
      <Card>
        <AppText variant="subtitle">Release evidence</AppText>
        <View style={styles.metrics}>
          <MetricCard label="Engine tests" value={Number(validation.result?.engineTests ?? 0)} />
          <MetricCard label="DSL tests" value={Number(validation.result?.dslTests ?? 0)} />
          <MetricCard label="Line coverage" value={String(validation.result?.lineCoverage ?? "-")} />
          <MetricCard label="Branch coverage" value={String(validation.result?.branchCoverage ?? "-")} />
        </View>
        <AppButton loading={validation.loading || analyzer.loading || benchmark.loading} onPress={refresh}>
          Refresh reports
        </AppButton>
      </Card>
      <Card>
        <AppText variant="subtitle">Validation</AppText>
        <AppText variant="body">Status: {String(validation.result?.status ?? "-")}</AppText>
        <AppText variant="body">DSL policies: {String(validation.result?.dslPolicies ?? "-")}</AppText>
        <AppText variant="body">XMI policies: {String(validation.result?.xmiPolicies ?? "-")}</AppText>
        <AppText variant="body">Missing required attributes: {String(validation.result?.missingRequiredPolicyAttributes ?? "-")}</AppText>
      </Card>
      <Card>
        <AppText variant="subtitle">Analyzer</AppText>
        <AppText variant="body">Status: {String(analyzer.result?.status ?? "-")}</AppText>
        <AppText variant="body">Warnings: {Array.isArray(analyzer.result?.warnings) ? analyzer.result.warnings.length : 0}</AppText>
      </Card>
      <Card>
        <AppText variant="subtitle">Benchmark</AppText>
        <AppText variant="body">REGISTER avg: {String((benchmark.result?.apiBenchmark as Record<string, unknown> | undefined)?.registerAvgMs ?? "-")} ms</AppText>
        <AppText variant="body">DROP avg: {String((benchmark.result?.apiBenchmark as Record<string, unknown> | undefined)?.dropAvgMs ?? "-")} ms</AppText>
      </Card>
    </AppScreen>
  );
}

const styles = StyleSheet.create({
  metrics: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: spacing.md,
    marginBottom: spacing.md,
  },
});
