import { useEffect, useState } from "react";
import { StyleSheet, View } from "react-native";
import { dependencies } from "@app/di";
import { AdminMonitoringResult } from "@domain/entities/AdminPortal";
import { AppButton } from "@presentation/components/AppButton";
import { AppScreen } from "@presentation/components/AppScreen";
import { AppText } from "@presentation/components/AppText";
import { Card } from "@presentation/components/Card";
import { ErrorBanner } from "@presentation/components/ErrorBanner";
import { Field } from "@presentation/components/Field";
import { MetricCard } from "@presentation/components/MetricCard";
import { useAsyncAction } from "@presentation/hooks/useAsyncAction";
import { spacing } from "@core/theme/theme";

export function MonitoringScreen() {
  const [classId, setClassId] = useState("CS102_01");
  const [classStatus, setClassStatus] = useState("LOCKED");
  const [studentId, setStudentId] = useState("SV001");
  const [holdCode, setHoldCode] = useState("ACADEMIC_HOLD");

  const summary = useAsyncAction(() => dependencies.admin.getMonitorSummary());
  const action = useAsyncAction(async (operation: () => Promise<AdminMonitoringResult | Record<string, unknown>>) => operation());
  const result = action.result;

  useEffect(() => {
    summary.execute().catch(() => undefined);
  }, []);

  return (
    <AppScreen>
      <AppText variant="label">ongoing continuity</AppText>
      <AppText variant="title">Monitoring Demo</AppText>
      <ErrorBanner error={summary.error ?? action.error} />
      <Card>
        <AppText variant="subtitle">Monitoring summary</AppText>
        <AppButton tone="secondary" loading={summary.loading} onPress={() => summary.execute()}>
          Refresh summary
        </AppButton>
        {summary.result ? (
          <View style={styles.metrics}>
            <MetricCard label="Active" value={Number(summary.result.activeSessions ?? 0)} />
            <MetricCard label="Revoked" value={Number(summary.result.revokedSessions ?? 0)} />
          </View>
        ) : null}
      </Card>
      <Card>
        <AppText variant="subtitle">Maintenance</AppText>
        <View style={styles.row}>
          <AppButton loading={action.loading} onPress={() => action.execute(() => dependencies.admin.setMaintenance(true))}>
            ON
          </AppButton>
          <AppButton tone="secondary" loading={action.loading} onPress={() => action.execute(() => dependencies.admin.setMaintenance(false))}>
            OFF
          </AppButton>
        </View>
      </Card>
      <Card>
        <AppText variant="subtitle">Class status</AppText>
        <Field label="classId" value={classId} onChangeText={setClassId} />
        <Field label="status" value={classStatus} onChangeText={setClassStatus} />
        <AppButton loading={action.loading} onPress={() => action.execute(() => dependencies.admin.changeClassStatus(classId, classStatus))}>
          Change class status
        </AppButton>
      </Card>
      <Card>
        <AppText variant="subtitle">Student hold</AppText>
        <Field label="studentId" value={studentId} onChangeText={setStudentId} />
        <Field label="holdCode" value={holdCode} onChangeText={setHoldCode} />
        <AppButton loading={action.loading} onPress={() => action.execute(() => dependencies.admin.addStudentHold(studentId, holdCode))}>
          Add hold
        </AppButton>
        <AppButton tone="secondary" loading={action.loading} onPress={() => action.execute(() => dependencies.admin.removeStudentHold(studentId, holdCode))}>
          Remove hold
        </AppButton>
      </Card>
      <Card>
        <AppText variant="subtitle">Manual recheck</AppText>
        <AppButton tone="secondary" loading={action.loading} onPress={() => action.execute(() => dependencies.admin.recheck("MANUAL_RECHECK"))}>
          Recheck all active sessions
        </AppButton>
      </Card>
      {result ? (
        <Card>
          <AppText variant="subtitle">Kết quả recheck</AppText>
          <View style={styles.metrics}>
            <MetricCard label="Checked sessions" value={Number(result.checkedSessions ?? 0)} />
            <MetricCard label="Revoked sessions" value={Number(result.revokedSessions ?? 0)} />
          </View>
          <AppText variant="body">Trigger: {String(result.trigger ?? "-")}</AppText>
          <AppText variant="body">{String(result.message ?? "Recheck completed.")}</AppText>
        </Card>
      ) : null}
    </AppScreen>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: "row",
    gap: spacing.md,
    marginTop: spacing.md,
  },
  metrics: {
    flexDirection: "row",
    gap: spacing.md,
  },
});
