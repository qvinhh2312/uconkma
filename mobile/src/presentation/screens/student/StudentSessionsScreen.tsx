import { useEffect } from "react";
import { dependencies } from "@app/di";
import { AppScreen } from "@presentation/components/AppScreen";
import { AppText } from "@presentation/components/AppText";
import { Card } from "@presentation/components/Card";
import { ErrorBanner } from "@presentation/components/ErrorBanner";
import { useAsyncAction } from "@presentation/hooks/useAsyncAction";

export function StudentSessionsScreen() {
  const sessions = useAsyncAction(() => dependencies.students.getMySessions());

  useEffect(() => {
    sessions.execute().catch(() => undefined);
  }, []);

  return (
    <AppScreen>
      <AppText variant="label">usage session</AppText>
      <AppText variant="title">Trang thai phien</AppText>
      <ErrorBanner error={sessions.error} />
      {sessions.result?.length ? sessions.result.map((session) => (
        <Card key={session.sessionId}>
          <AppText variant="subtitle">{session.status}</AppText>
          <AppText variant="body">Session ID: {session.sessionId}</AppText>
          <AppText variant="body">Action: {session.action}</AppText>
          <AppText variant="body">Class: {session.classId}</AppText>
          <AppText variant="body">Started: {session.startedAt ?? "-"}</AppText>
          <AppText variant="body">Last checked: {session.lastCheckedAt ?? "-"}</AppText>
          <AppText variant="body">Revoke reason: {session.revokeReason || "-"}</AppText>
        </Card>
      )) : (
        <Card>
          <AppText variant="muted">Chua co usage session nao.</AppText>
        </Card>
      )}
    </AppScreen>
  );
}
