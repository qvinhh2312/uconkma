import { useEffect } from "react";
import { dependencies } from "@app/di";
import { AppButton } from "@presentation/components/AppButton";
import { AppScreen } from "@presentation/components/AppScreen";
import { AppText } from "@presentation/components/AppText";
import { Card } from "@presentation/components/Card";
import { ErrorBanner } from "@presentation/components/ErrorBanner";
import { JsonPanel } from "@presentation/components/JsonPanel";
import { useAsyncAction } from "@presentation/hooks/useAsyncAction";

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
      <AppText variant="label">verification</AppText>
      <AppText variant="title">Validation / Analyzer / Benchmark</AppText>
      <ErrorBanner error={validation.error ?? analyzer.error ?? benchmark.error} />
      <Card>
        <AppText variant="subtitle">Release evidence</AppText>
        <AppText variant="body">Validation: {String(validation.result?.status ?? "-")}</AppText>
        <AppText variant="body">Analyzer: {String(analyzer.result?.status ?? "-")}</AppText>
        <AppText variant="body">Line coverage: {String(validation.result?.lineCoverage ?? "-")}</AppText>
        <AppButton loading={validation.loading || analyzer.loading || benchmark.loading} onPress={refresh}>
          Refresh reports
        </AppButton>
      </Card>
      <JsonPanel title="Validation" data={validation.result ?? {}} />
      <JsonPanel title="Analyzer" data={analyzer.result ?? {}} />
      <JsonPanel title="Benchmark" data={benchmark.result ?? {}} />
    </AppScreen>
  );
}
