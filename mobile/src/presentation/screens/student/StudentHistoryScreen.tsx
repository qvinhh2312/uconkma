import { useCallback, useState } from "react";
import { Pressable, StyleSheet, View } from "react-native";
import { useFocusEffect } from "@react-navigation/native";
import { dependencies } from "@app/di";
import { colors, spacing } from "@core/theme/theme";
import { friendlyMessage } from "@shared/data/policyMessages";
import { AppScreen } from "@presentation/components/AppScreen";
import { AppText } from "@presentation/components/AppText";
import { Card } from "@presentation/components/Card";
import { ErrorBanner } from "@presentation/components/ErrorBanner";
import { useAsyncAction } from "@presentation/hooks/useAsyncAction";

type ActionFilter = "REGISTER" | "DROP";

export function StudentHistoryScreen() {
  const [filter, setFilter] = useState<ActionFilter>("REGISTER");
  const history = useAsyncAction(() => dependencies.students.getMyHistory());

  useFocusEffect(
    useCallback(() => {
      history.execute().catch(() => undefined);
    }, []),
  );

  const visibleHistory = (history.result ?? []).filter((item) => item.action === filter);

  return (
    <AppScreen>
      <View style={styles.segment}>
        <SegmentButton label="Register" active={filter === "REGISTER"} onPress={() => setFilter("REGISTER")} />
        <SegmentButton label="Drop" active={filter === "DROP"} onPress={() => setFilter("DROP")} />
      </View>
      <ErrorBanner error={history.error} />
      {visibleHistory.length ? (
        visibleHistory.map((item) => (
          <Card key={`${item.id}-${item.requestId}`}>
            <AppText variant="subtitle">{item.classId || "Không rõ lớp"}</AppText>
            <AppText variant="body">Thời gian: {formatDateTime(item.createdAt)}</AppText>
            <AppText variant="body">Trạng thái: {normalizeDecision(item.decision)}</AppText>
            <AppText variant="body">Lý do: {friendlyMessage(item.denyReason || item.failedPolicy)}</AppText>
          </Card>
        ))
      ) : (
        <Card>
          <AppText variant="muted">Chưa có thao tác {filter.toLowerCase()}.</AppText>
        </Card>
      )}
    </AppScreen>
  );
}

function SegmentButton({ label, active, onPress }: { label: string; active: boolean; onPress(): void }) {
  return (
    <Pressable onPress={onPress} style={[styles.segmentButton, active ? styles.segmentButtonActive : null]}>
      <AppText variant="body" style={active ? styles.segmentTextActive : styles.segmentText}>
        {label}
      </AppText>
    </Pressable>
  );
}

function normalizeDecision(decision: string) {
  return decision?.toUpperCase() === "PERMIT" ? "ALLOW" : decision || "-";
}

function formatDateTime(value?: string) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  const pad = (input: number) => String(input).padStart(2, "0");
  return `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())} ${pad(date.getDate())}-${date.getMonth() + 1}-${date.getFullYear()}`;
}

const styles = StyleSheet.create({
  segment: {
    backgroundColor: "rgba(16,32,26,0.08)",
    borderRadius: 999,
    flexDirection: "row",
    gap: spacing.xs,
    marginBottom: spacing.md,
    padding: spacing.xs,
  },
  segmentButton: {
    alignItems: "center",
    borderRadius: 999,
    flex: 1,
    paddingVertical: spacing.sm,
  },
  segmentButtonActive: {
    backgroundColor: colors.ink,
  },
  segmentText: {
    color: colors.inkSoft,
    fontWeight: "800",
  },
  segmentTextActive: {
    color: colors.paper,
    fontWeight: "900",
  },
});
