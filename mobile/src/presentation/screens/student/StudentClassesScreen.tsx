import { useEffect, useState } from "react";
import { Alert, StyleSheet, Switch, View } from "react-native";
import { dependencies } from "@app/di";
import { useDecisionHistory } from "@app/providers/DecisionProvider";
import { useSession } from "@app/providers/SessionProvider";
import { ClassSection } from "@domain/entities/ClassSection";
import { colors, spacing } from "@core/theme/theme";
import { AppButton } from "@presentation/components/AppButton";
import { AppScreen } from "@presentation/components/AppScreen";
import { AppText } from "@presentation/components/AppText";
import { Card } from "@presentation/components/Card";
import { DecisionResultCard } from "@presentation/components/DecisionResultCard";
import { ErrorBanner } from "@presentation/components/ErrorBanner";
import { Field } from "@presentation/components/Field";
import { TraceTree } from "@presentation/components/TraceTree";
import { useAsyncAction } from "@presentation/hooks/useAsyncAction";

export function StudentClassesScreen() {
  const { session } = useSession();
  const { setLatestDecision } = useDecisionHistory();
  const classes = useAsyncAction(() => dependencies.students.listClasses());
  const register = useAsyncAction((classId: string, confirmed: boolean, leaseValid: boolean) =>
    dependencies.registration.register({
      requestId: `REQ-${Date.now()}`,
      studentId: session?.studentId ?? "",
      classId,
      confirmedRegistrationRule: confirmed,
      adminOverride: false,
      overrideReason: "",
      sessionLeaseValid: leaseValid,
    }),
  );
  const [search, setSearch] = useState("");

  useEffect(() => {
    classes.execute().catch(() => undefined);
  }, []);

  async function handleRegister(classId: string, confirmed: boolean, leaseValid: boolean) {
    const decision = await register.execute(classId, confirmed, leaseValid);
    setLatestDecision(decision);
  }

  const visibleClasses = (classes.result ?? []).filter((item) =>
    `${item.classId} ${item.courseId} ${item.courseName} ${item.scheduleSlots}`.toLowerCase().includes(search.toLowerCase()),
  );

  return (
    <AppScreen>
      <AppText variant="label">course registration</AppText>
      <AppText variant="title">Lop hoc phan co the dang ky</AppText>
      <ErrorBanner error={classes.error ?? register.error} />
      <Card>
        <Field label="Tim lop / ma mon / lich hoc" value={search} onChangeText={setSearch} />
      </Card>
      {visibleClasses.map((item) => (
        <ClassCard key={item.classId} item={item} loading={register.loading} onRegister={handleRegister} />
      ))}
      {register.result ? (
        <>
          <DecisionResultCard result={register.result} />
          <TraceTree trace={register.result.decisionTrace} />
        </>
      ) : null}
    </AppScreen>
  );
}

function ClassCard({
  item,
  loading,
  onRegister,
}: {
  item: ClassSection;
  loading: boolean;
  onRegister(classId: string, confirmed: boolean, leaseValid: boolean): void;
}) {
  const [confirmed, setConfirmed] = useState(false);
  const [leaseValid, setLeaseValid] = useState(true);

  return (
    <Card>
      <AppText variant="subtitle">{item.classId}</AppText>
      <Info label="courseName" value={item.courseName ?? item.courseId ?? "-"} />
      <Info label="courseId" value={item.courseId ?? "-"} />
      <Info label="semester" value={item.semester ?? "2026_FALL"} />
      <Info label="capacity" value={item.capacity} />
      <Info label="enrolled" value={item.enrolled} />
      <Info label="reservedSeats" value={item.reservedSeats} />
      <Info label="status" value={item.status} />
      <Info label="schedule" value={item.scheduleSlots} />
      <Info label="credits" value={item.credits ?? "-"} />
      <Info label="fee" value={`${formatCurrency(item.tuitionFee ?? 0)} VND`} />
      <Toggle label="Toi xac nhan da doc va dong y quy che dang ky hoc phan" value={confirmed} onValueChange={setConfirmed} />
      <Toggle label="Session lease valid" value={leaseValid} onValueChange={setLeaseValid} />
      <View style={styles.actions}>
        <AppButton loading={loading} onPress={() => onRegister(item.classId, confirmed, leaseValid)}>
          Dang ky
        </AppButton>
        <AppButton tone="secondary" onPress={() => showClassDetail(item)}>
          Xem chi tiet lop
        </AppButton>
        <AppButton tone="secondary" onPress={() => showRelatedPolicies(item)}>
          Xem policy lien quan
        </AppButton>
      </View>
    </Card>
  );
}

function showClassDetail(item: ClassSection) {
  Alert.alert(
    `Chi tiet ${item.classId}`,
    [
      `Mon: ${item.courseName ?? item.courseId ?? "-"}`,
      `Hoc ky: ${item.semester ?? "2026_FALL"}`,
      `Tin chi: ${item.credits ?? "-"}`,
      `Hoc phi: ${formatCurrency(item.tuitionFee ?? 0)} VND`,
      `Lich hoc: ${item.scheduleSlots || "-"}`,
      `Si so: ${item.enrolled}/${item.capacity}`,
      `Giu cho tam thoi: ${item.reservedSeats}`,
      `Trang thai: ${item.status}`,
      `Tien quyet: ${item.prerequisites || "Khong co"}`,
    ].join("\n"),
  );
}

function showRelatedPolicies(item: ClassSection) {
  Alert.alert(
    "Policy lien quan",
    [
      "P17_AgreeRegistrationRule_PreB0: xac nhan quy che dang ky.",
      "P01_TuitionPaid_PreA0: dieu kien hoc phi.",
      "P06_Prerequisite_PreA0: dieu kien tien quyet.",
      "P08_CapacityRecheck_OnA0: kiem tra lai si so.",
      "P20_ReserveSeat_OnA2: cap nhat giu cho trong ongoing.",
      `Lop dang xem: ${item.classId}`,
    ].join("\n\n"),
  );
}

function Info({ label, value }: { label: string; value: string | number }) {
  return (
    <View style={styles.info}>
      <AppText variant="muted">{label}</AppText>
      <AppText variant="body" style={styles.strong}>{value}</AppText>
    </View>
  );
}

function Toggle({ label, value, onValueChange }: { label: string; value: boolean; onValueChange(value: boolean): void }) {
  return (
    <View style={styles.toggle}>
      <AppText variant="body" style={styles.toggleText}>{label}</AppText>
      <Switch value={value} onValueChange={onValueChange} trackColor={{ true: colors.moss, false: "rgba(16,32,26,0.2)" }} />
    </View>
  );
}

function formatCurrency(value: number) {
  return new Intl.NumberFormat("vi-VN").format(value);
}

const styles = StyleSheet.create({
  info: {
    paddingVertical: spacing.xs,
  },
  strong: {
    fontWeight: "800",
  },
  toggle: {
    alignItems: "center",
    flexDirection: "row",
    justifyContent: "space-between",
    marginTop: spacing.sm,
  },
  toggleText: {
    flex: 1,
    paddingRight: spacing.md,
  },
  actions: {
    gap: spacing.md,
    marginTop: spacing.md,
  },
});
