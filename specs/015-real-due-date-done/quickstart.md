# Quickstart: Kiểm thử Done cho Real Due Date

## Điều kiện

- Có sẵn Gradle Wrapper.
- Không cần Jira thật.

## Kiểm thử

Chạy:

```sh
./gradlew :composeApp:jvmTest --tests com.jiratools.data.StatusChangelogMapperTest
```

Fixture phải bao gồm transition:
- `Close`;
- `Resolve`;
- `Done`;
- biến thể chữ hoa/thường của `Done`;
- trạng thái chứa chuỗi `Done` nhưng không khớp chính xác.

## Kết quả mong đợi

- `Done` và biến thể chữ hoa/thường được tính.
- `Done Review` không được tính.
- `Close`, `Resolve`, `Done` cạnh tranh theo timestamp cuối cùng.
- Transition không hợp lệ không làm mất candidate hợp lệ đã chọn trong lịch sử.
