import { useEffect, useState } from "react";
import { Alert, StyleSheet, View } from "react-native";
import { dependencies } from "@app/di";
import { useSession } from "@app/providers/SessionProvider";
import { AppButton } from "@presentation/components/AppButton";
import { AppScreen } from "@presentation/components/AppScreen";
import { AppText } from "@presentation/components/AppText";
import { Card } from "@presentation/components/Card";
import { ErrorBanner } from "@presentation/components/ErrorBanner";
import { Field } from "@presentation/components/Field";
import { useAsyncAction } from "@presentation/hooks/useAsyncAction";
import { spacing } from "@core/theme/theme";

export function StudentPortalScreen() {
  const { logout } = useSession();
  const profile = useAsyncAction(() => dependencies.students.getMyProfile());
  const updateProfile = useAsyncAction((email: string, dateOfBirth: string, gender: string) =>
    dependencies.students.updateMyProfile({ email, dateOfBirth, gender }),
  );
  const [email, setEmail] = useState("");
  const [dateOfBirth, setDateOfBirth] = useState("");
  const [gender, setGender] = useState("");

  useEffect(() => {
    profile.execute().catch(() => undefined);
  }, []);

  useEffect(() => {
    if (profile.result) {
      setEmail(profile.result.email ?? "");
      setDateOfBirth(profile.result.dateOfBirth ?? "");
      setGender(profile.result.gender ?? "");
    }
  }, [profile.result]);

  async function saveProfile() {
    const updated = await updateProfile.execute(email, dateOfBirth, gender);
    Alert.alert("Đã cập nhật", "Thông tin cá nhân đã được lưu.");
    setEmail(updated.email ?? "");
    setDateOfBirth(updated.dateOfBirth ?? "");
    setGender(updated.gender ?? "");
    await profile.execute().catch(() => undefined);
  }

  const data = updateProfile.result ?? profile.result;

  return (
    <AppScreen>
      <AppText variant="title">Profile</AppText>
      <ErrorBanner error={profile.error ?? updateProfile.error} />
      {data ? (
        <>
          <Card>
            <AppText variant="subtitle">{data.fullName}</AppText>
            <InfoRow label="Mã sinh viên" value={data.studentId} />
            <InfoRow label="Ngành" value={data.major} />
            <InfoRow label="Khóa" value={data.cohort} />
            <InfoRow label="Tín chỉ hiện tại" value={data.currentCredits} />
            <InfoRow label="Tín chỉ hoàn thành" value={data.completedCredits ?? 0} />
            <InfoRow label="Công nợ" value={`${formatCurrency(data.tuitionDebt)} VND`} />
            <InfoRow label="Hold" value={data.holds || "Không có"} />
            <InfoRow label="Môn đã hoàn thành" value={data.completedCourses || "Chưa có"} />
          </Card>
          <Card>
            <AppText variant="subtitle">Thông tin có thể chỉnh sửa</AppText>
            <Field label="Email" value={email} onChangeText={setEmail} keyboardType="email-address" />
            <Field label="Ngày sinh (YYYY-MM-DD)" value={dateOfBirth} onChangeText={setDateOfBirth} />
            <Field label="Giới tính" value={gender} onChangeText={setGender} />
            <AppButton loading={updateProfile.loading} onPress={saveProfile}>
              Lưu thay đổi
            </AppButton>
          </Card>
          <AppButton tone="secondary" onPress={logout} style={styles.logout}>
            Logout
          </AppButton>
        </>
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

function formatCurrency(value: number) {
  return new Intl.NumberFormat("vi-VN").format(value);
}

const styles = StyleSheet.create({
  infoRow: {
    borderBottomColor: "rgba(16,32,26,0.08)",
    borderBottomWidth: 1,
    paddingVertical: spacing.sm,
  },
  strong: {
    fontWeight: "800",
  },
  logout: {
    marginTop: spacing.md,
  },
});
