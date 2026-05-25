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
      "failedReason": "PREREQUISITE_NOT_MET"
    }
  ]
}
```

Y nghia:

- request bi chan truoc khi tao `UsageSession`
- khong co update nghiep vu nao duoc ap dung
- van co `AuditLog` o POST obligation

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
