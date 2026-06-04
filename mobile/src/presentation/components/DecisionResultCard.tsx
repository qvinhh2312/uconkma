import { ApiDecisionResponse } from "@domain/entities/Decision";
import { spacing } from "@core/theme/theme";
import { StyleSheet, View } from "react-native";
import { friendlyMessage } from "@shared/data/policyMessages";
import { AppText } from "./AppText";
import { Card } from "./Card";
import { DecisionBadge } from "./DecisionBadge";
import { PolicyExplanationCard } from "./PolicyExplanationCard";

export function DecisionResultCard({ result }: { result: ApiDecisionResponse }) {
  const success = result.decision === "ALLOW" || result.decision === "PERMIT";
  return (
    <Card>
      <DecisionBadge decision={result.decision} />
      <AppText variant="subtitle" style={styles.title}>
        {success ? "Thao tac thanh cong" : "Thao tac that bai"}
      </AppText>
      <Info label="Request ID" value={result.requestId} />
      <Info label="Action" value={result.action} />
      <Info label="Phase" value={result.phase ?? "-"} />
      <Info label="Predicate" value={result.predicate ?? "-"} />
      <Info label="Failed policy" value={result.failedPolicy ?? "-"} />
      <Info label="Deny reason" value={result.denyReason ?? "-"} />
      <Info label="Session" value={result.sessionStatus ?? "-"} />
      {!success ? (
        <AppText variant="body" style={styles.message}>
          {friendlyMessage(result.failedPolicy || result.denyReason)}
        </AppText>
      ) : null}
      <PolicyExplanationCard
        failedPolicy={result.failedPolicy}
        denyReason={result.denyReason}
        phase={result.phase}
        predicate={result.predicate}
      />
    </Card>
  );
}

function Info({ label, value }: { label: string; value: string }) {
  return (
    <View style={styles.info}>
      <AppText variant="muted">{label}</AppText>
      <AppText variant="body" style={styles.strong}>
        {value}
      </AppText>
    </View>
  );
}

const styles = StyleSheet.create({
  title: {
    marginBottom: spacing.sm,
    marginTop: spacing.md,
  },
  info: {
    paddingVertical: spacing.xs,
  },
  strong: {
    fontWeight: "800",
  },
  message: {
    marginVertical: spacing.md,
  },
});
