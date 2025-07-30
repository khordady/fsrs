import androidx.annotation.ColorRes

enum class Rating(val value: Int) {
    Again(1),
    Hard(2),
    Good(3),
    Easy(4)
}

enum class CardPhase(val value: Int) {
    Added(0), //just added
    Know(1), //after first rate good
    Learning(2), //second rate good or rate again when reviewed
    Review(3), //third rate good
}

data class Grade(
    @ColorRes val color: Int,
    val title: String,
    val durationMillis: Long = 0,
    val interval: Int = 0,
    val txt: String = "0",
    val choice: Rating,
    val stability: Double = 0.0,
    val difficulty: Double = 0.0
)

data class FlashCard(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    
    var stability: Double = 2.5,
    var difficulty: Double = 2.5,
    var interval: Int = 0,
    var dueDate: LocalDateTime = LocalDateTime.now(),
    var reviewCount: Int = 0,
    var lastReview : LocalDateTime = LocalDateTime.now(),
    var phase: Int = 0,
)
