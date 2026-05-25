# Benchmark Result

Tai lieu nay ghi lai benchmark thuc nghiem nho cho `UCONKMA`.

## Muc tieu

Benchmark tra loi 2 cau hoi:

- pipeline `validate -> analyze -> PAP filter` co con kha thi khi so policy tang len khong
- chi phi tang co gan tuyen tinh va con chap nhan duoc cho bai toan dang ky hoc phan khong

## Pham vi do

Harness hien tai do chi phi cua pipeline policy-model:

1. `PolicyValidator`
2. `PolicyAnalyzer`
3. `PolicyAdministrationPoint` loc chi giu `ACTIVE` policies

Khong do latency REST end-to-end. Muc tieu cua benchmark nay la chi phi enforcement/model-processing o tang policy.

## Cach do

- lenh chay:

```powershell
cd e:\UCON_KMA\engine
.\apache-maven-3.9.6\bin\mvn.cmd -Dtest=PolicyBenchmarkSuite test
```

- policy goc duoc nhan ban trong bo nho de tao cac moc:
  - `25`
  - `50`
  - `100`
  - `500`
- clone benchmark duoc doi `policyId` va `priority` de van hop le voi semantic validator
- warm-up: `3`
- sample: `10`
- so do:
  - `Avg`
  - `P95`
  - `P99`

## Moi truong do

- ngay do: `2026-05-25`
- may local: workspace `E:\UCON_KMA`
- JDK: `21.0.9`
- Maven local repo: `E:\UCON_KMA\.m2\repository`
- trace runtime va DB web flow khong duoc dua vao benchmark nay

## Ket qua

| Policy count | Avg ms | P95 ms | P99 ms | Notes |
|---|---:|---:|---:|---|
| 25 | 3.389 | 4.599 | 4.599 | validate + analyze + PAP filter |
| 50 | 4.402 | 5.696 | 5.696 | validate + analyze + PAP filter |
| 100 | 5.075 | 7.001 | 7.001 | validate + analyze + PAP filter |
| 500 | 23.026 | 32.336 | 32.336 | validate + analyze + PAP filter |

## Nhan xet

- tu `25 -> 100` policy, latency tang nhe va van o muc vai milliseconds
- moc `500` policy van chay on dinh, nhung bat dau thay ro chi phi validator/analyzer tang len
- doi voi quy mo policy hien tai cua do an, pipeline nay van kha thi
- chi phi benchmark nay nghieng ve `model validation/analyze`, khong phai chi phi `REST + DB + trace` end-to-end

## Gioi han

- benchmark dung policy clone trong bo nho, khong phai 500 policy duoc thiet ke tay hoan toan khac nhau
- chua tach rieng `validator time`, `analyzer time`, `trace overhead`
- chua do throughput song song
