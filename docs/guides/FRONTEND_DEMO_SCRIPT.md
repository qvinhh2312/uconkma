# Frontend Demo Script

Tai lieu nay dung de chay demo mini app UCONKMA trong 5-7 phut khi bao ve.

## 1. Run Backend

```powershell
cd engine
mvn spring-boot:run
```

Backend API:

```text
http://localhost:8080/api
```

Smoke test:

```text
GET http://localhost:8080/api/demo/state?studentId=SV001&classId=CS102_01
```

## 2. Run Frontend

```powershell
cd frontend
npm install
npm run dev
```

Open:

```text
http://localhost:5173
http://127.0.0.1:5173
```

## 3. Scenario 1 - Pre Obligation Fail

Page: `Register / Drop Simulator`

Steps:

- Action: `REGISTER`
- `studentId`: `SV001`
- `classId`: `CS102_01`
- Leave `confirmedRegistrationRule` unchecked
- Submit request

Expected result:

```text
decision = DENY
failedPolicy = P17_AgreeRegistrationRule_PreB0
phase = PRE
predicate = OBLIGATION
denyReason = REGULATION_NOT_CONFIRMED
```

Meaning:

```text
Demonstrates preB0 obligation. The request is denied because the student has not confirmed the registration rule.
```

## 4. Scenario 2 - Successful Register

Page: `Register / Drop Simulator`

Steps:

- Action: `REGISTER`
- `studentId`: `SV001`
- `classId`: `CS102_01`
- Check `confirmedRegistrationRule`
- Keep `sessionLeaseValid = true`
- Submit request

Expected result:

```text
decision = ALLOW
sessionStatus = COMMITTED
DecisionTrace contains PRE, ONGOING, POST
snapshotBefore and snapshotAfter are available
```

Meaning:

```text
Demonstrates UCON mutability: reserved seat, enrollment, credits, schedule, registered classes, and tuition debt can be changed through policy updates.
```

## 5. Scenario 3 - Drop Not Registered

Page: `Register / Drop Simulator`

Steps:

- Action: `DROP`
- Use a student/class pair that does not have a valid registration
- Submit request

Expected result:

```text
decision = DENY
failedPolicy = P16_DropOnlyIfRegistered_PreA0
denyReason = NOT_REGISTERED
phase = PRE
predicate = AUTHORIZATION
```

Meaning:

```text
Demonstrates DROP authorization through PDP policy evaluation, not controller hard-code.
```

## 6. Scenario 4 - Monitoring And Revoke

Page: `Monitoring Demo`

Steps:

- Click `Maintenance ON`
- Optionally change class status to `LOCKED`
- Optionally add hold `ACADEMIC_HOLD`
- Click manual recheck if needed

Expected result:

```text
Response contains checkedSessions
Response contains revokedSessions
Response is JSON and can be shown in JsonPanel
```

Meaning:

```text
Demonstrates UCON continuity: active sessions can be rechecked when environment, object, or subject attributes change.
```

## 7. Scenario 5 - Policy Explorer

Page: `Policy Explorer`

Filters to show:

- Phase: `ONGOING`
- Predicate: `CONDITION`
- Predicate: `AUTHORIZATION`
- Predicate: `OBLIGATION`

Policies to point out:

```text
P13_EmergencyMaintenance_OnC0
P20_ReserveSeat_OnA2
P27_SessionLease_OnB0
P12_AuditAndTrace_PostB3
```

Meaning:

```text
Demonstrates that the runtime is backed by explicit UCONABC policies with phase, predicate, update timing, metadata, and lifecycle status.
```

## 8. Suggested Screenshots

- Dashboard UCON coverage
- Policy Explorer filtered by `ONGOING`
- Register denied by `P17_AgreeRegistrationRule_PreB0`
- DecisionTrace with phase and predicate details
- Monitoring Demo with `checkedSessions` and `revokedSessions`
- Successful Register with `COMMITTED`
- Validation Report
- PAP Lifecycle

