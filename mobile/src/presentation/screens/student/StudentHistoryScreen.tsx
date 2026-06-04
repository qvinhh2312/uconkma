import { useEffect } from "react";
import { dependencies } from "@app/di";
import { friendlyMessage } from "@shared/data/policyMessages";
import { AppScreen } from "@presentation/components/AppScreen";
import { AppText } from "@presentation/components/AppText";
import { Card } from "@presentation/components/Card";
import { ErrorBanner } from "@presentation/components/ErrorBanner";
import { useAsyncAction } from "@presentation/hooks/useAsyncAction";

export function StudentHistoryScreen() {
  const history = useAsyncAction(() => dependencies.students.getMyHistory());

  useEffect(() => {
    history.execute().catch(() => undefined);
  }, []);

  return (
    <AppScreen>
      <AppText variant="label">request history</AppText>
      <AppText variant="title">Lich su thao tac</AppText>
      <ErrorBanner error={history.error} />
      {history.result?.map((item) => (
        <Card key={`${item.id}-${item.requestId}`}>
          <AppText variant="subtitle">{item.action} / {item.classId}</AppText>
          <AppText variant="body">Request: {item.requestId}</AppText>
          <AppText variant="body">Decision: {item.decision}</AppText>
          <AppText variant="body">Policy: {item.failedPolicy || "-"}</AppText>
          <AppText variant="body">Ly do: {friendlyMessage(item.denyReason || item.failedPolicy)}</AppText>
          <AppText variant="body">Session: {item.sessionStatus || "-"}</AppText>
          <AppText variant="muted">{item.createdAt ?? "-"}</AppText>
        </Card>
      ))}
    </AppScreen>
  );
}
