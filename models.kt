import androidx.annotation.ColorRes

enum class Rating(val value: Int) {
    Again(1),
    Hard(2),
    Good(3),
    Easy(4)
}

data class Grade(
    @ColorRes val color: Int,
    val title: String,
    val interval: Long = 0,
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
    var lastReview : LocalDateTime = LocalDateTime.now()
)
