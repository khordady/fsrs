import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import app.arteh.flashcard.db.FlashCard
import app.arteh.flashcard.db.Grade
import app.arteh.flashcard.db.Rating
import java.lang.Math.min
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.ranges.coerceIn

@SuppressLint("DefaultLocale")
class FSRS(
    private val requestRetention: Double,
    private val params: List<Double>,
    private val isReview: Boolean = false,
) {

    data class InitState(var difficulty: Double = 0.0, var stability: Double = 0.0)

    private val decay = -params[20]
    private val factor = 0.9.pow(1.0 / decay) - 1
    private val enableFuzz = true;

    var gradeList = mutableListOf<Grade>(
        Grade(R.color.grad_blue, "Easy", 0, 0, "", Rating.Easy),
        Grade(R.color.grad_green, "Good", 0, 0, "", Rating.Good),
        Grade(R.color.grad_purple, "Hard", 0, 0, "", Rating.Hard),
        Grade(R.color.grad_red, "Again", 0, 0, "", Rating.Again),
    )

    fun calculate(flashCard: FlashCard): List<Grade> {
        val isNewCard = flashCard.reviewCount == 0

        var stateAgain: InitState
        var stateHard: InitState
        var stateGood: InitState
        var stateEasy: InitState


        var durationHard = 10 * 60 * 1000L //10min
        var durationGood: Long
        var durationEasy: Long

        var ivlHard = 0
        var ivlGood = 0
        var ivlEasy = 0

        var txtHard: String
        var txtGood: String
        var txtEasy: String

        var dayConvertor: Long = 24 * 60 * 60 * 1000

        if (isNewCard) {
            stateAgain = initState(Rating.Again)
            stateHard = initState(Rating.Hard)
            stateGood = initState(Rating.Good)
            stateEasy = initState(Rating.Easy)

            ivlGood = nextInterval(stateGood.stability)
            ivlEasy = nextInterval(stateEasy.stability)
            ivlEasy = max(ivlEasy, ivlGood + 1)

            txtHard = "10 Min"
            txtGood = "$ivlGood days"
            txtEasy = "$ivlEasy days"

            durationGood = ivlGood * dayConvertor
            durationEasy = ivlEasy * dayConvertor


        } else if (flashCard.interval == 0) {
            val lastD = flashCard.difficulty
            val lastS = flashCard.stability

            stateAgain = InitState(
                difficulty = nextDifficulty(lastD, Rating.Again),
                stability = nextShortTermStability(lastS, Rating.Again)
            )
            stateHard = InitState(
                difficulty = nextDifficulty(lastD, Rating.Hard),
                stability = nextShortTermStability(lastS, Rating.Hard)
            )
            stateGood = InitState(
                difficulty = nextDifficulty(lastD, Rating.Good),
                stability = nextShortTermStability(lastS, Rating.Good)
            )
            stateEasy = InitState(
                difficulty = nextDifficulty(lastD, Rating.Easy),
                stability = nextShortTermStability(lastS, Rating.Easy)
            )

            ivlGood = nextInterval(stateGood.stability)
            ivlEasy = nextInterval(stateEasy.stability)
            ivlEasy = max(ivlEasy, ivlGood + 1)

            txtHard = "< 10 min"
            txtGood = "$ivlGood days"
            txtEasy = "$ivlEasy days"

            durationGood = ivlGood * dayConvertor
            durationEasy = ivlEasy * dayConvertor

        } else {
            val interval = flashCard.interval
            val lastD = flashCard.difficulty
            val lastS = flashCard.stability

            val retrievability = forgettingCurve(interval.toDouble(), lastS)

            stateAgain = InitState(
                difficulty = nextDifficulty(lastD, Rating.Again),
                stability = nextForgetStability(lastD, lastS, retrievability)
            )
            stateHard = InitState(
                difficulty = nextDifficulty(lastD, Rating.Hard),
                stability = nextRecallStability(lastD, lastS, retrievability, Rating.Hard)
            )
            stateGood = InitState(
                difficulty = nextDifficulty(lastD, Rating.Good),
                stability = nextRecallStability(lastD, lastS, retrievability, Rating.Good)
            )
            stateEasy = InitState(
                difficulty = nextDifficulty(lastD, Rating.Easy),
                stability = nextRecallStability(lastD, lastS, retrievability, Rating.Easy)
            )

            ivlHard = nextInterval(stateHard.stability)
            ivlGood = nextInterval(stateGood.stability)
            ivlEasy = nextInterval(stateEasy.stability)

            ivlHard = kotlin.math.min(ivlHard, ivlGood)
            ivlGood = kotlin.math.min(ivlGood, ivlHard + 1)
            ivlEasy = kotlin.math.min(ivlEasy, ivlGood + 1)

            txtHard = "$ivlHard days"
            txtGood = "$ivlGood days"
            txtEasy = "$ivlEasy days"

            durationHard = ivlHard * dayConvertor
            durationGood = ivlGood * dayConvertor
            durationEasy = ivlEasy * dayConvertor
        }

        gradeList[0] = gradeList[0].copy(
            stability = stateEasy.stability, difficulty = stateEasy.difficulty,
            durationMillis = durationEasy, interval = ivlEasy, txt = txtEasy
        )
        gradeList[1] = gradeList[1].copy(
            stability = stateGood.stability, difficulty = stateGood.difficulty,
            durationMillis = durationGood, interval = ivlGood, txt = txtGood
        )
        gradeList[2] = gradeList[2].copy(
            stability = stateHard.stability, difficulty = stateHard.difficulty,
            durationMillis = durationHard, interval = ivlHard, txt = txtHard
        )
        gradeList[3] = gradeList[3].copy(
            stability = stateAgain.stability, difficulty = stateAgain.difficulty,
            durationMillis = 3 * 60 * 1000L, txt = "< 3 Min"
        )

        return gradeList
    }

    private fun applyFuzz(
        interval: Double,
        fuzzFactor: Double,
        scheduledDays: Int = 0
    ): Double {
        if (!enableFuzz || interval < 2.5) return interval

        val ivl = interval.roundToInt()
        var minIvl = max(2, (ivl * 0.95 - 1).roundToInt())
        val maxIvl = (ivl * 1.05 + 1).roundToInt()

        if (isReview && ivl > scheduledDays)
            minIvl = max(minIvl, scheduledDays + 1)

        return floor(fuzzFactor * (maxIvl - minIvl + 1) + minIvl)
    }

    private fun forgettingCurve(interval: Double, stability: Double): Double {
        return exp(-interval / stability)
    }

    private fun generateFuzzFactor(): Double {
        val seed = System.currentTimeMillis()
        val random = Random(seed)
        return random.nextDouble()  // returns value between 0.0 and 1.0
    }

    private fun initDifficulty(rating: Rating): Double {
        val base = params[4]
        val exponent = params[5] * (rating.value - 1)
        val raw = base - exp(exponent) + 1
        return String.format("%.2f", raw.coerceIn(1.0, 10.0)).toDouble()
    }

    private fun initStability(rating: Rating): Double {
        val index = rating.value - 1
        val value = params.getOrElse(index) { 0.1 }
        return String.format("%.2f", value.coerceAtMost(0.1)).toDouble()
    }

    private fun initState(rating: Rating): InitState {
        return InitState(
            difficulty = initDifficulty(rating),
            stability = initStability(rating)
        )
    }

    private fun linearDamping(delta: Double, oldD: Double): Double {
        return delta * (10 - oldD / 9)
    }

    private fun meanReversion(initD: Double, nextD: Double): Double {
        return params[7] * initD + (1 - params[7]) * nextD
    }

    private fun nextInterval(
        stability: Double,
        maxInterval: Int = 36500, lastInterval: Int = 0
    ): Int {
        val fuzzFactor = generateFuzzFactor()
        val rawInterval = stability / factor * (requestRetention.pow(1 / decay) - 1)
        val fuzzed = applyFuzz(rawInterval, fuzzFactor, scheduledDays = lastInterval)
        return fuzzed.roundToInt().coerceIn(1, maxInterval)
    }

    private fun nextDifficulty(currentD: Double, rating: Rating): Double {
        val deltaD = -params[6] * (rating.value - 3)
        val damped = linearDamping(deltaD, currentD)
        val nextD = currentD + damped
        val reverted = meanReversion(initDifficulty(Rating.Easy), nextD)
        return String.format("%.2f", reverted.coerceIn(1.0, 10.0)).toDouble()
    }

    private fun nextShortTermStability(currentS: Double, rating: Rating): Double {
        var sinc = exp(params[17] * (rating.value - 3 + params[18])) * currentS.pow(-params[19])
        if (rating.value >= 3) {
            sinc = max(sinc, 1.0)
        }
        return String.format("%.2f", abs(currentS * sinc)).toDouble()
    }

    private fun nextForgetStability(
        difficulty: Double,
        stability: Double,
        retrievability: Double
    ): Double {
        val sMin = stability / exp(params[17] * params[18])

        val result = params[11] *
                difficulty.pow(-params[12]) *
                ((stability + 1).pow(params[13]) - 1) *
                exp((1 - retrievability) * params[14])

        return "%.2f".format(min(result, sMin)).toDouble()
    }

    private fun nextRecallStability(d: Double, s: Double, r: Double, rating: Rating): Double {
        val hardPenalty = if (rating == Rating.Hard) params[15] else 1.0
        val easyBonus = if (rating == Rating.Easy) params[16] else 1.0

        val factor = exp(params[8]) *
                (11 - d) *
                s.pow(-params[9]) *
                (exp((1 - r) * params[10]) - 1) *
                hardPenalty *
                easyBonus

        val result = s * (1 + factor)
        return "%.2f".format(result).toDouble()
    }
}
