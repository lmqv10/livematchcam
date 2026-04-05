package it.lmqv.livematchcam.services.firebase

import com.google.firebase.database.IgnoreExtraProperties
import it.lmqv.livematchcam.factories.FilterPosition
import it.lmqv.livematchcam.factories.sports.Sports

/*data class Accounts(
    val accounts: Map<String, FirebaseAccount>
)*/

@IgnoreExtraProperties
data class FirebaseAccount(
    val guid: String = "",
    val name: String = "",
    val admin: String = "",
    val logo: String = "",
    val users: List<String> = emptyList(),
    val channels: List<String> = emptyList(),
    //val matches: Map<String, Match> = emptyMap(),
    val streams: List<Stream> = emptyList(),
    var settings: Settings = Settings()
)

data class Stream(
    val streamId: String = "",
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
    val settings: Settings = Settings(),
    val remoteScoreAvailable: Boolean = false
)

data class Settings(
    val maxStreams: Int = 0,
    val uvcEnabled: Boolean = false,
    val youTubeEnabled: Boolean = false,
    val youtubeChannelName: String = "",
    val youtubeLinked: Boolean = false,
)

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

data class Schedule(
    val id: String = "",
    val matchDate: Long = 0,
    val visible: Boolean = false,
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

data class ScoreboardOverlay(
    val position: FilterPosition = FilterPosition.TOP_LEFT,
    val size: Int = 30,
    val visible: Boolean = true,
)

data class FilterOverlayEvent(
    val position: FilterPosition = FilterPosition.TOP_LEFT,
    val filter: FilterOverlay? = null
)

data class FilterOverlay(
    val position: FilterPosition = FilterPosition.TOP_RIGHT,
    val urls: List<String> = emptyList(),
    val size: Int = 20,
    val visible: Boolean = false
)


data class OverlaysModel(
    val scoreboard: ScoreboardOverlay = ScoreboardOverlay(),
    val filters: List<FilterOverlay> = emptyList()
)

interface IScore {
    val command: String
}

class UnknownScore(override val command: String = "") : IScore

data class SoccerScore(
    val home: Long = 0,
    val away: Long = 0,
    val period: String = "1T",
    override val command: String = ""
) : IScore

data class BasketScore(
    val home: Long = 0,
    val away: Long = 0,
    val period: String = "1Q",
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
