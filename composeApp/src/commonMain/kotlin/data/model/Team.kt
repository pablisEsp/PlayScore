package data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Team(
    @Transient
    val id: String = "",
    val name: String = "",
    val presidentId: String = "",
    val vicePresidentId: String? = null,
    val captainIds: List<String> = emptyList(),
    val playerIds: List<String> = emptyList(),
    val pointsTotal: Int = 0,
    val createdAt: String = "",
    val description: String = "",
    val logoUrl: String = "",
    val location: String = "",
    val ranking: Int = 0,
    val totalWins: Int = 0,
    val totalLosses: Int = 0
)