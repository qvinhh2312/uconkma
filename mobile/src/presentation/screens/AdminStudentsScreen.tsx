import { useEffect, useState } from "react";
import { StyleSheet, View } from "react-native";
import { dependencies } from "@app/di";
import { AppButton } from "@presentation/components/AppButton";
import { AppScreen } from "@presentation/components/AppScreen";
import { AppText } from "@presentation/components/AppText";
import { Card } from "@presentation/components/Card";
import { ErrorBanner } from "@presentation/components/ErrorBanner";
import { Field } from "@presentation/components/Field";
import { MetricCard } from "@presentation/components/MetricCard";
import { useAsyncAction } from "@presentation/hooks/useAsyncAction";
import { spacing } from "@core/theme/theme";

export function AdminStudentsScreen() {
  const students = useAsyncAction(() => dependencies.students.listStudents());
  const classes = useAsyncAction(() => dependencies.students.listClasses());
  const environment = useAsyncAction(() => dependencies.monitoring.getEnvironmentState());
  const openRegistration = useAsyncAction(() => dependencies.monitoring.openRegistrationWindow());
  const closeRegistration = useAsyncAction(() => dependencies.monitoring.closeRegistrationWindow());

  useEffect(() => {
    refresh().catch(() => undefined);
  }, []);

  async function refresh() {
    await Promise.all([
      students.execute().catch(() => undefined),
      classes.execute().catch(() => undefined),
      environment.execute().catch(() => undefined),
    ]);
  }

  async function openWindow() {
    await openRegistration.execute();
    await environment.execute();
  }

  async function closeWindow() {
    await closeRegistration.execute();
    await environment.execute();
  }

  const environmentState = closeRegistration.result ?? openRegistration.result ?? environment.result;
  const totalDebt = (students.result ?? []).reduce((sum, student) => sum + (student.tuitionDebt ?? 0), 0);

  return (
    <AppScreen>
      <AppText variant="label">admin scope</AppText>
      <AppText variant="title">Admin Console</AppText>
      <ErrorBanner error={students.error ?? classes.error ?? environment.error ?? openRegistration.error ?? closeRegistration.error} />
      <View style={styles.metrics}>
        <MetricCard label="Sinh vien" value={students.result?.length ?? "..."} />
        <MetricCard label="Lop hoc" value={classes.result?.length ?? "..."} />
        <MetricCard label="Tong du no" value={`${formatCurrency(totalDebt)} VND`} />
        <MetricCard label="Dot dang ky" value={environmentState?.registrationPhase ?? "..."} />
      </View>
      <Card>
        <AppText variant="subtitle">Quan ly thoi gian dang ky</AppText>
        <InfoRow label="Hoc ky" value={environmentState?.semester ?? "-"} />
        <InfoRow label="Trang thai dot" value={environmentState?.registrationPhase ?? "-"} />
        <InfoRow label="Maintenance" value={String(environmentState?.maintenance ?? false)} />
        <InfoRow label="Open time" value={environmentState?.openTime ?? "-"} />
        <InfoRow label="Close time" value={environmentState?.closeTime ?? "-"} />
        <View style={styles.actions}>
          <AppButton loading={openRegistration.loading} onPress={openWindow}>
            Mo dang ky
          </AppButton>
          <AppButton tone="secondary" loading={closeRegistration.loading} onPress={closeWindow}>
            Dong dang ky
          </AppButton>
        </View>
      </Card>
      <Card>
        <AppText variant="subtitle">Kiem tra lop hoc</AppText>
        {classes.result?.map((item) => (
          <View key={item.classId} style={styles.classRow}>
            <AppText variant="body" style={styles.strong}>
              {item.classId} / {item.courseId}
            </AppText>
            <AppText variant="muted">Trang thai: {item.status}</AppText>
            <AppText variant="muted">Si so: {item.enrolled + item.reservedSeats}/{item.capacity}</AppText>
            <AppText variant="muted">Lich: {item.scheduleSlots}</AppText>
          </View>
        ))}
      </Card>
      <Card>
        <AppText variant="subtitle">Doi trang thai lop nhanh</AppText>
        <ClassStatusControl />
      </Card>
      {students.result?.map((student) => (
        <Card key={student.studentId}>
          <AppText variant="subtitle">{student.studentId} - {student.fullName}</AppText>
          <InfoRow label="Nganh" value={student.major} />
          <InfoRow label="Khoa" value={student.cohort} />
          <InfoRow label="Tin chi dang hoc" value={student.currentCredits} />
          <InfoRow label="Du no" value={`${formatCurrency(student.tuitionDebt)} VND`} />
          <InfoRow label="Hoc phi" value={student.tuitionPaid ? "Da hoan tat" : "Chua hoan tat"} />
          <InfoRow label="Hold" value={student.holds || "Khong co"} />
        </Card>
      ))}
    </AppScreen>
  );
}

function ClassStatusControl() {
  const [classId, setClassId] = useState("CS102_01");
  const [status, setStatus] = useState("OPEN");
  const action = useAsyncAction(() => dependencies.monitoring.changeClassStatus(classId, status));

  return (
    <>
      <Field label="classId" value={classId} onChangeText={setClassId} />
      <Field label="status OPEN / LOCKED / CLOSED / CANCELLED" value={status} onChangeText={setStatus} />
      <AppButton loading={action.loading} onPress={() => action.execute()}>
        Cap nhat trang thai lop
      </AppButton>
      <ErrorBanner error={action.error} />
      {action.result ? (
        <AppText variant="muted">
          Checked {action.result.checkedSessions}, revoked {action.result.revokedSessions}
        </AppText>
      ) : null}
    </>
  );
}

function InfoRow({ label, value }: { label: string; value: string | number }) {
  return (
    <View style={styles.infoRow}>
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
  actions: {
    flexDirection: "row",
    gap: spacing.md,
    marginTop: spacing.md,
  },
  classRow: {
    borderBottomColor: "rgba(16,32,26,0.08)",
    borderBottomWidth: 1,
    paddingVertical: spacing.md,
  },
  infoRow: {
    borderBottomColor: "rgba(16,32,26,0.08)",
    borderBottomWidth: 1,
    paddingVertical: spacing.sm,
  },
  strong: {
    fontWeight: "800",
  },
});
