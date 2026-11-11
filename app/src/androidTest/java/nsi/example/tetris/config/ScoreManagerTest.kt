package nsi.example.tetris.config

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Инструментальные тесты для [ScoreManager].
 * <p>
 * Проверяют корректность сохранения и извлечения рекордов из [android.content.SharedPreferences].
 * Требуют запуска на реальном устройстве или эмуляторе (используют Android SDK).
 */
@RunWith(AndroidJUnit4::class)
class ScoreManagerTest {

    private lateinit var context: Context
    private lateinit var scoreManager: ScoreManager

    /**
     * Подготовка перед каждым тестом.
     * Очищает SharedPreferences, чтобы результаты предыдущих тестов
     * не влияли на результаты текущего.
     */
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("tetris_prefs", Context.MODE_PRIVATE)
            .edit().clear().apply()
        scoreManager = ScoreManager(context)
    }

    /**
     * Проверяет, что при первом запуске (когда рекордов еще нет) возвращается 0.
     */
    @Test
    fun initialState_returnsZero() {
        assertEquals(0, scoreManager.getHighScore())
    }

    /**
     * Проверяет, что новый рекорд сохраняется, если он превышает текущий.
     * Метод должен вернуть `true`, подтверждая обновление.
     */
    @Test
    fun saveHighScore_updatesIfNewRecordIsHigher() {
        val isNewRecord = scoreManager.saveHighScore(100)

        assertTrue("Должен вернуть true при новом рекорде", isNewRecord)
        assertEquals(100, scoreManager.getHighScore())
    }

    /**
     * Проверяет, что новый результат НЕ сохраняется, если он меньше текущего рекорда.
     * Метод должен вернуть `false`, а старый рекорд остаться неизменным.
     */
    @Test
    fun saveHighScore_doesNotUpdateIfScoreIsLower() {
        scoreManager.saveHighScore(200) // Ставим рекорд 200

        val isNewRecord = scoreManager.saveHighScore(150) // Пытаемся сохранить 150

        assertFalse("Не должен обновлять рекорд при меньшем счете", isNewRecord)
        assertEquals("Рекорд должен остаться 200", 200, scoreManager.getHighScore())
    }
}
