# 🧠 FSRS Scheduler (Version 6) – Kotlin Implementation

This repository contains a **Kotlin implementation** of the [Free Spaced Repetition Scheduler (FSRS)](https://github.com/open-spaced-repetition), version **6**. FSRS is a modern algorithm for spaced repetition, optimized using machine learning to improve long-term retention and scheduling efficiency.

> 🔬 FSRS v6 is the latest official version of the algorithm, offering better accuracy and flexibility compared to older algorithms like SM-2.

---

## ✅ Features

- 📌 Based on the **official FSRS v6 specification**
- 🔄 Supports all review responses: `Again`, `Hard`, `Good`, `Easy`
- 🧮 Returns updated **stability**, **difficulty**, and **interval**
- 🧱 Written in idiomatic **Kotlin**, easy to integrate into Android or JVM projects
- 🕒 Uses `LocalDateTime` for precise due date handling
- 🧪 Suitable for experimentation or direct production use

---

## 📦 Use Cases

This FSRS implementation can be used in:

- Flashcard apps (Anki-like)
- Educational software for medical, language, and test prep
- Any Kotlin-based spaced repetition project

---

## 🚀 Example Usage

```kotlin
val scheduler = FSRS()

val review = ReviewLog(
    rating = Rating.Good,
    lastReview = LocalDateTime.now().minusDays(3),
    due = LocalDateTime.now(),
    stability = 3.5,
    difficulty = 4.0
)

val result = scheduler.schedule(review)

println("Next Interval: ${result.interval}")
println("Updated Stability: ${result.stability}")
println("Updated Difficulty: ${result.difficulty}")
println("Next Due Date: ${result.due}")
```
## 📚 References
- [FSRS ALgorithm](https://github.com/open-spaced-repetition/fsrs4anki)
- [FSRS v6 Documentation & Research Paper](https://github.com/open-spaced-repetition/fsrs4anki/wiki/Research-resources)

## 🛠 License
This implementation is provided under the MIT License. Attribution to the original FSRS author is appreciated.

## 🙌 Contributing
Feel free to fork the repo and submit pull requests if you’d like to add enhancements or bug fixes. Issues and suggestions are always welcome.
