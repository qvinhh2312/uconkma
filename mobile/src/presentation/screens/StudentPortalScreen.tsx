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

  useEffect(() => {
    if (session?.role === "STUDENT") {
      profile.execute().catch(() => undefined);
      grades.execute().catch(() => undefined);
    }
  }, [session?.role]);

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
          <AppText variant="body">Major: {profile.result.major}</AppText>
          <AppText variant="body">Current credits: {profile.result.currentCredits}</AppText>
          <AppText variant="body">Tuition paid: {String(profile.result.tuitionPaid)}</AppText>
          <AppText variant="body">Holds: {profile.result.holds || "-"}</AppText>
        </Card>
      ) : null}
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

const styles = StyleSheet.create({
  logout: {
    marginTop: spacing.md,
  },
  grade: {
    marginTop: spacing.md,
  },
});
