import { useEffect, useState } from "react";
import { dependencies } from "@app/di";
import { AppButton } from "@presentation/components/AppButton";
import { AppScreen } from "@presentation/components/AppScreen";
import { AppText } from "@presentation/components/AppText";
import { Card } from "@presentation/components/Card";
import { ErrorBanner } from "@presentation/components/ErrorBanner";
import { Field } from "@presentation/components/Field";
import { JsonPanel } from "@presentation/components/JsonPanel";
import { useAsyncAction } from "@presentation/hooks/useAsyncAction";

export function AdminSessionsScreen() {
  const [studentId, setStudentId] = useState("");
  const [status, setStatus] = useState("");
  const sessions = useAsyncAction(() => dependencies.admin.listSessions(compact({ studentId, status })));
  const auditLogs = useAsyncAction(() => dependencies.admin.listAuditLogs(compact({ studentId })));

  useEffect(() => {
    refresh().catch(() => undefined);
  }, []);

  async function refresh() {
    await Promise.all([sessions.execute().catch(() => undefined), auditLogs.execute().catch(() => undefined)]);
  }

  return (
    <AppScreen>
      <AppText variant="label">continuity evidence</AppText>
      <AppText variant="title">Sessions / Audit</AppText>
      <ErrorBanner error={sessions.error ?? auditLogs.error} />
      <Card>
        <Field label="studentId filter" value={studentId} onChangeText={setStudentId} />
        <Field label="status ACTIVE / COMMITTED / FAILED / REVOKED" value={status} onChangeText={setStatus} />
        <AppButton loading={sessions.loading || auditLogs.loading} onPress={refresh}>
          Loc du lieu
        </AppButton>
      </Card>
      <Card>
        <AppText variant="subtitle">Usage sessions</AppText>
        {sessions.result?.length ? sessions.result.map((session) => (
          <AppText key={session.sessionId} variant="body">
            {session.status} / {session.studentId} / {session.classId} / {session.action}
          </AppText>
        )) : <AppText variant="muted">Chua co session.</AppText>}
      </Card>
      <JsonPanel title="Sessions JSON" data={sessions.result ?? []} />
      <JsonPanel title="Audit logs JSON" data={auditLogs.result ?? []} />
    </AppScreen>
  );
}

function compact(values: Record<string, string>) {
  return Object.fromEntries(Object.entries(values).filter(([, value]) => value.trim().length > 0));
}
