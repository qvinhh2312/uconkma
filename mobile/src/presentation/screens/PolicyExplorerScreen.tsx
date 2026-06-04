import { useMemo, useState } from "react";
import { StyleSheet, View } from "react-native";
import { AppScreen } from "@presentation/components/AppScreen";
import { AppText } from "@presentation/components/AppText";
import { Card } from "@presentation/components/Card";
import { Field } from "@presentation/components/Field";
import { policyCatalog } from "@shared/data/policyCatalog";
import { colors, radius, spacing } from "@core/theme/theme";

export function PolicyExplorerScreen() {
  const [search, setSearch] = useState("");
  const [phase, setPhase] = useState("");

  const policies = useMemo(
    () =>
      policyCatalog.filter(
        (policy) =>
          policy.policyId.toLowerCase().includes(search.toLowerCase()) &&
          (!phase || policy.phase.toLowerCase() === phase.toLowerCase()),
      ),
    [phase, search],
  );

  return (
    <AppScreen>
      <AppText variant="label">policy source of truth</AppText>
      <AppText variant="title">Policy Explorer</AppText>
      <Card>
        <Field label="search policyId" value={search} onChangeText={setSearch} />
        <Field label="phase filter PRE / ONGOING / POST" value={phase} onChangeText={setPhase} />
      </Card>
      {policies.map((policy) => (
        <Card key={policy.policyId}>
          <AppText variant="subtitle">{policy.policyId}</AppText>
          <View style={styles.badges}>
            {[policy.predicate, policy.phase, policy.updateTiming, policy.action, policy.variant, policy.status].map((item) => (
              <View key={item} style={styles.badge}>
                <AppText style={styles.badgeText}>{item}</AppText>
              </View>
            ))}
          </View>
          <AppText variant="muted" style={styles.source}>
            {policy.source} / v{policy.version}
          </AppText>
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
