import { useEffect } from "react";
import { dependencies } from "@app/di";
import { AppScreen } from "@presentation/components/AppScreen";
import { AppText } from "@presentation/components/AppText";
import { Card } from "@presentation/components/Card";
import { ErrorBanner } from "@presentation/components/ErrorBanner";
import { useAsyncAction } from "@presentation/hooks/useAsyncAction";

export function AdminStudentsScreen() {
  const action = useAsyncAction(() => dependencies.students.listStudents());

  useEffect(() => {
    action.execute().catch(() => undefined);
  }, []);

  return (
    <AppScreen>
      <AppText variant="label">admin scope</AppText>
      <AppText variant="title">Students</AppText>
      <ErrorBanner error={action.error} />
      {action.result?.map((student) => (
        <Card key={student.studentId}>
          <AppText variant="subtitle">{student.studentId} - {student.fullName}</AppText>
          <AppText variant="body">Major: {student.major}</AppText>
          <AppText variant="body">Credits: {student.currentCredits}</AppText>
          <AppText variant="body">Tuition paid: {String(student.tuitionPaid)}</AppText>
          <AppText variant="body">Holds: {student.holds || "-"}</AppText>
        </Card>
      ))}
    </AppScreen>
  );
}
