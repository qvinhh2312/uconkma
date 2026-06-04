import { StyleSheet, View } from "react-native";
import { AppScreen } from "@presentation/components/AppScreen";
import { AppText } from "@presentation/components/AppText";
import { Card } from "@presentation/components/Card";
import { MetricCard } from "@presentation/components/MetricCard";
import { policyCatalog, uconVariants, validationSnapshot } from "@shared/data/policyCatalog";
import { colors, radius, spacing } from "@core/theme/theme";

export function DashboardScreen() {
  const authorization = policyCatalog.filter((policy) => policy.predicate === "AUTHORIZATION").length;
  const obligation = policyCatalog.filter((policy) => policy.predicate === "OBLIGATION").length;
  const condition = policyCatalog.filter((policy) => policy.predicate === "CONDITION").length;

  return (
    <AppScreen>
      <AppText variant="label">overview</AppText>
      <AppText variant="title">UCON Demo Dashboard</AppText>
      <View style={styles.metrics}>
        <MetricCard label="Policies" value={policyCatalog.length} />
        <MetricCard label="Authorization" value={authorization} />
        <MetricCard label="Obligation" value={obligation} />
        <MetricCard label="Condition" value={condition} />
      </View>
      <Card>
        <AppText variant="subtitle">Covered UCON variants</AppText>
        <View style={styles.badges}>
          {uconVariants.map((variant) => (
            <View key={variant} style={styles.badge}>
              <AppText style={styles.badgeText}>{variant}</AppText>
            </View>
          ))}
        </View>
      </Card>
      <Card>
        <AppText variant="subtitle">Verification snapshot</AppText>
        <AppText variant="body">Engine tests: {validationSnapshot.engineTests} pass</AppText>
        <AppText variant="body">DSL tests: {validationSnapshot.dslTests} pass</AppText>
        <AppText variant="body">Line coverage: {validationSnapshot.lineCoverage}</AppText>
        <AppText variant="body">Branch coverage: {validationSnapshot.branchCoverage}</AppText>
      </Card>
    </AppScreen>
  );
}

const styles = StyleSheet.create({
  metrics: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: spacing.md,
    marginTop: spacing.lg,
    marginBottom: spacing.lg,
  },
  badges: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: spacing.sm,
    marginTop: spacing.md,
  },
  badge: {
    backgroundColor: colors.ink,
    borderRadius: radius.sm,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
  },
  badgeText: {
    color: colors.paper,
    fontSize: 12,
    fontWeight: "800",
  },
});
