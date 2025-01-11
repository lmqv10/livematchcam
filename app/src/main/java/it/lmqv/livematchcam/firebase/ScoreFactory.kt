package it.lmqv.livematchcam.firebase

import com.google.firebase.database.DataSnapshot

class ScoreFactory {

    companion object {
        @Volatile
        private var INSTANCE: ScoreFactory? = null

        fun getInstance(): ScoreFactory {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ScoreFactory().also { INSTANCE = it }
            }
        }
    }

    fun buildByType(scoreType: String, snapshot: DataSnapshot): Any {
        var scoreSnapshot = snapshot.child("score")
        var score: Any = when (scoreType) {
            "Football" -> {
                FootballScore(
                    scoreSnapshot.child("home").getValue(Int::class.java) ?: 0,
                    scoreSnapshot.child("away").getValue(Int::class.java) ?: 0,
                    scoreSnapshot.child("period").getValue(String::class.java) ?: "",
                    scoreSnapshot.child("currentPeriodStartTimestamp").getValue(Long::class.java) ?: 0L)
            }
            "Volley" -> {
                VolleyScore(
                    sets = scoreSnapshot.child("sets").children.map {
                        SetScore(
                            home = it.child("home").getValue(Int::class.java) ?: 0,
                            guest = it.child("guest").getValue(Int::class.java) ?: 0
                        )
                    },
                    currentSet = scoreSnapshot.child("currentSet").getValue(Int::class.java) ?: 1
                )
            }
            else -> {
                {}
            }
        }
        return score
    }
}