import { useEffect } from "react";
import { StyleSheet, View } from "react-native";
import { dependencies } from "@app/di";
import { useSession } from "@app/providers/SessionProvider";
import { AppButton } from "@presentation/components/AppButton";
import { AppScreen } from "@presentation/components/AppScreen";
import { AppText } from "@presentation/components/AppText";
import { Card } from "@presentation/components/Card";
import { ErrorBanner } from "@presentation/components/ErrorBanner";
import { useAsyncAction } from "@presentation/hooks/useAsyncAction";
import { spacing } from "@core/theme/theme";

export function StudentPortalScreen() {
  const { session, logout } = useSession();
  const profile = useAsyncAction(() => dependencies.students.getMyProfile());
  const grades = useAsyncAction(() => dependencies.students.getMyGrades());
  const classes = useAsyncAction(() => dependencies.students.listClasses());

  useEffect(() => {
    if (session?.role === "STUDENT") {
      profile.execute().catch(() => undefined);
      grades.execute().catch(() => undefined);
      classes.execute().catch(() => undefined);
    }
  }, [session?.role]);

  const registeredClassIds = parseCsv(profile.result?.registeredClassIds);
  const registeredClasses = (classes.result ?? []).filter((item) => registeredClassIds.includes(item.classId));

  return (
    <AppScreen>
      <AppText variant="label">student scope</AppText>
      <AppText variant="title">My Student Portal</AppText>
      <ErrorBanner error={profile.error ?? grades.error} />
      <Card>
        <AppText variant="subtitle">{session?.displayName}</AppText>
        <AppText variant="body">Role: {session?.role}</AppText>
        <AppText variant="body">Student ID: {session?.studentId ?? "-"}</AppText>
        <AppButton tone="secondary" onPress={() => logout()} style={styles.logout}>
          Logout
        </AppButton>
      </Card>
      {profile.result ? (
        <Card>
          <AppText variant="subtitle">{profile.result.fullName}</AppText>
          <InfoRow label="Ma sinh vien" value={profile.result.studentId} />
          <InfoRow label="Email" value={profile.result.email} />
          <AppText variant="body">Major: {profile.result.major}</AppText>
          <InfoRow label="Khoa" value={profile.result.cohort} />
          <InfoRow label="Tin chi dang hoc" value={profile.result.currentCredits ?? 0} />
          <InfoRow label="Tin chi da hoan thanh" value={profile.result.completedCredits ?? 0} />
          <InfoRow label="Hoc phi da xac nhan" value={profile.result.tuitionPaid ? "Da hoan tat" : "Chua hoan tat"} />
          <InfoRow label="Du no hoc phi" value={`${formatCurrency(profile.result.tuitionDebt)} VND`} />
          <InfoRow label="Canh bao/hold" value={profile.result.holds || "Khong co"} />
          <InfoRow label="Hoc phan da qua" value={profile.result.completedCourses || "Chua co"} />
        </Card>
      ) : null}
      <Card>
        <AppText variant="subtitle">Lop hoc da dang ky</AppText>
        {registeredClasses.length ? (
          registeredClasses.map((item) => (
            <View key={item.classId} style={styles.registeredClass}>
              <AppText variant="body" style={styles.strong}>
                {item.classId} / {item.courseId}
              </AppText>
              <AppText variant="muted">Lich: {item.scheduleSlots}</AppText>
              <AppText variant="muted">Trang thai: {item.status}</AppText>
              <AppText variant="muted">Tin chi: {item.credits ?? "-"} / Hoc phi: {formatCurrency(item.tuitionFee ?? 0)} VND</AppText>
            </View>
          ))
        ) : (
          <AppText variant="muted">Sinh vien chua co lop nao trong hoc ky hien tai.</AppText>
        )}
      </Card>
      {grades.result ? (
        <Card>
          <AppText variant="subtitle">Grades</AppText>
          {grades.result.map((grade) => (
            <View key={`${grade.courseId}-${grade.semester}`} style={styles.grade}>
              <AppText variant="body">{grade.courseId} - {grade.courseName}</AppText>
              <AppText variant="muted">{grade.semester}: {grade.totalScore} / {grade.letterGrade}</AppText>
            </View>
          ))}
        </Card>
      ) : null}
    </AppScreen>
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

function parseCsv(value?: string) {
  if (!value || value === "<empty>") return [];
  return value.split(",").map((item) => item.trim()).filter(Boolean);
}

function formatCurrency(value: number) {
  return new Intl.NumberFormat("vi-VN").format(value);
}

const styles = StyleSheet.create({
  logout: {
    marginTop: spacing.md,
  },
  grade: {
    marginTop: spacing.md,
  },
  infoRow: {
    borderBottomColor: "rgba(16,32,26,0.08)",
    borderBottomWidth: 1,
    paddingVertical: spacing.sm,
  },
  strong: {
    fontWeight: "800",
  },
  registeredClass: {
    marginTop: spacing.md,
  },
});
