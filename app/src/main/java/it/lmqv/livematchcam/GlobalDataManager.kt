package it.lmqv.livematchcam

class Team {
    var name : String = ""
    var color : Int = 0
    var score : Int = 0
}
/*
object GlobalDataManagerHelper {
    fun getServerURI(server: String, key: String) : String
    {
        return "${server}/${key}";
    }
}
*/
object GlobalDataManager {

    var server = ""
    var key = "";

    private var _serverUri = ""
    //var server = "rtmp://a.rtmp.youtube.com/live2"
    //var key = "yyx0-at5u-b330-4avg-4kx6";

    fun getServerURI() : String
    { return this._serverUri; }
    fun setServerURI(server: String, key: String)
    { this._serverUri = "${server}/${key}"; }

    var homeTeam : Team = Team()
    var awayTeam : Team = Team()

    var leftZoomDegreeTrigger : Int = 35
    var rightZoomDegreeTrigger : Int = 40
    var autoZoomEnabled : Boolean = true

    var zoomOffset : Float = 0.3f
}