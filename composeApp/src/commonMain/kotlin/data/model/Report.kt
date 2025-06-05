package data.model

import kotlinx.serialization.Serializable

@Serializable
data class Report(
    val id: String = "",
    val postId: String,
    val reporterId: String,
    val reason: String,
    val timestamp: String,
    val status: ReportStatus = ReportStatus.PENDING
)

@Serializable
enum class ReportStatus {
    PENDING, REVIEWED, IGNORED, ACCEPTED
}