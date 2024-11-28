package it.lmqv.livematchcam

class Team {
    var name : String = ""
    var color : Int = 0
}

object GlobalDataManager {
    var homeTeam : Team = Team()
    var awayTeam : Team = Team()
}