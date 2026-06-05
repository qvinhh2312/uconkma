import { useEffect } from "react";
import { StyleSheet, View } from "react-native";
import { dependencies } from "@app/di";
import { AppScreen } from "@presentation/components/AppScreen";
import { AppText } from "@presentation/components/AppText";
import { Card } from "@presentation/components/Card";
import { ErrorBanner } from "@presentation/components/ErrorBanner";
import { MetricCard } from "@presentation/components/MetricCard";
import { useAsyncAction } from "@presentation/hooks/useAsyncAction";
import { spacing } from "@core/theme/theme";

type ScheduleClass = {
  classId: string;
  courseName?: string | null;
  scheduleSlots?: string | null;
};

export function StudentDashboardScreen() {
  const action = useAsyncAction(() => dependencies.students.getMyDashboard());

  useEffect(() => {
    action.execute().catch(() => undefined);
  }, []);

  const profile = action.result?.profile;
  const revoked = action.result?.sessions.find((session) => session.status === "REVOKED");
  const schedule = weeklySchedule(action.result?.registeredClasses ?? []);

  return (
    <AppScreen>
      <AppText variant="label">student dashboard</AppText>
      <AppText variant="title">Dashboard</AppText>
      <ErrorBanner error={action.error} />
      {revoked ? (
        <Card>
          <AppText variant="subtitle">Phiên đăng kí đã bị thu hồi</AppText>
          <AppText variant="body">Lý do: {revoked.revokeReason || "Chính sách ongoing không còn thỏa mãn."}</AppText>
        </Card>
      ) : null}
      {profile ? (
        <>
          <Card>
            <AppText variant="subtitle">Tổng quan học kỳ</AppText>
            <View style={styles.metrics}>
              <MetricCard label="Tín chỉ hiện tại" value={profile.currentCredits} />
              <MetricCard label="Tín chỉ hoàn thành" value={profile.completedCredits ?? 0} />
              <MetricCard label="Công nợ" value={`${formatCurrency(profile.tuitionDebt)} VND`} />
              <MetricCard label="Lần đăng kí" value={profile.registerAttemptCount ?? 0} />
              <MetricCard label="Lần hủy" value={profile.dropCountForSemester ?? 0} />
              <MetricCard label="Lớp đã đăng kí" value={action.result?.registeredClasses.length ?? 0} />
            </View>
          </Card>
          <Card>
            <AppText variant="subtitle">Trạng thái hold</AppText>
            <AppText variant="body">{profile.holds || "Không có hold/cảnh báo."}</AppText>
          </Card>
          <Card>
            <AppText variant="subtitle">Lịch học trong tuần</AppText>
            {schedule.length ? (
              schedule.map((item) => (
                <AppText key={item} variant="body" style={styles.scheduleItem}>
                  {item}
                </AppText>
              ))
            ) : (
              <AppText variant="muted">Chưa có lớp trong lịch học cá nhân.</AppText>
            )}
          </Card>
        </>
      ) : null}
    </AppScreen>
  );
}

function weeklySchedule(classes: ScheduleClass[]) {
  return classes.flatMap((item) =>
    (item.scheduleSlots ?? "")
      .split(",")
      .map((slot) => slot.trim())
      .filter(Boolean)
      .map((slot) => `${formatSlot(slot)}: ${item.classId} - ${item.courseName ?? "Học phần"}`),
  );
}

function formatSlot(slot: string) {
  const [day, period] = slot.split("_");
  const dayLabel: Record<string, string> = {
    T2: "Thứ 2",
    T3: "Thứ 3",
    T4: "Thứ 4",
    T5: "Thứ 5",
    T6: "Thứ 6",
    T7: "Thứ 7",
    CN: "Chủ nhật",
  };
  return `${dayLabel[day] ?? day} ${period ? `(T${period})` : ""}`;
}

function formatCurrency(value: number) {
  return new Intl.NumberFormat("vi-VN").format(value);
}

const styles = StyleSheet.create({
  metrics: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: spacing.md,
    marginTop: spacing.md,
  },
  scheduleItem: {
    marginTop: spacing.sm,
  },
});
