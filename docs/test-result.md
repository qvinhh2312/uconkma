# Test Result

Tai lieu nay ghi lai ket qua kiem thu tong hop cho ban hien tai cua repo.

## Command

```powershell
cd e:\UCON_KMA\engine
.\apache-maven-3.9.6\bin\mvn.cmd clean test
```

```powershell
cd e:\UCON_KMA\dsl
.\apache-maven-3.9.6\bin\mvn.cmd test
```

## Result

```text
Engine: Tests run: 29, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

DSL: BUILD SUCCESS
```

Full engine suite completed locally in about `50.653 s` on the current machine snapshot.

## Y nghia

- policy engine van chay dung sau khi nang cap theo huong UCONABC
- `Authorization`, `Condition`, `Obligation`
- `preUpdates`, `ongoingUpdates`, `rollbackUpdates`, `postUpdates`
- `UsageSession`
- semantic validation
- invariant check
- decision trace
- race condition tests
- `onB0` ongoing obligation
- `PolicyAnalyzer` warnings cho `shadowing` va `conflicting priority`
- XMI explicit hon va nhat quan hon voi Ecore metamodel

## Luu y

Khi cap nhat them policy hoac metamodel/XMI, file nay can duoc cap nhat lai theo ket qua `mvn clean test` moi nhat.
