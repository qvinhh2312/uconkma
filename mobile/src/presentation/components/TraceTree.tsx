import { StyleSheet, View } from "react-native";
import { DecisionTrace, DecisionTracePhase } from "@domain/entities/Decision";
import { colors, radius, spacing } from "@core/theme/theme";
import { AppText } from "./AppText";

export function TraceTree({ trace }: { trace?: DecisionTrace | null }) {
  const phases = normalizePhases(trace);

  if (!phases.length) {
    return (
      <View style={styles.empty}>
        <AppText variant="muted">No phase trace available.</AppText>
      </View>
    );
  }

  return (
    <View style={styles.wrapper}>
      {phases.map((phase, index) => (
        <View key={`${phase.phase}-${phase.predicate}-${index}`} style={styles.phase}>
          <AppText variant="subtitle">
            {phase.phase ?? "PHASE"} / {phase.predicate ?? "PREDICATE"}
          </AppText>
          {(phase.policies ?? []).map((policy) => (
            <View key={`${policy.policyId}-${policy.result}`} style={styles.policy}>
              <AppText style={styles.policyId}>{policy.policyId ?? "policy"}</AppText>
              <AppText variant="muted">
                {policy.result ?? String(policy.conditionResult ?? "")} {policy.denyReason ? `- ${policy.denyReason}` : ""}
              </AppText>
            </View>
          ))}
        </View>
      ))}
    </View>
  );
}

function normalizePhases(trace?: DecisionTrace | null): DecisionTracePhase[] {
  if (!trace) return [];
  if (Array.isArray(trace.phases)) return trace.phases;
  if (Array.isArray(trace.phaseTraces)) return trace.phaseTraces;
  return [];
}

const styles = StyleSheet.create({
  wrapper: {
    gap: spacing.md,
  },
  phase: {
    backgroundColor: colors.paper,
    borderRadius: radius.lg,
    padding: spacing.lg,
  },
  policy: {
    backgroundColor: colors.white,
    borderRadius: radius.md,
    marginTop: spacing.sm,
    padding: spacing.md,
  },
  policyId: {
    fontWeight: "800",
  },
  empty: {
    backgroundColor: colors.paper,
    borderRadius: radius.lg,
    padding: spacing.lg,
  },
});
