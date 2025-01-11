package it.lmqv.livematchcam.firebase

data class Accounts(
    val accounts: Map<String, Account>
)

data class Account(
    val guid: String = "",
    val name: String = "",
    val admin: String = "",
    val logo: String = "",
    val users: List<String> = emptyList(),
    val channels: List<String> = emptyList(),
    val matches: Map<String, Match> = emptyMap()
)

data class Match(
    val homeTeam: String = "ABC",
    val homeColorHex: String = "#FFFFFF",
    val homeLogo: String = "",
    val guestTeam: String = "DEF",
    val guestColorHex: String = "#000000",
    val guestLogo: String = "",
    val scheduledTimestamp: Long = 0,
    val type: String = "",
    var score: Any? = null
)

data class FootballScore(
    val home: Int = 0,
    val away: Int = 0,
    val period: String = "",
    val currentPeriodStartTimestamp: Long = 0
)

data class VolleyScore(
    val sets: List<SetScore> = listOf(),
    val currentSet: Int = 1
)

data class SetScore(
    val home: Int = 0,
    val guest: Int = 0
)
