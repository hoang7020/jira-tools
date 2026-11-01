package com.jiratools.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

/** Cấu trúc lỗi chuẩn của Jira REST API v3. */
@Serializable
data class ErrorCollectionDto(
    val errorMessages: List<String> = emptyList(),
    val errors: Map<String, String> = emptyMap(),
)

/** Kết quả `GET /rest/api/3/myself`, dùng để kiểm tra thông tin xác thực. */
@Serializable
data class MyselfDto(
    val accountId: String? = null,
    val displayName: String? = null,
    val emailAddress: String? = null,
    val active: Boolean = true,
    val timeZone: String? = null,
)

/** Thân yêu cầu của `POST /rest/api/3/search/jql`; `fields` là bắt buộc, thiếu thì Jira chỉ trả `id`. */
@Serializable
data class SearchJqlRequest(
    val jql: String,
    val fields: List<String>,
    val maxResults: Int,
    val nextPageToken: String? = null,
)

/** Thân yêu cầu cập nhật riêng Due Date của một issue Jira. */
@Serializable
data class IssueDueDateUpdateRequest(
    val fields: IssueDueDateFields,
)

@Serializable
data class IssueDueDateFields(
    @SerialName("duedate")
    val dueDate: String,
)

/** Một issue trong phản hồi tìm kiếm; chỉ các trường được yêu cầu là có giá trị. */
@Serializable
data class IssueDto(
    val id: String? = null,
    val key: String? = null,
    val fields: FieldsDto? = null,
)

@Serializable
data class FieldsDto(
    val summary: String? = null,
    val status: StatusDto? = null,
    val assignee: UserDto? = null,
    val priority: PriorityDto? = null,
    val updated: String? = null,
    val timetracking: TimeTrackingDto? = null,
    @SerialName("duedate")
    val dueDate: String? = null,
    val parent: ParentDto? = null,
    val issuetype: IssueTypeDto? = null,
    val subtasks: List<SubtaskDto> = emptyList(),
)

@Serializable
data class ParentDto(
    val key: String? = null,
)

@Serializable
data class IssueTypeDto(
    val subtask: Boolean = false,
)

@Serializable
data class SubtaskDto(
    val key: String? = null,
)

@Serializable
data class StatusDto(
    val id: String? = null,
    val name: String? = null,
    val statusCategory: StatusCategoryDto? = null,
)

@Serializable
data class StatusCategoryDto(
    val key: String? = null,
)

@Serializable
data class TimeTrackingDto(
    val originalEstimate: String? = null,
    val originalEstimateSeconds: Long? = null,
)

@Serializable
data class PriorityDto(
    val id: String? = null,
    val name: String? = null,
)

@Serializable
data class UserDto(
    val accountId: String? = null,
    val displayName: String? = null,
)

/** Phản hồi của `POST /rest/api/3/search/jql`; endpoint này không trả về tổng số ticket. */
@Serializable
data class SearchJqlResponse(
    val issues: List<IssueDto> = emptyList(),
    val nextPageToken: String? = null,
    val isLast: Boolean? = null,
    val names: Map<String, String> = emptyMap(),
    val warnings: List<String> = emptyList(),
)

@Serializable
data class BulkChangelogRequest(
    val issueIdsOrKeys: List<String>,
    val fieldIds: List<String>,
    val maxResults: Int,
    val nextPageToken: String? = null,
)

@Serializable
data class BulkChangelogResponse(
    val issueChangeLogs: List<IssueChangeLogDto> = emptyList(),
    val nextPageToken: String? = null,
)

@Serializable
data class IssueChangeLogDto(
    val issueId: String? = null,
    val changeHistories: List<ChangelogHistoryDto> = emptyList(),
)

@Serializable
data class ChangelogHistoryDto(
    val created: JsonPrimitive? = null,
    val items: List<ChangelogItemDto> = emptyList(),
)

@Serializable
data class ChangelogItemDto(
    val field: String? = null,
    val fieldId: String? = null,
    val fromString: String? = null,
    val toString: String? = null,
)

/** Thân yêu cầu của `POST /rest/api/3/search/approximate-count`. */
@Serializable
data class ApproximateCountRequest(
    val jql: String,
)

@Serializable
data class ApproximateCountResponse(
    val count: Int? = null,
)
