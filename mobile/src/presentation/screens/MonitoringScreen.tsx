import { useState } from "react";
import { StyleSheet, View } from "react-native";
import { dependencies } from "@app/di";
import { MonitoringResult } from "@domain/entities/Monitoring";
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

  const action = useAsyncAction(async (operation: () => Promise<MonitoringResult>) => operation());
  const result = action.result;

  return (
    <AppScreen>
      <AppText variant="label">ongoing continuity</AppText>
      <AppText variant="title">Monitoring Demo</AppText>
      <ErrorBanner error={action.error} />
      <Card>
        <AppText variant="subtitle">Maintenance</AppText>
        <View style={styles.row}>
          <AppButton loading={action.loading} onPress={() => action.execute(() => dependencies.monitoring.setMaintenance(true))}>
            ON
          </AppButton>
          <AppButton tone="secondary" loading={action.loading} onPress={() => action.execute(() => dependencies.monitoring.setMaintenance(false))}>
            OFF
          </AppButton>
        </View>
      </Card>
      <Card>
        <AppText variant="subtitle">Class status</AppText>
        <Field label="classId" value={classId} onChangeText={setClassId} />
        <Field label="status" value={classStatus} onChangeText={setClassStatus} />
        <AppButton loading={action.loading} onPress={() => action.execute(() => dependencies.monitoring.changeClassStatus(classId, classStatus))}>
          Change class status
        </AppButton>
      </Card>
      <Card>
        <AppText variant="subtitle">Student hold</AppText>
        <Field label="studentId" value={studentId} onChangeText={setStudentId} />
        <Field label="holdCode" value={holdCode} onChangeText={setHoldCode} />
        <AppButton loading={action.loading} onPress={() => action.execute(() => dependencies.monitoring.addStudentHold(studentId, holdCode))}>
          Add hold
        </AppButton>
      </Card>
      <Card>
        <AppText variant="subtitle">Manual recheck</AppText>
        <AppButton tone="secondary" loading={action.loading} onPress={() => action.execute(() => dependencies.monitoring.recheckActiveSessions())}>
          Recheck all active sessions
        </AppButton>
      </Card>
      {result ? (
        <View style={styles.metrics}>
          <MetricCard label="Checked sessions" value={result.checkedSessions} />
          <MetricCard label="Revoked sessions" value={result.revokedSessions} />
        </View>
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
