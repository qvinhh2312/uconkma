# Chương 1 & 2: Cơ sở Lý thuyết & Phân tích bài toán KMA theo mô hình UCON

## 1. Giới hạn của RBAC và Giải pháp từ UCON
Mô hình RBAC (Role-Based Access Control) truyền thống chỉ cấp quyền dựa trên Role (sinh viên, giảng viên). Tuy nhiên, bài toán đăng ký học phần tại KMA có những đặc thù động:
- Quyền đăng ký thay đổi liên tục theo trạng thái hệ thống: Số chỗ trống của lớp (sĩ số), số tín chỉ đã đăng ký, hoặc tình trạng học phí.
- Quyền đòi hỏi cập nhật tức thời (Post-updates): Ngay khi đăng ký thành công, sĩ số lớp phải tăng lên để giảm quota cho request cạnh tranh tiếp theo.

UCON (Usage Control) cung cấp kiểm soát dựa trên **Attributes** biến thiên (mutable) và hỗ trợ kiểm soát cả trước, trong và sau tiến trình. Do đó, UCON là mô hình lý tưởng để áp dụng vào KMA.

## 2. Phân tích 4 thành phần UCON cho KMA Registration

Ánh xạ bài toán Đăng ký học phần KMA sang 4 nguyên hàm lõi của UCON để phục vụ 12 policies hạt nhân của hệ thống.

### 2.1. SUBJECT (Chủ thể thao tác)
Đại diện cho Sinh viên (Student) đang thực hiện request. Gồm các Mutable Attributes:
- `studentId`: Mã sinh viên (Identifier).
- `currentCredits`: Tổng số tín chỉ đang bảo lưu/đã đăng ký trong học kỳ hiện tại (Integer).
- `maxCreditsEffective`: Hạn mức tín chỉ tối đa thực tế (Sau khi áp dụng rules cảnh cáo học vụ).
- `tuitionPaid`: Tình trạng tài chính/học phí (Boolean: true = đã đóng).
- `holds`: Danh sách các lệnh chặn hành chính, kỷ luật đang mắc phải (List<String> - rỗng là hợp lệ).
- `completedCourses`: Danh sách các mã học phần đã tích lũy điểm đạt (List<String>).
- `registeredScheduleSlots`: Danh sách lịch học đã đăng ký trong học kỳ để check trùng lịch (List<String>).

### 2.2. OBJECT (Đối tượng chịu tác động)
Đại diện cho Lớp học phần (ClassSection). Gồm các Attributes:
- `classId`: Mã lớp học phần duy nhất (Identifier).
- `courseId`: Mã môn học để đối chiếu môn tiên quyết (Identifier).
- `capacity`: Định mức tối đa của lớp / Sĩ số tối đa (Integer).
- `enrolled`: Số sinh viên hiện đã có trong lớp (Integer).
- `scheduleSlots`: Khung giờ của lớp học (List<String>).
- `status`: Trạng thái của lớp (Enum: OPEN, LOCKED, CANCELLED).
- `course.prerequisites`: Danh sách học phần tiên quyết yêu cầu của môn học (List<String>).

### 2.3. RIGHT (Quyền thi hành)
Quyền truy xuất và sử dụng hệ thống của Subject lên Object.
- `Register`: Đăng ký chọn lớp.
- `Drop`: Hủy chọn lớp.

### 2.4. ENVIRONMENT (Môi trường hệ thống)
Ngữ cảnh độc lập với cả Subject và Object nhưng đóng vai trò quan trọng trong việc chặn rule biên.
- `registrationPhase`: Giai đoạn hiện hành (Enum: NORMAL, LATE, ADJUSTMENT).
- `currentDateTime`: Ngày giờ thực thi transaction thực tế.
- `openTime`, `closeTime`: Khung băng thông thời gian cho phép của đợt đăng ký.
- `semester`: Học kỳ tác nghiệp (String).

## 3. Các quy tắc (Rule Types) trong UCON KMA
Kiến trúc UCON đánh giá các Rules dựa trên cấu trúc Logical của Predicates, phân loại thành 3 tập bắt buộc để hoàn thiện quy trình vòng lặp UCON:
1. **preAuthorization (Tiền kiểm tra - 7 Policies):** Đánh giá các thuộc tính tĩnh trước hành động (Như Tuittion, Prerequisite, Schedule, v.v.).
2. **ongoingAuthorization (Đánh giá liên tục/Re-check - 3 Policies):** Mô phỏng kiểm soát ở sát thời gian thực hiện db-commit đê chặn biến đổi thay đổi trạng thái (Như Hold, Capacity ở slot cuối, trạng thái khóa của Class).
3. **postUpdate
*Kết luận Step 1:* Ta đã bóc tách rõ ràng hệ thống Metadata làm nền tảng cho việc khởi tạo UML Domain Model và Domain-Specific Language (DSL) ở các bước tiếp theo đê hiện thực hóa hệ 12 Policy Rules đặc thù này.
 (Hậu cập nhật - 2 Policies):** Đảm nhận vai trò chuyển đổi trạng thái của model (Mutate state) và lưu log kiểm toán vào DB sau mọi quyết định.

---