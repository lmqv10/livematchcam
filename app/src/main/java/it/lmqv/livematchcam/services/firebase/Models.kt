package it.lmqv.livematchcam.services.firebase

import it.lmqv.livematchcam.factories.Sports

/*data class Accounts(
    val accounts: Map<String, Account>
)*/

data class Account(
    val guid: String = "",
    val name: String = "",
    val admin: String = "",
    val logo: String = "",
    val users: List<String> = emptyList(),
    val channels: List<String> = emptyList(),
    val matches: Map<String, Match> = emptyMap()
)

/*data class Event(
    val match: Match = Match(),
    var eventInfo: EventInfo = EventInfo()
    TODO var banners: Banners = Banners()
)*/

data class Match(
    val homeTeam: String = "ABC",
    val homePrimaryColorHex: String = "#FFFFFF",
    val homeSecondaryColorHex: String = "#FFFFFF",
    val homeLogo: String = "",
    val guestTeam: String = "DEF",
    val guestPrimaryColorHex: String = "#000000",
    val guestSecondaryColorHex: String = "#000000",
    val guestLogo: String = "",
    val spotBannerURL: String = "",
    val spotBannerVisible: Boolean = false,
    val mainBannerURL: String = "",
    val mainBannerVisible: Boolean = false
)

data class EventInfo(
    var sport: Sports = Sports.SOCCER,
    var score: IScore = SoccerScore()
)

data class EventInfoData(
    var sport: String = "",
    var score: Map<String, Any?>? = null
)

data class Banners (
    val spotBannerUrls: List<String> = listOf(),
    val spotBannerVisible: Boolean = false,
    val mainBannerURL: List<String> = listOf(),
    val mainBannerVisible: Boolean = false
)

interface IScore {
    val command: String
}

class UnknownScore(override val command: String = "") : IScore

data class SoccerScore (
    val home: Long = 0,
    val away: Long = 0,
    val period: String = "1T",
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
