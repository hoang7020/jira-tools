package com.jiratools.data

/**
 * Ngoại lệ mạng giả lập dùng chung cho các bài kiểm thử.
 *
 * [JiraErrorMapper] phân loại lỗi theo `simpleName` của lớp, nên tên lớp ở đây phải trùng với
 * tên lớp thật trên JVM (`java.net.UnknownHostException`, `io.ktor.client.network.sockets.*`…).
 * Vì vậy các lớp này được khai báo tập trung một chỗ để tránh trùng lặp giữa các tệp kiểm thử.
 */
internal class UnknownHostException(
    message: String,
) : Exception(message)

internal class ConnectException(
    message: String,
) : Exception(message)

internal class IOException(
    message: String,
) : Exception(message)

internal class HttpRequestTimeoutException(
    message: String,
) : Exception(message)

internal class ConnectTimeoutException(
    message: String,
) : Exception(message)
