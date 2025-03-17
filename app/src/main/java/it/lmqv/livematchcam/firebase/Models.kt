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
    val spotBannerURL: String = "",
    var type: String = "",
    var score: Map<String, Any?>? = null
)

interface IScore {
    val command: String
}

class UnknownScore(override val command: String) : IScore

data class SoccerScore (
    val home: Long = 0,
    val away: Long = 0,
    val period: String = "",
    override val command: String = ""
) : IScore

data class VolleyScore(
    val sets: MutableList<SetScore> = mutableListOf(SetScore()),
    val league: String = "",
    override val command: String = ""
) : IScore

data class SetScore(
    val home: Long = 0,
    val guest: Long = 0
)

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
