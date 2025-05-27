package data.model

import kotlinx.serialization.Serializable

@Serializable
data class TeamJoinRequest(
    val id: String = "",
    val teamId: String = "",
    val userId: String = "",
    val status: RequestStatus = RequestStatus.PENDING,
    val timestamp: String = "",
    val responseTimestamp: String = "",
    val responseBy: String = ""
)

@Serializable
enum class RequestStatus {
    PENDING, APPROVED, REJECTED
}