package it.lmqv.livematchcam.services.firebase

import it.lmqv.livematchcam.factories.Sports

/*data class Accounts(
    val accounts: Map<String, FirebaseAccount>
)*/

data class FirebaseAccount(
    val guid: String = "",
    val name: String = "",
    val admin: String = "",
    val logo: String = "",
    val users: List<String> = emptyList(),
    val channels: List<String> = emptyList(),
    val matches: Map<String, Match> = emptyMap(),
    val streams: List<Stream> = emptyList(),
    var settings : Settings = Settings()
)

data class Stream(
    val description: String = "",
    val logo: String = "",
    val server: String = "",
    val key: String = "",
    val owners: List<String> = emptyList()
)

data class FirebaseAccountDataContract(
    val guid: String = "",
    val logoURL: String = "",
    val title: String = "",
    val streams: List<Stream> = emptyList(),
    val settings : Settings = Settings(),
    val remoteScoreAvailable: Boolean = false
)

data class Settings(
    val youTubeEnabled: Boolean = false,
    val uvcEnabled: Boolean = false
)

/*TODO data class Event(
    val match: Match = Match(),
    var eventInfo: EventInfo = EventInfo()
    var banners: Banners = Banners()
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
    var sport: Sports = Sports.VOLLEY,
    var score: IScore = VolleyScore()
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
    val sets: List<SetScore> = listOf(SetScore()),
    val league: String = "",
    override val command: String = ""
) : IScore

data class SetScore(
    val home: Long = 0,
    val guest: Long = 0
)

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
data class Penta<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)
