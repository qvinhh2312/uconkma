import { useState } from "react";
import { StyleSheet, Switch, View } from "react-native";
import { dependencies } from "@app/di";
import { useDecisionHistory } from "@app/providers/DecisionProvider";
import { colors, spacing } from "@core/theme/theme";
import { AppButton } from "@presentation/components/AppButton";
import { AppScreen } from "@presentation/components/AppScreen";
import { AppText } from "@presentation/components/AppText";
import { Card } from "@presentation/components/Card";
import { DecisionBadge } from "@presentation/components/DecisionBadge";
import { ErrorBanner } from "@presentation/components/ErrorBanner";
import { Field } from "@presentation/components/Field";
import { SnapshotDiff } from "@presentation/components/SnapshotDiff";
import { TraceTree } from "@presentation/components/TraceTree";
import { useAsyncAction } from "@presentation/hooks/useAsyncAction";

export function RegisterDropScreen() {
  const { setLatestDecision } = useDecisionHistory();
  const [requestId, setRequestId] = useState(`REQ-${Date.now()}`);
  const [studentId, setStudentId] = useState("SV001");
  const [classId, setClassId] = useState("CS102_01");
  const [confirmed, setConfirmed] = useState(false);
  const [sessionLeaseValid, setSessionLeaseValid] = useState(true);
  const [adminOverride, setAdminOverride] = useState(false);
  const [overrideReason, setOverrideReason] = useState("");

  const registerAction = useAsyncAction(async () => {
    const decision = await dependencies.registration.register({
      requestId,
      studentId,
      classId,
      confirmedRegistrationRule: confirmed,
      adminOverride,
      overrideReason,
      sessionLeaseValid,
    });
    setLatestDecision(decision);
    return decision;
  });

  const dropAction = useAsyncAction(async () => {
    const decision = await dependencies.registration.drop({
      requestId,
      studentId,
      classId,
      sessionLeaseValid,
    });
    setLatestDecision(decision);
    return decision;
  });

  const result = registerAction.result ?? dropAction.result;

  return (
    <AppScreen>
      <AppText variant="label">PEP request simulator</AppText>
      <AppText variant="title">Register / Drop</AppText>
      <ErrorBanner error={registerAction.error ?? dropAction.error} />
      <Card>
        <Field label="requestId" value={requestId} onChangeText={setRequestId} />
        <Field label="studentId" value={studentId} onChangeText={setStudentId} />
        <Field label="classId" value={classId} onChangeText={setClassId} />
        <ToggleRow label="confirmedRegistrationRule" value={confirmed} onValueChange={setConfirmed} />
        <ToggleRow label="sessionLeaseValid" value={sessionLeaseValid} onValueChange={setSessionLeaseValid} />
        <ToggleRow label="adminOverride" value={adminOverride} onValueChange={setAdminOverride} />
        <Field label="overrideReason" value={overrideReason} onChangeText={setOverrideReason} />
        <View style={styles.actions}>
          <AppButton loading={registerAction.loading} onPress={() => registerAction.execute()}>
            REGISTER
          </AppButton>
          <AppButton tone="secondary" loading={dropAction.loading} onPress={() => dropAction.execute()}>
            DROP
          </AppButton>
        </View>
      </Card>
      {result ? (
        <>
          <Card>
            <DecisionBadge decision={result.decision} />
            <AppText variant="subtitle" style={styles.resultTitle}>
              {result.failedPolicy || result.message || "UCON decision completed"}
            </AppText>
            <AppText variant="body">Phase: {result.phase ?? "-"}</AppText>
            <AppText variant="body">Predicate: {result.predicate ?? "-"}</AppText>
            <AppText variant="body">Reason: {result.denyReason ?? "-"}</AppText>
            <AppText variant="body">Session: {result.sessionStatus ?? "-"}</AppText>
          </Card>
          <TraceTree trace={result.decisionTrace} />
          <Card>
            <AppText variant="subtitle">Snapshot diff</AppText>
            <SnapshotDiff response={result} />
          </Card>
        </>
      ) : null}
    </AppScreen>
  );
}

function ToggleRow({ label, value, onValueChange }: { label: string; value: boolean; onValueChange(value: boolean): void }) {
  return (
    <View style={styles.toggle}>
      <AppText variant="body">{label}</AppText>
      <Switch
        value={value}
        onValueChange={onValueChange}
        trackColor={{ true: colors.moss, false: "rgba(16,32,26,0.2)" }}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  toggle: {
    alignItems: "center",
    flexDirection: "row",
    justifyContent: "space-between",
    marginBottom: spacing.md,
  },
  actions: {
    flexDirection: "row",
    gap: spacing.md,
  },
  resultTitle: {
    marginBottom: spacing.sm,
    marginTop: spacing.md,
  },
});
