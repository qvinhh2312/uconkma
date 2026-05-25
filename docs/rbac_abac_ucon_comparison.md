# RBAC vs ABAC vs UCONKMA

Tai lieu nay tom tat vi sao bai toan dang ky hoc phan trong repo phu hop voi `UCON` hon so voi `RBAC` hoac `ABAC` chi danh gia tai request-time.

| Kich ban | RBAC | ABAC request-time | UCONKMA |
|---|---|---|---|
| Chua dong hoc phi | Khong du tinh linh hoat | Co the kiem tra | Co `preA0` |
| Thieu mon tien quyet | Khong mo ta tu nhien | Co the kiem tra | Co `preA0` |
| Trung lich hoc | Khong tu nhien | Co the kiem tra | Co `preA0` |
| Lop bi khoa giua luc xu ly | Khong ho tro | Kho xu ly | Co `onA0` |
| Maintenance bat giua giao dich | Khong ho tro | Kho xu ly | Co `onC0` |
| Tranh slot cuoi cung | Khong ho tro | Khong du | Co `onA0` + `onA2` + rollback |
| Buoc xac nhan quy che | Khong tu nhien | Co the chen them field | Co `preB0` |
| Nghia vu session lease trong luc xu ly | Khong ho tro | Rat kho | Co `onB0` |
| Audit/trace sau quyet dinh | Ngoai mo hinh | Ngoai mo hinh | Co `postB3` |
| Cap nhat cong no/tin chi/si so sau commit | Ngoai mo hinh | Thuong tach khoi policy | Co `postA3` |

## Ket luan

`UCONKMA` duoc chon vi bai toan dang ky hoc phan khong chi can cau tra loi `permit/deny` tai mot thoi diem. No can:

- `Authorization`
- `Condition`
- `Obligation`
- `mutability`
- `continuity`
- `traceability`

Day la ly do `UCONKMA` phu hop hon `RBAC` va day du hon `ABAC` thuan request-time cho bai toan nay.
