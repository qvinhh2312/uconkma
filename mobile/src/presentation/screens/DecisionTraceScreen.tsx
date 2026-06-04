import { AppScreen } from "@presentation/components/AppScreen";
import { AppText } from "@presentation/components/AppText";
import { Card } from "@presentation/components/Card";
import { SnapshotDiff } from "@presentation/components/SnapshotDiff";
import { TraceTree } from "@presentation/components/TraceTree";
import { useDecisionHistory } from "@app/providers/DecisionProvider";

export function DecisionTraceScreen() {
  const { latestDecision } = useDecisionHistory();

  return (
    <AppScreen>
      <AppText variant="label">explainability</AppText>
      <AppText variant="title">Decision Trace</AppText>
      {latestDecision ? (
        <>
          <Card>
            <AppText variant="subtitle">{latestDecision.decision}</AppText>
            <AppText variant="body">Failed policy: {latestDecision.failedPolicy ?? "-"}</AppText>
            <AppText variant="body">Reason: {latestDecision.denyReason ?? "-"}</AppText>
          </Card>
          <TraceTree trace={latestDecision.decisionTrace} />
          <Card>
            <AppText variant="subtitle">Mutable snapshot</AppText>
            <SnapshotDiff response={latestDecision} />
          </Card>
        </>
      ) : (
        <Card>
          <AppText variant="subtitle">No decision yet</AppText>
          <AppText variant="muted">Run REGISTER or DROP first to inspect PRE / ONGOING / POST trace here.</AppText>
        </Card>
      )}
    </AppScreen>
  );
}
