import { useEffect } from "react";
import { dependencies } from "@app/di";
import { useDecisionHistory } from "@app/providers/DecisionProvider";
import { useSession } from "@app/providers/SessionProvider";
import { AppButton } from "@presentation/components/AppButton";
import { AppScreen } from "@presentation/components/AppScreen";
import { AppText } from "@presentation/components/AppText";
import { Card } from "@presentation/components/Card";
import { DecisionResultCard } from "@presentation/components/DecisionResultCard";
import { ErrorBanner } from "@presentation/components/ErrorBanner";
import { TraceTree } from "@presentation/components/TraceTree";
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

  useEffect(() => {
    registered.execute().catch(() => undefined);
  }, []);

  async function handleDrop(classId: string) {
    const decision = await drop.execute(classId);
    setLatestDecision(decision);
    await registered.execute().catch(() => undefined);
  }

  return (
    <AppScreen>
      <AppText variant="label">registered classes</AppText>
      <AppText variant="title">Lop da dang ky</AppText>
      <ErrorBanner error={registered.error ?? drop.error} />
      {registered.result?.length ? registered.result.map((item) => (
        <Card key={item.classId}>
          <AppText variant="subtitle">{item.classId} / {item.courseName ?? item.courseId}</AppText>
          <AppText variant="body">Tin chi: {item.credits ?? "-"}</AppText>
          <AppText variant="body">Hoc ky: {item.semester}</AppText>
          <AppText variant="body">Trang thai: {item.registrationStatus}</AppText>
          <AppText variant="body">Registered at: {item.registeredAt ?? "-"}</AppText>
          <AppButton tone="danger" loading={drop.loading} onPress={() => handleDrop(item.classId)}>
            Huy dang ky
          </AppButton>
        </Card>
      )) : (
        <Card>
          <AppText variant="muted">Chua co lop da dang ky.</AppText>
        </Card>
      )}
      {drop.result ? (
        <>
          <DecisionResultCard result={drop.result} />
          <TraceTree trace={drop.result.decisionTrace} />
        </>
      ) : null}
    </AppScreen>
  );
}
