import { useEffect } from "react";
import { dependencies } from "@app/di";
import { AppButton } from "@presentation/components/AppButton";
import { AppScreen } from "@presentation/components/AppScreen";
import { AppText } from "@presentation/components/AppText";
import { Card } from "@presentation/components/Card";
import { ErrorBanner } from "@presentation/components/ErrorBanner";
import { useAsyncAction } from "@presentation/hooks/useAsyncAction";

export function PapLifecycleScreen() {
  const policies = useAsyncAction(() => dependencies.pap.listPolicies());
  const summary = useAsyncAction(() => dependencies.pap.getSummary());
  const reload = useAsyncAction(() => dependencies.pap.reloadPolicies());

  useEffect(() => {
    policies.execute().catch(() => undefined);
    summary.execute().catch(() => undefined);
  }, []);

  return (
    <AppScreen>
      <AppText variant="label">policy administration point</AppText>
      <AppText variant="title">PAP Lifecycle</AppText>
      <ErrorBanner error={policies.error ?? summary.error ?? reload.error} />
      <Card>
        <AppText variant="subtitle">Runtime summary</AppText>
        {summary.result
          ? Object.entries(summary.result).map(([key, value]) => (
              <AppText key={key} variant="body">
                {key}: {value}
              </AppText>
            ))
          : null}
        <AppButton tone="secondary" loading={reload.loading} onPress={() => reload.execute()}>
          Reload policy model
        </AppButton>
      </Card>
      {policies.result?.slice(0, 25).map((policy) => (
        <Card key={policy.policyId}>
          <AppText variant="subtitle">{policy.policyId}</AppText>
          <AppText variant="body">Status: {policy.status ?? "-"}</AppText>
          <AppText variant="body">Phase: {policy.phase ?? "-"}</AppText>
          <AppText variant="body">Predicate: {policy.predicate ?? "-"}</AppText>
        </Card>
      ))}
    </AppScreen>
  );
}
