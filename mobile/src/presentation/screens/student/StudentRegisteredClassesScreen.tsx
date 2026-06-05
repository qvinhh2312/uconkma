import { useCallback } from "react";
import { Alert } from "react-native";
import { useFocusEffect } from "@react-navigation/native";
import { dependencies } from "@app/di";
import { useDecisionHistory } from "@app/providers/DecisionProvider";
import { useSession } from "@app/providers/SessionProvider";
import { friendlyMessage } from "@shared/data/policyMessages";
import { AppButton } from "@presentation/components/AppButton";
import { AppScreen } from "@presentation/components/AppScreen";
import { AppText } from "@presentation/components/AppText";
import { Card } from "@presentation/components/Card";
import { ErrorBanner } from "@presentation/components/ErrorBanner";
import { useAsyncAction } from "@presentation/hooks/useAsyncAction";

export function StudentRegisteredClassesScreen() {
  const { session } = useSession();
  const { setLatestDecision } = useDecisionHistory();
  const registered = useAsyncAction(() => dependencies.students.getMyRegisteredClasses());
  const drop = useAsyncAction((classId: string) =>
    dependencies.registration.drop({
      requestId: `REQ-DROP-${Date.now()}`,
      studentId: session?.studentId ?? "",
      classId,
      sessionLeaseValid: true,
    }),
  );

  useFocusEffect(
    useCallback(() => {
      registered.execute().catch(() => undefined);
    }, []),
  );

  async function handleDrop(classId: string) {
    const decision = await drop.execute(classId);
    setLatestDecision(decision);
    const decisionText = String(decision.decision ?? "").toUpperCase();
    if (decisionText === "PERMIT" || decisionText === "ALLOW") {
      Alert.alert("Đã hủy đăng kí", "Lớp học phần đã được cập nhật.");
    } else {
      Alert.alert("Không thể hủy đăng kí", friendlyMessage(decision.denyReason || decision.failedPolicy));
    }
    await registered.execute().catch(() => undefined);
  }

  return (
    <AppScreen>
      <AppText variant="title">Môn đã đăng kí</AppText>
      <ErrorBanner error={registered.error ?? drop.error} />
      {registered.result?.length ? (
        registered.result.map((item) => (
          <Card key={item.classId}>
            <AppText variant="subtitle">{item.courseName ?? item.courseId ?? item.classId}</AppText>
            <AppText variant="body">Mã lớp: {item.classId}</AppText>
            <AppText variant="body">Tín chỉ: {item.credits ?? "-"}</AppText>
            <AppText variant="body">Học kỳ: {item.semester}</AppText>
            <AppText variant="body">Lịch học: {item.scheduleSlots || "-"}</AppText>
            <AppText variant="body">Trạng thái: {item.registrationStatus}</AppText>
            <AppButton tone="danger" loading={drop.loading} onPress={() => handleDrop(item.classId)}>
              Hủy đăng kí
            </AppButton>
          </Card>
        ))
      ) : (
        <Card>
          <AppText variant="muted">Bạn chưa đăng kí lớp nào trong học kỳ này.</AppText>
        </Card>
      )}
    </AppScreen>
  );
}
