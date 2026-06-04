import { Modal, Pressable, StyleSheet, View } from "react-native";
import { useState } from "react";
import { colors, radius, spacing } from "@core/theme/theme";
import { AppText } from "./AppText";
import { AppButton } from "./AppButton";
import { friendlyMessage } from "@shared/data/policyMessages";

export function PolicyExplanationCard({
  failedPolicy,
  denyReason,
  phase,
  predicate,
}: {
  failedPolicy?: string;
  denyReason?: string;
  phase?: string;
  predicate?: string;
}) {
  const [visible, setVisible] = useState(false);
  const key = failedPolicy || denyReason;

  if (!key) {
    return null;
  }

  return (
    <>
      <AppButton tone="secondary" onPress={() => setVisible(true)}>
        Vi sao bi tu choi?
      </AppButton>
      <Modal transparent visible={visible} animationType="fade" onRequestClose={() => setVisible(false)}>
        <Pressable style={styles.backdrop} onPress={() => setVisible(false)}>
          <Pressable style={styles.modal}>
            <AppText variant="label">policy explanation</AppText>
            <AppText variant="subtitle" style={styles.title}>
              {failedPolicy ?? denyReason}
            </AppText>
            <AppText variant="body">{friendlyMessage(key)}</AppText>
            <View style={styles.meta}>
              <AppText variant="muted">Phase: {phase ?? "-"}</AppText>
              <AppText variant="muted">Predicate: {predicate ?? "-"}</AppText>
              <AppText variant="muted">Deny reason: {denyReason ?? "-"}</AppText>
            </View>
            <AppButton onPress={() => setVisible(false)}>Dong</AppButton>
          </Pressable>
        </Pressable>
      </Modal>
    </>
  );
}

const styles = StyleSheet.create({
  backdrop: {
    alignItems: "center",
    backgroundColor: "rgba(16,32,26,0.55)",
    flex: 1,
    justifyContent: "center",
    padding: spacing.lg,
  },
  modal: {
    backgroundColor: colors.paper,
    borderRadius: radius.lg,
    padding: spacing.lg,
    width: "100%",
  },
  title: {
    marginBottom: spacing.md,
    marginTop: spacing.sm,
  },
  meta: {
    gap: spacing.xs,
    marginVertical: spacing.lg,
  },
});
