# Glossary & Assumptions (Định nghĩa & Giả định)

Tài liệu này định nghĩa các thuật ngữ chính và các quy tắc/giả định nghiệp vụ áp dụng trong suốt đồ án biểu diễn hệ thống phân quyền UCON cho bài toán Đăng ký học phần tại KMA. Điều giúp đảm bảo tính nhất quán về ngữ nghĩa của các hệ rules từ khâu DSL đến Engine evaluation.

## 1. Glossary (Thuật ngữ cốt lõi)

- **UCON (Usage Control):** Mô hình kiểm soát quyền truy cập thế hệ mới, quyết định quyền dựa trên Attributes có thể thay đổi liên tục trước, trong, và sau quá trình truy cập.
- **Attributes:** Giá trị trạng thái nội hàm gắn với Subject hoặc Object.
- **Metamodel (Abstract Syntax):** Lược đồ khái niệm định nghĩa các thành phần ngôn ngữ (ở file `ucon.ecore`). Tương tự cấu trúc Class trong OOP (XMI Schema).
- **Model Instance:** Thể hiện cụ thể của Metamodel, chứa dữ liệu thực tế (ở file `ucon_policies.xmi`). Tương tự một Object được khởi tạo từ Class.
- **Rules (DSL):** Cú pháp dạng chữ (Textual) dùng để viết chính sách dễ đọc với con người.
- **PEP (Policy Enforcement Point):** Điểm chặn request, gửi câu hỏi về quyền và thực thi quyết định cho phép hay từ chối.
- **PDP (Policy Decision Point):** Não bộ trung tâm thực hiện evaluation trên các policy tree.
- **PIP (Policy Information Point):** Nguồn cung cấp Attributes hiện tại cho PDP (mock DB).

## 2. Business Assumptions (Giả định Nghiệp vụ KMA)

Toàn bộ các rules sẽ tuân theo các giả định (constants/rules) sau:

- **Max Credits (Hạn mức tín chỉ tối đa):**
  - Mặc định đối với sinh viên bình thường: Tối đa 24 tín chỉ / học kỳ.
  - Sinh viên thuộc diện "Cảnh báo học vụ": Tối đa 14 tín chỉ / học kỳ.
  
- **Cảnh báo học vụ (Academic Warning):**
  - Là một cờ boolean (`academicWarning = true/false`) áp lên Subject.
  - Cần xét cờ này trước khi kiểm tra Hạn mức tín chỉ (Credit Limit).

- **Trạng thái Học phí (Tuition Paid):**
  - Sinh viên bắt buộc phải hoàn thành học phí học kỳ trước đó (`tuitionPaid = true`).
  - Nếu nợ học phí, mọi thao tác `Register` bị từ chối tuyệt đối ở khâu preAuthorization.

- **Môn học Đặc thù (Special Course):**
  - Các môn ngoại lệ (như Đồ án tốt nghiệp, Thực tập chuyên ngành).
  - Phải có cờ đánh dấu `course.special == true`. Được áp các quyền đặc biệt (ví dụ: yêu cầu sinh viên năm cuối, hoặc tổng tín chỉ tích lũy >= 120).

- **Phân loại Chính sách UCON áp dụng:**
  - **preAuthorization:** Đánh giá *trước* khi hành động xảy ra (ví dụ: Check sĩ số lớp `class.enrolled < class.capacity`).
  - **postUpdate:** Cập nhật Attribute *sau* khi hành động thành công (ví dụ: `class.enrolled++`).
  - **ongoingAuthorization:** Đánh giá *liên tục* trong suốt phiên. Nhưng do giới hạn của mô hình API request-response (không duy trì trạng thái TCP session dài hạn), ongoingAuthorization sẽ được *mô phỏng* thông qua kiểm tra lại (re-check) đồng thời lock data trước thời điểm commit (concurrency control).
