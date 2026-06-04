import { useEffect, useState } from "react";
import { StyleSheet, View } from "react-native";
import { dependencies } from "@app/di";
import { AppButton } from "@presentation/components/AppButton";
import { AppScreen } from "@presentation/components/AppScreen";
import { AppText } from "@presentation/components/AppText";
import { Card } from "@presentation/components/Card";
import { ErrorBanner } from "@presentation/components/ErrorBanner";
import { Field } from "@presentation/components/Field";
import { JsonPanel } from "@presentation/components/JsonPanel";
import { useAsyncAction } from "@presentation/hooks/useAsyncAction";
import { spacing } from "@core/theme/theme";

export function AdminDataScreen() {
  const students = useAsyncAction(() => dependencies.admin.listStudents());
  const classes = useAsyncAction(() => dependencies.admin.listClasses());
  const studentState = useAsyncAction((studentId: string, tuitionDebt: number, holds: string) =>
    dependencies.admin.updateStudentState(studentId, {
      tuitionDebt,
      holds: holds.split(",").map((item) => item.trim()).filter(Boolean),
    }),
  );
  const classState = useAsyncAction((classId: string, capacity: number, enrolled: number, reservedSeats: number, status: string) =>
    dependencies.admin.updateClassState(classId, { capacity, enrolled, reservedSeats, status }),
  );
  const [studentId, setStudentId] = useState("SV001");
  const [tuitionDebt, setTuitionDebt] = useState("0");
  const [holds, setHolds] = useState("");
  const [classId, setClassId] = useState("CS102_01");
  const [capacity, setCapacity] = useState("5");
  const [enrolled, setEnrolled] = useState("4");
  const [reservedSeats, setReservedSeats] = useState("0");
  const [status, setStatus] = useState("OPEN");

  useEffect(() => {
    refresh().catch(() => undefined);
  }, []);

  async function refresh() {
    await Promise.all([students.execute().catch(() => undefined), classes.execute().catch(() => undefined)]);
  }

  async function updateStudent() {
    await studentState.execute(studentId, Number(tuitionDebt), holds);
    await students.execute();
  }

  async function updateClass() {
    await classState.execute(classId, Number(capacity), Number(enrolled), Number(reservedSeats), status);
    await classes.execute();
  }

  return (
    <AppScreen>
      <AppText variant="label">admin data</AppText>
      <AppText variant="title">Student / Class Demo State</AppText>
      <ErrorBanner error={students.error ?? classes.error ?? studentState.error ?? classState.error} />
      <Card>
        <AppText variant="subtitle">Sua state sinh vien demo</AppText>
        <Field label="studentId" value={studentId} onChangeText={setStudentId} />
        <Field label="tuitionDebt" value={tuitionDebt} onChangeText={setTuitionDebt} keyboardType="numeric" />
        <Field label="holds, comma separated" value={holds} onChangeText={setHolds} />
        <AppButton loading={studentState.loading} onPress={updateStudent}>
          Cap nhat sinh vien
        </AppButton>
      </Card>
      <Card>
        <AppText variant="subtitle">Sua state lop demo</AppText>
        <Field label="classId" value={classId} onChangeText={setClassId} />
        <Field label="capacity" value={capacity} onChangeText={setCapacity} keyboardType="numeric" />
        <Field label="enrolled" value={enrolled} onChangeText={setEnrolled} keyboardType="numeric" />
        <Field label="reservedSeats" value={reservedSeats} onChangeText={setReservedSeats} keyboardType="numeric" />
        <Field label="status OPEN / LOCKED / CLOSED / CANCELLED" value={status} onChangeText={setStatus} />
        <AppButton loading={classState.loading} onPress={updateClass}>
          Cap nhat lop
        </AppButton>
      </Card>
      <View style={styles.columns}>
        <Card>
          <AppText variant="subtitle">Sinh vien</AppText>
          {students.result?.map((student) => (
            <View key={student.studentId} style={styles.row}>
              <AppText variant="body" style={styles.strong}>{student.studentId} - {student.fullName}</AppText>
              <AppText variant="muted">Credits: {student.currentCredits} / Debt: {formatCurrency(Number(student.tuitionDebt ?? 0))} VND</AppText>
              <AppText variant="muted">Holds: {Array.isArray(student.holds) ? student.holds.join(", ") : student.holds || "none"}</AppText>
            </View>
          ))}
        </Card>
        <Card>
          <AppText variant="subtitle">Lop hoc</AppText>
          {classes.result?.map((item) => (
            <View key={item.classId} style={styles.row}>
              <AppText variant="body" style={styles.strong}>{item.classId} - {item.courseName}</AppText>
              <AppText variant="muted">Status: {item.status} / Seats: {item.enrolled}+{item.reservedSeats}/{item.capacity}</AppText>
              <AppText variant="muted">Available: {item.availableSeats}</AppText>
            </View>
          ))}
        </Card>
      </View>
      {studentState.result ? <JsonPanel title="Student update response" data={studentState.result} /> : null}
      {classState.result ? <JsonPanel title="Class update response" data={classState.result} /> : null}
    </AppScreen>
  );
}

function formatCurrency(value: number) {
  return new Intl.NumberFormat("vi-VN").format(value);
}

const styles = StyleSheet.create({
  columns: {
    gap: spacing.md,
  },
  row: {
    borderBottomColor: "rgba(16,32,26,0.08)",
    borderBottomWidth: 1,
    paddingVertical: spacing.sm,
  },
  strong: {
    fontWeight: "800",
  },
});
