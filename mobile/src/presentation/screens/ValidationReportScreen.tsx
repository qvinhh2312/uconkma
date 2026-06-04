import { AppScreen } from "@presentation/components/AppScreen";
import { AppText } from "@presentation/components/AppText";
import { Card } from "@presentation/components/Card";
import { MetricCard } from "@presentation/components/MetricCard";
import { validationSnapshot } from "@shared/data/policyCatalog";
import { StyleSheet, View } from "react-native";
import { spacing } from "@core/theme/theme";

export function ValidationReportScreen() {
  return (
    <AppScreen>
      <AppText variant="label">verification evidence</AppText>
      <AppText variant="title">Validation Report</AppText>
      <View style={styles.metrics}>
        <MetricCard label="DSL policies" value={validationSnapshot.dslPolicies} />
        <MetricCard label="XMI policies" value={validationSnapshot.xmiPolicies} />
        <MetricCard label="Engine tests" value={`${validationSnapshot.engineTests} pass`} />
        <MetricCard label="Coverage" value={validationSnapshot.lineCoverage} />
      </View>
      <Card>
        <AppText variant="subtitle">Conformance</AppText>
        <AppText variant="body">Missing DSL policies in XMI: {validationSnapshot.missingDslPoliciesInXmi}</AppText>
        <AppText variant="body">Missing required attributes: {validationSnapshot.missingRequiredAttributes}</AppText>
        <AppText variant="body">Policy sets: {validationSnapshot.policySets}</AppText>
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
    marginBottom: spacing.lg,
  },
});
