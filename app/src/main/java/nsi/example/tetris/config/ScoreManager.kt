package nsi.example.tetris.config

import android.content.Context

/**
 * Сервис для сохранения и загрузки максимального рекорда игрока.
 * Использует [android.content.SharedPreferences] для локального хранения данных на устройстве.
 *
 * @param context Контекст приложения (обычно передается из Activity или View).
 */
class ScoreManager(context: Context) {
    private val prefs = context.getSharedPreferences("tetris_prefs", Context.MODE_PRIVATE)

    /**
     * Возвращает текущий рекорд игрока.
     *
     * @return количество набранных очков или 0, если рекорд еще не установлен.
     */
    fun getHighScore(): Int {
        return prefs.getInt("high_score", 0)
    }

    /**
     * Сохраняет новый рекорд, если он больше текущего.
     *
     * @param score Новый счет игрока.
     * @return `true`, если рекорд был обновлен, `false` — если текущий рекорд остался выше.
     */
    fun saveHighScore(score: Int): Boolean {
        val currentHighScore = getHighScore()
        if (score > currentHighScore) {
            prefs.edit().putInt("high_score", score).apply()
            return true
        }
        return false
    }
}
