package me.matsumo.grabee.core.datasource.db

import io.github.aakira.napier.Napier
import kotlinx.serialization.json.Json
import me.matsumo.grabee.core.datasource.db.entity.LearningProgressEntity
import me.matsumo.grabee.core.datasource.db.entity.LevelEntity
import me.matsumo.grabee.core.datasource.db.entity.UnitEntity
import me.matsumo.grabee.core.datasource.db.entity.UserProgressEntity
import me.matsumo.grabee.core.datasource.db.entity.WordEntity
import me.matsumo.grabee.core.model.VocabularyItem

class DatabaseSeeder(private val database: GrabeeDatabase) {

    suspend fun seedIfEmpty() {
        if (database.levelDao().count() > 0) return

        database.levelDao().insertAll(SEED_LEVELS)
        database.unitDao().insertAll(SEED_UNITS)
        database.wordDao().insertAll(SEED_WORDS)
        database.learningProgressDao().upsert(SEED_PROGRESS)
        SEED_USER_PROGRESS.forEach { database.userProgressDao().upsert(it) }

        Napier.d(tag = TAG) {
            "Seeded ${SEED_LEVELS.size} levels, ${SEED_UNITS.size} units, " +
                "${SEED_WORDS.size} words, ${SEED_USER_PROGRESS.size} progress rows"
        }
    }

