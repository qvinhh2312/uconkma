import { useEffect, useState } from "react";
import { dependencies } from "@app/di";
import { AppButton } from "@presentation/components/AppButton";
import { AppScreen } from "@presentation/components/AppScreen";
import { AppText } from "@presentation/components/AppText";
import { Card } from "@presentation/components/Card";
import { ErrorBanner } from "@presentation/components/ErrorBanner";
import { Field } from "@presentation/components/Field";
import { JsonPanel } from "@presentation/components/JsonPanel";
import { useAsyncAction } from "@presentation/hooks/useAsyncAction";

export function PapLifecycleScreen() {
  const [policyId, setPolicyId] = useState("P20_ReserveSeat_OnA2");
  const [targetStatus, setTargetStatus] = useState("DEPRECATED");
  const policies = useAsyncAction(() => dependencies.admin.listPolicies());
  const summary = useAsyncAction(() => dependencies.admin.getPolicySummary());
  const reload = useAsyncAction(() => dependencies.admin.reloadPolicies());
  const transition = useAsyncAction((id: string, status: string) => dependencies.admin.transitionPolicy(id, status));

  useEffect(() => {
    policies.execute().catch(() => undefined);
    summary.execute().catch(() => undefined);
  }, []);

  return (
    <AppScreen>
      <AppText variant="label">policy administration point</AppText>
      <AppText variant="title">PAP Lifecycle</AppText>
      <ErrorBanner error={policies.error ?? summary.error ?? reload.error ?? transition.error} />
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
      <Card>
        <AppText variant="subtitle">Transition policy status</AppText>
        <Field label="policyId" value={policyId} onChangeText={setPolicyId} />
        <Field label="targetStatus" value={targetStatus} onChangeText={setTargetStatus} />
        <AppButton loading={transition.loading} onPress={() => transition.execute(policyId, targetStatus)}>
          Transition
        </AppButton>
      </Card>
      {transition.result ? <JsonPanel title="Transition response" data={transition.result} /> : null}
      {policies.result?.slice(0, 25).map((policy) => (
        <Card key={policy.policyId}>
          <AppText variant="subtitle">{policy.policyId}</AppText>
          <AppText variant="body">Status: {policy.policyStatus ?? policy.status ?? "-"}</AppText>
          <AppText variant="body">Phase: {policy.phase ?? "-"}</AppText>
          <AppText variant="body">Predicate: {policy.predicate ?? "-"}</AppText>
          <AppText variant="body">Variant: {policy.uconVariant ?? "-"}</AppText>
        </Card>
      ))}
    </AppScreen>
  );
}
