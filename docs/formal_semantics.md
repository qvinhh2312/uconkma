# Formal Semantics of UCONKMA DSL

Tai lieu nay dinh nghia ngan gon ngu nghia thuc thi cua DSL. Muc tieu la lam ro cach policy
duoc evaluate va cach state thay doi trong runtime, khong chi mo ta bang code.

## 1. Core Sets

| Ky hieu | Y nghia |
| --- | --- |
| `S` | Tap subject, trong do do an hien tai dung `Student` |
| `O` | Tap object, trong do do an hien tai dung `ClassSection` / `Course` |
| `R` | Tap right/action: `REGISTER`, `DROP`, `ANY` |
| `E` | Tap environment attributes: registration phase, time window, maintenance flag, semester |
| `P` | Tap policies duoc load tu DSL/XMI |
| `D` | Tap decision: `{PERMIT, DENY}` |

## 2. Runtime Objects

Policy:

```text
p = <policyId, predicate, phase, updateTiming, targetAction, effect,
     condition, updates, rollbackUpdates, metadata>
```

Trong do:

```text
predicate    in {AUTHORIZATION, OBLIGATION, CONDITION}
phase        in {PRE, ONGOING, POST}
updateTiming in {NONE, PRE, ONGOING, POST}
effect       in {PERMIT, DENY}
metadata     = <source, version, policyStatus, uconVariant>
```

Request:

```text
r = <subject, object, right, requestAttrs>
```

State:

```text
sigma = <subjectAttrs, objectAttrs, environmentAttrs, requestAttrs, usageSession, trace>
```

Decision:

```text
decision(r, sigma, P) in {PERMIT, DENY}
```

## 3. Expression Evaluation

Expression evaluation duoc ky hieu:

```text
[[expr]](sigma) -> value
```

Vi du:

```text
[[subject.tuitionPaid == true]](sigma) = true
[[object.enrolled < object.capacity]](sigma) = true
[[request.sessionLeaseValid == true]](sigma) = false
```

Function call trong DSL chi hop le neu nam trong whitelist cua validator, vi du:

```text
checkExistsRegistration(studentId, classId, semester)
isEmpty(subject.holds)
```

## 4. Policy Applicability

Policy `p` applicable voi request `r` neu:

```text
p.targetAction = r.right or p.targetAction = ANY
p.policyStatus = ACTIVE
p.phase = currentPhase
p.predicate = currentPredicate
```

Neu policy khong applicable thi runtime khong dung policy do de ra decision cho phase hien tai.

## 5. Combining Semantics

Policy set hien tai dung `DENY_OVERRIDES`.

Voi mot phase/predicate:

```text
Eval(P_i, r, sigma) = PASS neu [[p_i.condition]](sigma) = true
Eval(P_i, r, sigma) = FAIL neu [[p_i.condition]](sigma) = false
```

Voi permit policy, `FAIL` nghia la request bi deny boi policy do:

```text
exists p_i: Eval(P_i, r, sigma) = FAIL
=> Decision = DENY
```

Neu tat ca applicable policies pass:

```text
forall p_i: Eval(P_i, r, sigma) = PASS
=> PhaseDecision = PERMIT
```

Trace ghi lai moi policy result:

```text
PolicyTraceEntry = <policyId, predicate, phase, updateTiming, source, version,
                    uconVariant, policyStatus, conditionResult, blocked>
```

## 6. PRE Semantics

PRE la giai doan truoc khi usage session / business commit duoc chap nhan.

```text
PRE(r, sigma):
  evaluate CONDITION policies with phase = PRE
  evaluate AUTHORIZATION policies with phase = PRE
  evaluate OBLIGATION policies with phase = PRE
  if any phase decision = DENY:
      write DecisionTrace
      return DENY
  apply preUpdates
  return PERMIT
```

Vi du UCON variants:

| Variant | Policy example |
| --- | --- |
| `preA0` | tuition paid, class open, prerequisite, schedule conflict |
| `preA1` | register attempt count update |
| `preB0` | confirmed registration rule, admin override reason |
| `preC0` | transaction window, maintenance off |

## 7. ONGOING Semantics

ONGOING dai dien cho continuity cua UCON. Trong ban hien tai, ongoing duoc thuc thi o 2 muc:

1. transaction-level re-check trong workflow register/drop
2. event-driven revoke cho ACTIVE usage sessions

