# Phân Kho Native Alpha 0.1

Giai đoạn đầu chuyển ứng dụng Phân Kho từ WebView/IndexedDB sang Android native.

## Kiến trúc

- Kotlin + Jetpack Compose: toàn bộ giao diện.
- Room: thành viên, chấm cơm, thực đơn, thu chi, việc cần làm, phương tiện, xăng dầu, cấu hình và nhật ký.
- Storage Access Framework: chọn vị trí sao lưu JSON, khôi phục JSON và xuất Excel `.xlsx`.
- Nhập dữ liệu cũ: đọc được định dạng `quan-ly-phan-kho-backup` của V58 và chuyển sang bảng Room.

## An toàn dữ liệu

Native Alpha dùng package `vn.quanlyphankho.nativealpha`, cài song song với V58 hiện tại. Không đổi package sang `vn.quanlyphankho.offline.v57` trước khi kiểm thử nhập–xuất dữ liệu hoàn chỉnh.

## Chức năng đã có trong Alpha 0.1

- Tab Hôm nay dạng vuốt ngang.
- Chấm cơm theo ngày và 3 bữa.
- Thực đơn theo ngày.
- Thu/chi nhanh theo ngày.
- Việc cần làm.
- Room schema v1 cho toàn bộ nhóm dữ liệu cốt lõi.
- Sao lưu/khôi phục qua SAF.
- Nhập backup JSON của V58.
- Xuất Excel OpenXML `.xlsx` không cần quyền bộ nhớ.
- GitHub Actions tạo APK debug.

## Build APK

Mỗi lần mã nguồn được cập nhật lên nhánh `main`, GitHub Actions tự build. APK nằm trong artifact `phan-kho-native-alpha-apk` của workflow **Build Native Alpha APK**.
