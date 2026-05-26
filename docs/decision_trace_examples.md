# Decision Trace Examples

Tai lieu nay ghi lai mot so mau `DecisionTrace` rut gon de phuc vu bao cao va demo.

## 1. PRE deny do thieu tien quyet

```json
{
  "requestId": "req-pre-001",
  "action": "REGISTER",
  "decision": "DENY",
  "studentId": "SV001",
  "classId": "CS102_01",
  "sessionId": null,
  "sessionStatus": null,
  "phases": [
    {
      "phase": "PRE",
      "predicate": "CONDITION",
      "decision": "ALLOW"
    },
    {
      "phase": "PRE",
      "predicate": "AUTHORIZATION",
      "decision": "DENY",
      "failedPolicy": "P06_Prerequisite_PreA0",
      "failedReason": "PREREQUISITE_NOT_MET",
      "policies": [
        {
          "policyId": "P06_Prerequisite_PreA0",
          "predicate": "AUTHORIZATION",
          "phase": "PRE",
          "updateTiming": "NONE",
          "effect": "PERMIT",
          "source": "Quy che dang ky hoc phan KMA",
          "version": "1.0",
          "uconVariant": "preA0",
          "policyStatus": "ACTIVE",
          "conditionResult": false,
          "matched": false,
          "blocked": true,
          "denyReason": "PREREQUISITE_NOT_MET"
        }
      ]
    }
  ]
}
```

Y nghia:

- request bi chan truoc khi tao `UsageSession`
- khong co update nghiep vu nao duoc ap dung
- van co `AuditLog` o POST obligation
- moi `PolicyTraceEntry` co `phase`, `updateTiming`, `conditionResult` va metadata `source`, `version`, `uconVariant`, `policyStatus` de truy vet ve quy che va policy goc

## 2. ONGOING revoke do maintenance

```json
{
  "requestId": "req-on-001",
  "action": "REGISTER",
  "decision": "DENY",
  "studentId": "SV001",
  "classId": "CS102_01",
  "sessionId": "session-001",
  "sessionStatus": "REVOKED",
  "phases": [
    {
      "phase": "PRE",
      "predicate": "CONDITION",
      "decision": "ALLOW"
    },
    {
      "phase": "PRE",
      "predicate": "AUTHORIZATION",
      "decision": "ALLOW"
    },
    {
      "phase": "ONGOING",
      "predicate": "CONDITION",
      "decision": "DENY",
      "failedPolicy": "P13_EmergencyMaintenance_OnC0",
      "failedReason": "SYSTEM_UNDER_MAINTENANCE"
    }
  ]
}
```

Y nghia:

- `UsageSession` da duoc tao `ACTIVE`
- request bi revoke giua qua trinh xu ly
- state nghiep vu duoc giu nguyen

## 3. ONGOING deny do session lease obligation

```json
{
  "requestId": "req-onb0-001",
  "action": "REGISTER",
  "decision": "DENY",
  "studentId": "SV001",
  "classId": "CS102_01",
  "sessionId": "session-002",
  "sessionStatus": "REVOKED",
  "phases": [
    {
      "phase": "ONGOING",
      "predicate": "OBLIGATION",
      "decision": "DENY",
      "failedPolicy": "P27_SessionLease_OnB0",
      "failedReason": "USAGE_SESSION_EXPIRED"
    }
  ]
}
```

Y nghia:

- project da co dai dien `onB0`
- nghia vu ongoing co the tu choi request du PRE va ONGOING authorization da pass

## 4. COMMITTED voi post-update

```json
{
  "requestId": "req-ok-001",
  "action": "REGISTER",
  "decision": "ALLOW",
  "studentId": "SV001",
  "classId": "CS102_01",
  "sessionId": "session-003",
  "sessionStatus": "COMMITTED",
  "snapshotBefore": {
    "subject.currentCredits": 0,
    "subject.tuitionDebt": 0,
    "object.enrolled": 4,
    "object.reservedSeats": 0
  },
  "snapshotAfter": {
    "subject.currentCredits": 4,
    "subject.tuitionDebt": 4000000,
    "object.enrolled": 5,
    "object.reservedSeats": 0
  },
  "phases": [
    {
      "phase": "PRE",
      "predicate": "AUTHORIZATION",
      "decision": "ALLOW",
      "updatesApplied": ["P19_RegisterAttempt_PreA1"]
    },
    {
      "phase": "ONGOING",
      "predicate": "AUTHORIZATION",
      "decision": "ALLOW",
      "updatesApplied": ["P20_ReserveSeat_OnA2"]
    },
    {
      "phase": "POST",
      "predicate": "AUTHORIZATION",
      "decision": "ALLOW",
      "updatesApplied": ["P11_RegisterStateUpdate_PostA3"]
    },
    {
      "phase": "POST",
      "predicate": "OBLIGATION",
      "decision": "ALLOW",
      "updatesApplied": ["P12_AuditAndTrace_PostB3"]
    }
  ]
}
```

Y nghia:

- request duoc commit thanh cong
- trace cho thay ro pha nao da update state, pha nao la obligation
- `snapshotBefore` va `snapshotAfter` cho thay mutable attributes thay doi nhu the nao sau enforcement

## 5. Event-driven ongoing recheck trace

Event-driven revoke khong phai request moi cua sinh vien, ma la recheck mot `UsageSession` dang `ACTIVE`
khi subject/object/environment thay doi.

```json
{
  "event": "ClassStatusChangedEvent",
  "eventPayload": {
    "classId": "CS102_01",
    "newStatus": "LOCKED"
  },
  "activeSessionsFound": 1,
  "ongoingPoliciesReevaluated": [
    {
      "policyId": "P09_ClassStatusRecheck_OnA0",
      "phase": "ONGOING",
      "predicate": "AUTHORIZATION",
      "updateTiming": "NONE",
      "uconVariant": "onA0",
      "conditionResult": false,
      "blocked": true,
      "denyReason": "CLASS_STATUS_CHANGED"
    }
  ],
  "sessionStatus": "REVOKED",
  "rollbackApplied": true
}
```

Y nghia:

- monitor nhan event thay doi state
- tim session `ACTIVE` lien quan
- evaluate lai ONGOING policies
- revoke session neu ONGOING fail
- ap dung rollback neu session da co ongoing mutation can bu tru
