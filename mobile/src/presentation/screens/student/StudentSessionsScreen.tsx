import { useCallback } from "react";
import { useFocusEffect } from "@react-navigation/native";
import { dependencies } from "@app/di";
import { useDecisionHistory } from "@app/providers/DecisionProvider";
import { friendlyMessage } from "@shared/data/policyMessages";
import { AppScreen } from "@presentation/components/AppScreen";
import { AppText } from "@presentation/components/AppText";
import { Card } from "@presentation/components/Card";
import { ErrorBanner } from "@presentation/components/ErrorBanner";
import { useAsyncAction } from "@presentation/hooks/useAsyncAction";

export function StudentSessionsScreen() {
  const { latestDecision } = useDecisionHistory();
  const sessions = useAsyncAction(() => dependencies.students.getMySessions());

  useFocusEffect(
    useCallback(() => {
      sessions.execute().catch(() => undefined);
    }, []),
  );

  return (
    <AppScreen>
      <AppText variant="title">Sessions</AppText>
      <ErrorBanner error={sessions.error} />
      <Card>
        <AppText variant="subtitle">Sessions dùng để làm gì?</AppText>
        <AppText variant="body">
          Mỗi request hợp lệ tạo một usage session. Nếu điều kiện ongoing thay đổi, session có thể chuyển sang REVOKED.
        </AppText>
      </Card>
      {latestDecision ? (
        <Card>
          <AppText variant="subtitle">Trace gần nhất</AppText>
          <AppText variant="body">Action: {latestDecision.action}</AppText>
          <AppText variant="body">Decision: {latestDecision.decision}</AppText>
          <AppText variant="body">Lý do: {friendlyMessage(latestDecision.denyReason || latestDecision.failedPolicy)}</AppText>
        </Card>
      ) : null}
      {sessions.result?.length ? (
        sessions.result.map((session) => (
          <Card key={session.sessionId}>
            <AppText variant="subtitle">{session.status}</AppText>
            <AppText variant="body">Lớp: {session.classId}</AppText>
            <AppText variant="body">Action: {session.action}</AppText>
            <AppText variant="body">Bắt đầu: {session.startedAt ?? "-"}</AppText>
            <AppText variant="body">Lần kiểm tra cuối: {session.lastCheckedAt ?? "-"}</AppText>
            <AppText variant="body">Lý do revoke: {session.revokeReason || "-"}</AppText>
          </Card>
        ))
      ) : (
        <Card>
          <AppText variant="muted">Chưa có usage session nào.</AppText>
        </Card>
      )}
    </AppScreen>
  );
}