```text
ONGOING(r, sigma):
  create/load UsageSession(status = ACTIVE)
  evaluate CONDITION policies with phase = ONGOING
  evaluate AUTHORIZATION policies with phase = ONGOING
  evaluate OBLIGATION policies with phase = ONGOING
  if any phase decision = DENY:
      apply rollbackUpdates for ongoing mutations
      mark UsageSession = REVOKED or FAILED
      write DecisionTrace
      return DENY
  apply ongoingUpdates
  return PERMIT
```

Vi du UCON variants:

| Variant | Policy example |
| --- | --- |
| `onA0` | capacity recheck, class status recheck, student hold recheck |
| `onA2` | reserve seat with rollback |
| `onB0` | session lease valid |
| `onC0` | emergency maintenance not active |

## 8. POST Semantics

POST chay sau khi PRE va ONGOING da permit.

```text
POST(r, sigma):
  execute business commit
  apply postUpdates
  execute postObligations
  run DomainInvariantChecker
  mark UsageSession = COMMITTED
  write DecisionTrace
  return PERMIT
```

Vi du UCON variants:

| Variant | Policy example |
| --- | --- |
| `postA3` | create/delete registration, enrolled/currentCredits/tuitionDebt mutation |
| `postB3` | audit log obligation |

## 9. Event-Driven Recheck Semantics

Voi mot event `ev`:

```text
ON_EVENT(ev, sigma):
  sessions = activeSessionsAffectedBy(ev)
  for each session in sessions:
      reconstruct context from session
      evaluate ONGOING policies
      if DENY:
          apply rollback if needed
          mark session = REVOKED
          record recheck result
```

Events hien tai:

| Event | Recheck scope |
| --- | --- |
| `ClassStatusChangedEvent(classId, newStatus)` | ACTIVE sessions linked to `classId` |
| `MaintenanceEnabledEvent(active)` | all ACTIVE sessions when maintenance is enabled |
| `StudentHoldAddedEvent(studentId, holdCode)` | ACTIVE sessions linked to `studentId` |

## 10. Safety Properties

| Ma | Property | Evidence |
| --- | --- | --- |
| `S1` | `object.enrolled <= object.capacity` | `DomainInvariantChecker`, race tests |
| `S2` | `object.reservedSeats >= 0` | rollback tests, invariant checker |
| `S3` | `subject.tuitionDebt >= 0` | invariant checker |
| `S4` | no duplicate registration for `<studentId, classId, semester>` | database unique constraint + tests |
| `S5` | every mutable request has decision trace | `DecisionTrace`, `updatesApplied`, `rollbackApplied`, snapshots |

## 11. Liveness Properties

| Ma | Property | Scope hien tai |
| --- | --- | --- |
| `L1` | eligible register request eventually returns `PERMIT` | satisfied in synchronous REST workflow |
| `L2` | valid drop request eventually restores credits/seats | satisfied in synchronous REST workflow |
| `L3` | active session affected by monitored event is eventually rechecked | satisfied in local event-driven monitor |

## 12. OCL-like Well-Formedness Rules

Nhung rule nay duoc hien thuc bang `PolicyModelSemanticValidator` / schema validator thay vi mot OCL engine rieng.

```ocl
context Policy
  inv ConditionHasNoUpdate:
    self.predicate = CONDITION implies
      self.preUpdates->isEmpty() and
      self.ongoingUpdates->isEmpty() and
      self.postUpdates->isEmpty()

context UpdateStatement
  inv NoEnvironmentOrRequestMutation:
    self.target.entity <> ENVIRONMENT and self.target.entity <> REQUEST

context Policy
  inv OngoingUpdateNeedsRollback:
    self.updateTiming = ONGOING implies self.rollbackUpdates->notEmpty()

context Policy
  inv ExplicitRequiredMetadata:
    self.source <> '' and
    self.version <> '' and
    self.uconVariant <> '' and
    self.policyStatus <> null

context Policy
  inv UpdateTimingMatchesBlocks:
    self.updateTiming = NONE implies all update blocks are empty
```

## 13. Known Scope

Ban hien tai la formal execution semantics cho do an UCONKMA, khong phai full formal proof:

- chua bao phu day du 24 bien the UCONABC
- chua co TLA+/SMT/OCL solver thuc thi
- event-driven monitoring chua phai distributed scheduler / event bus dai han
- analyzer hien tai la heuristic analyzer, khong phai formal verifier
