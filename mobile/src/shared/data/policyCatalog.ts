import { Policy } from "@domain/entities/Policy";
import { ValidationSnapshot } from "@domain/entities/ValidationSnapshot";

export const policyCatalog: Policy[] = [
  ["P01_TuitionPaid_PreA0", "AUTHORIZATION", "PRE", "NONE", "REGISTER", "PERMIT", "preA0"],
  ["P13a_EmergencyMaintenance_PreC0", "CONDITION", "PRE", "NONE", "ANY", "PERMIT", "preC0"],
  ["P02_TransactionWindow_PreC0", "CONDITION", "PRE", "NONE", "ANY", "PERMIT", "preC0"],
  ["P03_ClassStatusOpen_PreA0", "AUTHORIZATION", "PRE", "NONE", "REGISTER", "PERMIT", "preA0"],
  ["P04_NotAlreadyRegistered_PreA0", "AUTHORIZATION", "PRE", "NONE", "REGISTER", "PERMIT", "preA0"],
  ["P16_DropOnlyIfRegistered_PreA0", "AUTHORIZATION", "PRE", "NONE", "DROP", "PERMIT", "preA0"],
  ["P21_DropWindow_PreC0", "CONDITION", "PRE", "NONE", "DROP", "PERMIT", "preC0"],
  ["P26_MaxDropTimes_PreA0", "AUTHORIZATION", "PRE", "NONE", "DROP", "PERMIT", "preA0"],
  ["P05_CreditLimit_PreA0", "AUTHORIZATION", "PRE", "NONE", "REGISTER", "PERMIT", "preA0"],
  ["P25_MaxRegisterAttempts_PreA0", "AUTHORIZATION", "PRE", "NONE", "REGISTER", "PERMIT", "preA0"],
  ["P06_Prerequisite_PreA0", "AUTHORIZATION", "PRE", "NONE", "REGISTER", "PERMIT", "preA0"],
  ["P17_AgreeRegistrationRule_PreB0", "OBLIGATION", "PRE", "NONE", "REGISTER", "PERMIT", "preB0"],
  ["P07_ScheduleConflict_PreA0", "AUTHORIZATION", "PRE", "NONE", "REGISTER", "PERMIT", "preA0"],
  ["P18_AdminOverrideReason_PreB0", "OBLIGATION", "PRE", "NONE", "REGISTER", "PERMIT", "preB0"],
  ["P19_RegisterAttempt_PreA1", "AUTHORIZATION", "PRE", "PRE", "REGISTER", "PERMIT", "preA1"],
  ["P13_EmergencyMaintenance_OnC0", "CONDITION", "ONGOING", "NONE", "ANY", "PERMIT", "onC0"],
  ["P08_CapacityRecheck_OnA0", "AUTHORIZATION", "ONGOING", "NONE", "REGISTER", "PERMIT", "onA0"],
  ["P20_ReserveSeat_OnA2", "AUTHORIZATION", "ONGOING", "ONGOING", "REGISTER", "PERMIT", "onA2"],
  ["P23_DropLockedClass_OnA0", "AUTHORIZATION", "ONGOING", "NONE", "DROP", "PERMIT", "onA0"],
  ["P09_ClassStatusRecheck_OnA0", "AUTHORIZATION", "ONGOING", "NONE", "REGISTER", "PERMIT", "onA0"],
  ["P27_SessionLease_OnB0", "OBLIGATION", "ONGOING", "NONE", "ANY", "PERMIT", "onB0"],
  ["P10_StudentHoldRecheck_OnA0", "AUTHORIZATION", "ONGOING", "NONE", "REGISTER", "PERMIT", "onA0"],
  ["P11_RegisterStateUpdate_PostA3", "AUTHORIZATION", "POST", "POST", "REGISTER", "PERMIT", "postA3"],
  ["P14_DropStateRevert_PostA3", "AUTHORIZATION", "POST", "POST", "DROP", "PERMIT", "postA3"],
  ["P12_AuditAndTrace_PostB3", "OBLIGATION", "POST", "POST", "ANY", "PERMIT", "postB3"],
].map(([policyId, predicate, phase, updateTiming, action, effect, variant]) => ({
  policyId,
  predicate,
  phase,
  updateTiming,
  action,
  effect,
  variant,
  status: "ACTIVE",
  source: "UCONKMA course registration policy model",
  version: "1.0",
}));

export const uconVariants = ["preA0", "preA1", "preB0", "preC0", "onA0", "onA2", "onB0", "onC0", "postA3", "postB3"];

export const validationSnapshot: ValidationSnapshot = {
  dslPolicies: 25,
  xmiPolicies: 25,
  policySets: 1,
  missingDslPoliciesInXmi: 0,
  missingRequiredAttributes: 0,
  engineTests: 65,
  dslTests: 3,
  lineCoverage: "83.21%",
  branchCoverage: "62.18%",
};
