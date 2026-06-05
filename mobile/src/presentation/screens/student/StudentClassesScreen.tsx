import { useEffect, useState } from "react";
import { Alert, StyleSheet, Switch, View } from "react-native";
import { dependencies } from "@app/di";
import { useDecisionHistory } from "@app/providers/DecisionProvider";
import { useSession } from "@app/providers/SessionProvider";
import { ClassSection } from "@domain/entities/ClassSection";
import { colors, spacing } from "@core/theme/theme";
import { friendlyMessage } from "@shared/data/policyMessages";
import { AppButton } from "@presentation/components/AppButton";
import { AppScreen } from "@presentation/components/AppScreen";
import { AppText } from "@presentation/components/AppText";
import { Card } from "@presentation/components/Card";
import { ErrorBanner } from "@presentation/components/ErrorBanner";
import { useAsyncAction } from "@presentation/hooks/useAsyncAction";

export function StudentClassesScreen() {
  const { session } = useSession();
  const { setLatestDecision } = useDecisionHistory();
  const classes = useAsyncAction(() => dependencies.students.listClasses());
  const register = useAsyncAction((classId: string, confirmed: boolean, leaseValid: boolean) =>
    dependencies.registration.register({
      requestId: `REQ-${Date.now()}`,
      studentId: session?.studentId ?? "",
      classId,
      confirmedRegistrationRule: confirmed,
      adminOverride: false,
      overrideReason: "",
      sessionLeaseValid: leaseValid,
    }),
  );

  useEffect(() => {
    classes.execute().catch(() => undefined);
  }, []);

  async function handleRegister(classId: string, confirmed: boolean, leaseValid: boolean) {
    if (!confirmed) {
      Alert.alert("Không thể đăng kí", "Cần tuân thủ quy định trước khi đăng kí");
      return;
    }
    const decision = await register.execute(classId, confirmed, leaseValid);
    setLatestDecision(decision);
    const decisionText = String(decision.decision ?? "").toUpperCase();
    if (decisionText === "PERMIT" || decisionText === "ALLOW") {
      Alert.alert("Đăng kí thành công", "Lớp học phần đã được ghi nhận.");
      await classes.execute().catch(() => undefined);
      return;
    }
    Alert.alert("Đăng kí thất bại", friendlyMessage(decision.denyReason || decision.failedPolicy));
  }

  return (
    <AppScreen>
      <AppText variant="title">ĐĂNG KÍ MÔN</AppText>
      <ErrorBanner error={classes.error ?? register.error} />
      {(classes.result ?? []).map((item) => (
        <ClassCard key={item.classId} item={item} loading={register.loading} onRegister={handleRegister} />
      ))}
    </AppScreen>
  );
}

function ClassCard({
  item,
  loading,
  onRegister,
}: {
  item: ClassSection;
  loading: boolean;
  onRegister(classId: string, confirmed: boolean, leaseValid: boolean): void;
}) {
  const [confirmed, setConfirmed] = useState(false);
  const [leaseValid, setLeaseValid] = useState(true);

  return (
    <Card>
      <AppText variant="subtitle">{item.courseName ?? item.courseId ?? item.classId}</AppText>
      <Info label="Mã lớp" value={item.classId} />
      <Info label="Học kỳ" value={item.semester ?? "2026_FALL"} />
      <Info label="Sĩ số" value={`${item.enrolled}+${item.reservedSeats}/${item.capacity}`} />
      <Info label="Trạng thái" value={item.status} />
      <Info label="Lịch học" value={item.scheduleSlots || "-"} />
      <Info label="Tín chỉ" value={item.credits ?? "-"} />
      <Info label="Học phí" value={`${formatCurrency(item.tuitionFee ?? 0)} VND`} />
      <Toggle label="Tôi xác nhận đã đọc và đồng ý quy chế đăng ký học phần" value={confirmed} onValueChange={setConfirmed} />
      <Toggle label="Phiên xử lý còn hợp lệ" value={leaseValid} onValueChange={setLeaseValid} />
      <View style={styles.actions}>
        <AppButton loading={loading} onPress={() => onRegister(item.classId, confirmed, leaseValid)}>
          Đăng kí
        </AppButton>
        <AppButton tone="secondary" onPress={() => showClassDetail(item)}>
          Xem chi tiết lớp
        </AppButton>
      </View>
    </Card>
  );
}

function showClassDetail(item: ClassSection) {
  Alert.alert(
    `Chi tiết ${item.classId}`,
    [
      `Môn: ${item.courseName ?? item.courseId ?? "-"}`,
      `Tín chỉ: ${item.credits ?? "-"}`,
      `Lịch học: ${item.scheduleSlots || "-"}`,
      `Sĩ số: ${item.enrolled}/${item.capacity}`,
      `Học phí: ${formatCurrency(item.tuitionFee ?? 0)} VND`,
      `Tiên quyết: ${item.prerequisites || "Không có"}`,
    ].join("\n"),
  );
}

function Info({ label, value }: { label: string; value: string | number }) {
  return (
    <View style={styles.info}>
      <AppText variant="muted">{label}</AppText>
      <AppText variant="body" style={styles.strong}>
        {value}
      </AppText>
    </View>
  );
}

function Toggle({ label, value, onValueChange }: { label: string; value: boolean; onValueChange(value: boolean): void }) {
  return (
    <View style={styles.toggle}>
      <AppText variant="body" style={styles.toggleText}>
        {label}
      </AppText>
      <Switch value={value} onValueChange={onValueChange} trackColor={{ true: colors.moss, false: "rgba(16,32,26,0.2)" }} />
    </View>
  );
}

function formatCurrency(value: number) {
  return new Intl.NumberFormat("vi-VN").format(value);
}

const styles = StyleSheet.create({
  info: {
    paddingVertical: spacing.xs,
  },
  strong: {
    fontWeight: "800",
  },
  toggle: {
    alignItems: "center",
    flexDirection: "row",
    justifyContent: "space-between",
    marginTop: spacing.sm,
  },
  toggleText: {
    flex: 1,
    paddingRight: spacing.md,
  },
  actions: {
    gap: spacing.md,
    marginTop: spacing.md,
  },
});
