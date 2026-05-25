# Benchmark Result

Tai lieu nay mo ta cach do va baseline benchmark nho cho `UCONKMA`.

## Muc tieu

Benchmark khong nham chay dua hieu nang, ma de tra loi cau hoi hoc thuat:

- policy engine co van kha thi khi so luong policy tang len khong
- validator, decision trace va update/rollback co lam runtime qua nang khong

## Pham vi do

Ban hien tai tap trung vao 3 thao tac:

1. `PRE + ONGOING + POST` cho `REGISTER`
2. `PRE + POST` cho `DROP`
3. khoi dong `PolicyDecisionPoint` de load `Ecore + XMI + validator + analyzer`

## Cach do de xuat

1. Giu nguyen policy set hien tai trong `xmi/ucon_policy.xmi`
2. Tao them cac ban sao policy khong thay doi semantics de nang tong so policy len cac moc:
   - `25` policy (baseline hien tai)
   - `50` policy
   - `100` policy
   - `500` policy
3. Do rieng:
   - average latency
   - p95
   - p99
   - policy-model load + validation time
   - trace/update overhead

## Ket qua baseline hien tai

Ban repo hien tai da duoc xac nhan o muc `functional baseline`:

- `mvn clean test` trong `engine` xanh
- `Tests run: 29, Failures: 0, Errors: 0, Skipped: 0`
- full engine test suite hoan tat tren may hien tai trong khoang `50.653 s`
- co `Authorization`, `Condition`, `Obligation`
- co `preUpdates`, `ongoingUpdates`, `rollbackUpdates`, `postUpdates`
- co `UsageSession`, `DecisionTrace`, `PolicyValidator`, `PolicyAnalyzer`

## Doc so lieu benchmark the nao

- neu latency tang tuyen tinh theo so policy, he thong van chap nhan duoc voi bai toan dang ky hoc phan
- neu `validator/analyzer` tang chi phi luc khoi dong nhung runtime request van on dinh, kien truc hien tai van hop ly
- neu `trace/update` la chi phi chinh, can toi uu o tang `POST` va `ONGOING`

## Ghi chu

Tai lieu nay la baseline benchmark plan chinh thuc cua repo. Khi bo sung harness do hieu nang rieng, cap nhat file nay bang bang so lieu thuc do tren may test cu the.
