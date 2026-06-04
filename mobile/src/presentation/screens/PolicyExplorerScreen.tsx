import { useEffect, useMemo, useState } from "react";
import { StyleSheet, View } from "react-native";
import { dependencies } from "@app/di";
import { AppScreen } from "@presentation/components/AppScreen";
import { AppText } from "@presentation/components/AppText";
import { Card } from "@presentation/components/Card";
import { ErrorBanner } from "@presentation/components/ErrorBanner";
import { Field } from "@presentation/components/Field";
import { useAsyncAction } from "@presentation/hooks/useAsyncAction";
import { colors, radius, spacing } from "@core/theme/theme";

export function PolicyExplorerScreen() {
  const [search, setSearch] = useState("");
  const [phase, setPhase] = useState("");
  const [predicate, setPredicate] = useState("");
  const policiesAction = useAsyncAction(() => dependencies.admin.listPolicies());

  useEffect(() => {
    policiesAction.execute().catch(() => undefined);
  }, []);

  const policies = useMemo(
    () =>
      (policiesAction.result ?? []).filter(
        (policy) =>
          policy.policyId.toLowerCase().includes(search.toLowerCase()) &&
          (!phase || String(policy.phase ?? "").toLowerCase() === phase.toLowerCase()) &&
          (!predicate || String(policy.predicate ?? "").toLowerCase() === predicate.toLowerCase()),
      ),
    [phase, policiesAction.result, predicate, search],
  );

  return (
    <AppScreen>
      <AppText variant="label">policy source of truth</AppText>
      <AppText variant="title">Policy Explorer</AppText>
      <ErrorBanner error={policiesAction.error} />
      <Card>
        <Field label="search policyId" value={search} onChangeText={setSearch} />
        <Field label="phase filter PRE / ONGOING / POST" value={phase} onChangeText={setPhase} />
        <Field label="predicate filter AUTHORIZATION / OBLIGATION / CONDITION" value={predicate} onChangeText={setPredicate} />
      </Card>
      {policies.map((policy) => (
        <Card key={policy.policyId}>
          <AppText variant="subtitle">{policy.policyId}</AppText>
          <View style={styles.badges}>
            {[policy.predicate, policy.phase, policy.updateTiming, policy.targetAction, policy.uconVariant, policy.policyStatus].map((item) => (
              <View key={String(item)} style={styles.badge}>
                <AppText style={styles.badgeText}>{String(item ?? "-")}</AppText>
              </View>
            ))}
          </View>
          <AppText variant="muted" style={styles.source}>
            {policy.source} / v{policy.version}
          </AppText>
          <AppText variant="body">{policy.description}</AppText>
        </Card>
      ))}
    </AppScreen>
  );
}

const styles = StyleSheet.create({
  badges: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: spacing.sm,
    marginTop: spacing.md,
  },
  badge: {
    backgroundColor: colors.ink,
    borderRadius: radius.sm,
    paddingHorizontal: spacing.sm,
    paddingVertical: spacing.xs,
  },
  badgeText: {
    color: colors.paper,
    fontSize: 11,
    fontWeight: "800",
  },
  source: {
    marginTop: spacing.md,
  },
});
