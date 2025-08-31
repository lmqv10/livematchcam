package it.lmqv.livematchcam.services.firebase

import com.google.firebase.database.DataSnapshot
import it.lmqv.livematchcam.extensions.toMap
import it.lmqv.livematchcam.factories.Sports

object ScoreFactory {

    fun buildByType(scoreType: String, snapshot: DataSnapshot): IScore {
        var scoreSnapshot = snapshot.child("score")
        var score: Any = when (scoreType) {
            Sports.SOCCER.name -> {
                SoccerScore(
                    home = scoreSnapshot.child("home").getValue(Long::class.java) ?: 0,
                    away = scoreSnapshot.child("away").getValue(Long::class.java) ?: 0,
                    period = scoreSnapshot.child("period").getValue(String::class.java) ?: "",
                    command = scoreSnapshot.child("command").getValue(String::class.java) ?: ""
                )
            }
            Sports.VOLLEY.name -> {
                VolleyScore(
                    sets = scoreSnapshot.child("sets").children.map {
                            SetScore(
                                home = it.child("home").getValue(Long::class.java) ?: 0,
                                guest = it.child("guest").getValue(Long::class.java) ?: 0
                            )
                        }.ifEmpty { mutableListOf(SetScore()) }
                        .toMutableList(),
                    league = scoreSnapshot.child("league").getValue(String::class.java) ?: "",
                    command = scoreSnapshot.child("command").getValue(String::class.java) ?: ""
                )
            }
            else -> {
                UnknownScore()
            }
        }
        return score as IScore
    }

    fun buildMap(score: IScore) : Map<String, Any?> {
        return when (score) {
            is SoccerScore -> score.toMap()
            is VolleyScore -> score.toMap()
            else -> mapOf()
        }
    }

    fun getInitialScore(sport: Sports) : IScore {
        return when (sport) {
            Sports.SOCCER -> SoccerScore()
            Sports.VOLLEY -> VolleyScore()
        }
    }

    /*fun build(scoreType: String, scoreMap: Map<String, Any?>?): IScore {
        var score: IScore = when (scoreType) {
            Sports.SOCCER.name -> {
                mapToClass<SoccerScore>(scoreMap) as IScore
            }
            Sports.VOLLEY.name -> {
                mapToClass<VolleyScore>(scoreMap) as IScore
            }
            else -> {
                NoScore() as IScore
            }
        }
        return score
    }*/
}