    private companion object {
        const val TAG = "DatabaseSeeder"

        private val seedJson = Json { ignoreUnknownKeys = true }

        val SEED_LEVELS = listOf(
            LevelEntity("level-1", 1, "The Alphabet", 8, isPremium = false, orderIndex = 0),
            LevelEntity("level-2", 2, "Short Vowels", 9, isPremium = true, orderIndex = 1),
            LevelEntity("level-3", 3, "Long Vowels", 9, isPremium = true, orderIndex = 2),
            LevelEntity("level-4", 4, "Blends & Digraphs", 9, isPremium = true, orderIndex = 3),
            LevelEntity("level-5", 5, "Advanced Patterns", 9, isPremium = true, orderIndex = 4),
        )

        val SEED_UNITS = listOf(
            UnitEntity("level-1-unit-1", "level-1", 1, "Aa Bb Cc", 0),
            UnitEntity("level-1-unit-2", "level-1", 2, "Dd Ee Ff", 1),
            UnitEntity("level-1-unit-3", "level-1", 3, "Gg Hh Ii", 2),
            UnitEntity("level-1-unit-4", "level-1", 4, "Jj Kk Ll", 3),
            UnitEntity("level-1-unit-5", "level-1", 5, "Mm Nn Oo", 4),
            UnitEntity("level-1-unit-6", "level-1", 6, "Pp Qq Rr", 5),
            UnitEntity("level-1-unit-7", "level-1", 7, "Ss Tt Uu Vv", 6),
            UnitEntity("level-1-unit-8", "level-1", 8, "Ww Xx Yy Zz", 7),
        )

        val SEED_WORDS = listOf(
            // Unit 1: Aa Bb Cc
            word(
                unitId = "level-1-unit-1",
                text = "apple",
                phoneme = "/æ/",
                emoji = "\uD83C\uDF4E",
                orderIndex = 0,
                vocabulary = listOf(
                    VocabularyItem(text = "Apple", emoji = "\uD83C\uDF4E", orderIndex = 0),
                    VocabularyItem(text = "Ant", emoji = "\uD83D\uDC1C", orderIndex = 1),
                    VocabularyItem(text = "Airplane", emoji = "✈\uFE0F", orderIndex = 2),
                ),
            ),
            word(
                unitId = "level-1-unit-1",
                text = "bear",
                phoneme = "/b/",
                emoji = "\uD83D\uDC3B",
                orderIndex = 1,
                vocabulary = listOf(
                    VocabularyItem(text = "Bear", emoji = "\uD83D\uDC3B", orderIndex = 0),
                    VocabularyItem(text = "Ball", emoji = "⚽", orderIndex = 1),
                    VocabularyItem(text = "Banana", emoji = "\uD83C\uDF4C", orderIndex = 2),
                ),
            ),
            word(
                unitId = "level-1-unit-1",
                text = "cat",
                phoneme = "/k/",
                emoji = "\uD83D\uDC31",
                orderIndex = 2,
                vocabulary = listOf(
                    VocabularyItem(text = "Cat", emoji = "\uD83D\uDC31", orderIndex = 0),
                    VocabularyItem(text = "Car", emoji = "\uD83D\uDE97", orderIndex = 1),
                    VocabularyItem(text = "Cake", emoji = "\uD83C\uDF82", orderIndex = 2),
                ),
            ),
            // Unit 2: Dd Ee Ff
            word("level-1-unit-2", "dog", "/d/", "\uD83D\uDC36", 0),
            word("level-1-unit-2", "egg", "/ɛ/", "\uD83E\uDD5A", 1),
            word("level-1-unit-2", "fish", "/f/", "\uD83D\uDC1F", 2),
            // Unit 3: Gg Hh Ii
            word("level-1-unit-3", "gorilla", "/g/", "\uD83E\uDD8D", 0),
            word("level-1-unit-3", "horse", "/h/", "\uD83D\uDC0E", 1),
            word("level-1-unit-3", "insect", "/ɪ/", "\uD83D\uDC1B", 2),
            // Unit 4: Jj Kk Ll
            word("level-1-unit-4", "jet", "/dʒ/", "✈️", 0),
            word("level-1-unit-4", "kangaroo", "/k/", "\uD83E\uDD98", 1),
            word("level-1-unit-4", "lion", "/l/", "\uD83E\uDD81", 2),
            // Unit 5: Mm Nn Oo
            word("level-1-unit-5", "monkey", "/m/", "\uD83D\uDC12", 0),
            word("level-1-unit-5", "nut", "/n/", "\uD83C\uDF30", 1),
            word("level-1-unit-5", "octopus", "/ɒ/", "\uD83D\uDC19", 2),
            // Unit 6: Pp Qq Rr
            word("level-1-unit-6", "peach", "/p/", "\uD83C\uDF51", 0),
            word("level-1-unit-6", "queen", "/kw/", "\uD83D\uDC78", 1),
            word("level-1-unit-6", "rabbit", "/r/", "\uD83D\uDC30", 2),
            // Unit 7: Ss Tt Uu Vv
            word("level-1-unit-7", "seal", "/s/", "\uD83E\uDDAD", 0),
            word("level-1-unit-7", "turtle", "/t/", "\uD83D\uDC22", 1),
            word("level-1-unit-7", "umbrella", "/ʌ/", "☂️", 2),
            word("level-1-unit-7", "van", "/v/", "\uD83D\uDE90", 3),
            // Unit 8: Ww Xx Yy Zz
            word("level-1-unit-8", "wolf", "/w/", "\uD83D\uDC3A", 0),
            word("level-1-unit-8", "fox", "/ks/", "\uD83E\uDD8A", 1),
            word("level-1-unit-8", "yo-yo", "/j/", "\uD83C\uDFAF", 2),
            word("level-1-unit-8", "zebra", "/z/", "\uD83E\uDD93", 3),
        )

        val SEED_PROGRESS = LearningProgressEntity(
            activeLevelId = "level-1",
            activeUnitId = "level-1-unit-2",
            unitProgressPercent = 25,
        )

        val SEED_USER_PROGRESS = listOf(
            // Unit 1: hoàn thành đủ stars để unlock Unit 2
            UserProgressEntity("level-1-unit-1", stepIndex = 0, starsEarned = 3, completedAt = 0),
            UserProgressEntity("level-1-unit-1", stepIndex = 1, starsEarned = 3, completedAt = 0),
            // Unit 2: đã bắt đầu
            UserProgressEntity("level-1-unit-2", stepIndex = 0, starsEarned = 2, completedAt = 0),
        )

        private fun word(
            unitId: String,
            text: String,
            phoneme: String,
            emoji: String,
            orderIndex: Int,
            vocabulary: List<VocabularyItem> = emptyList(),
        ) = WordEntity(
            id = "$unitId-$text",
            unitId = unitId,
            text = text,
            phoneme = phoneme,
            emoji = emoji,
            imageAsset = null,
            wordAudioAsset = null,
            sentenceAudioAsset = null,
            orderIndex = orderIndex,
            vocabularyJson = if (vocabulary.isEmpty()) "[]" else seedJson.encodeToString(vocabulary),
        )
    }
}
