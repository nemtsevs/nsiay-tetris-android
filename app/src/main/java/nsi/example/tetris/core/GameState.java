package nsi.example.tetris.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import nsi.example.tetris.config.GameConfig;

/**
 * Главный класс игровой логики (Model).
 * <p>
 * Адаптирован под Android: использует константы из Kotlin-объекта {@link GameConfig}.
 * Управляет состоянием сессии: доска, текущая фигура, счет, уровни, пауза.
 * UI-слой должен взаимодействовать с игрой <b>только</b> через этот класс.
 */
public final class GameState {
    private static final List<Tetromino> PIECES = Tetromino.all();
    private final List<Tetromino> bag = new ArrayList<>();

    private final long baseSpeed = GameConfig.BASE_SPEED;
    private final double speedFactor = GameConfig.SPEED_FACTOR;
    private final long minSpeed = GameConfig.MIN_SPEED;

    private final Board board = new Board();
    private Tetromino current;
    private int px, py;
    private int score;
    private boolean gameOver;
    private boolean paused;
    private boolean started = false;
    private boolean ghostEnabled = true;
    private boolean nextPieceEnabled = true;

    public GameState() {
        spawn();
    }

    // --- Геттеры состояния ---

    public Board getBoard() {
        return board;
    }

    public Tetromino getCurrent() {
        return current;
    }

    public int getPx() {
        return px;
    }

    public int getPy() {
        return py;
    }

    public int getScore() {
        return score;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public boolean isPaused() {
        return paused;
    }

    public boolean isStarted() {
        return started;
    }

    public boolean isGhostEnabled() {
        return ghostEnabled;
    }

    public boolean isNextPieceEnabled() {
        return nextPieceEnabled;
    }

    // --- Управление игрой ---

    /**
     * Запускает игру (перевод из состояния "ожидания" в активную игру).
     */
    public void startGame() {
        if (!gameOver) started = true;
    }

    /**
     * Переключает видимость "призрака" (тени фигуры на дне).
     */
    public void toggleGhost() {
        ghostEnabled = !ghostEnabled;
    }

    /**
     * Переключает видимость превью следующей фигуры.
     */
    public void toggleNextPiece() {
        nextPieceEnabled = !nextPieceEnabled;
    }

    /**
     * Вычисляет Y-координату, на которой фигура зафиксируется при Hard Drop.
     * Используется для отрисовки "призрака" (Ghost Piece).
     *
     * @return самая низкая допустимая Y-координата для текущей фигуры
     */
    public int getGhostPy() {
        if (current == null) return py;
        int ghostY = py;
        while (canPlace(current, px, ghostY + 1)) {
            ghostY++;
        }
        return ghostY;
    }

    /**
     * Возвращает следующую фигуру из "мешка", не извлекая её.
     * Если мешок пуст, он виртуально заполняется для просмотра.
     *
     * @return следующая фигура
     */
    public Tetromino getNextPiece() {
        if (bag.isEmpty()) {
            bag.addAll(PIECES);
            Collections.shuffle(bag);
        }
        return bag.get(0);
    }

    /**
     * Вычисляет текущий уровень на основе набранных очков (каждые 500 очков).
     */
    public int getLevel() {
        return 1 + (score / 500);
    }

    /**
     * Вычисляет задержку между тиками (падениями) фигуры в наносекундах.
     * Использует экспоненциальную кривую (как в Tetris Guideline):
     * каждый уровень умножает задержку на {@link GameConfig#SPEED_FACTOR}.
     */
    public long getTickDurationNanos() {
        double factor = Math.pow(speedFactor, getLevel() - 1);
        long currentDelay = (long) (baseSpeed * factor);
        return Math.max(minSpeed, currentDelay);
    }

    // --- Действия игрока ---

    /**
     * Продвигает игру на один шаг (тик).
     * Если фигура может упасть — она падает. Если нет — фиксируется.
     * Не делает ничего, если игра на паузе, окончена или не начата.
     */
    public void tick() {
        if (isFrozen()) return;
        if (canPlace(current, px, py + 1)) py++;
        else lock();
    }

    public void moveLeft() {
        if (!isFrozen() && canPlace(current, px - 1, py)) px--;
    }

    public void moveRight() {
        if (!isFrozen() && canPlace(current, px + 1, py)) px++;
    }

    public void rotateCW() {
        if (isFrozen()) return;
        Tetromino rotated = current.rotatedCW();
        if (canPlace(rotated, px, py)) current = rotated;
    }

    public void rotateCCW() {
        if (isFrozen()) return;
        Tetromino rotated = current.rotatedCCW();
        if (canPlace(rotated, px, py)) current = rotated;
    }

    /**
     * Мгновенно сбрасывает фигуру на дно доски и фиксирует её.
     * <p>
     * Начисляет +2 очка за каждую пройденную клетку (стандарт Guideline).
     */
    public void hardDrop() {
        if (isFrozen()) return;
        int distance = 0;
        while (canPlace(current, px, py + 1)) {
            py++;
            distance++;
        }
        score += distance * 2;
        lock();
    }

    /**
     * Ставит игру на паузу или снимает её. Не работает после Game Over.
     */
    public void togglePause() {
        if (!started || gameOver) return;
        paused = !paused;
    }

    // --- Внутренняя логика ---

    private boolean isFrozen() {
        return !started || gameOver || paused;
    }

    /**
     * Фиксирует текущую фигуру на доске, начисляет очки за линии и спавнит новую.
     */
    private void lock() {
        for (Point c : current.cells()) {
            board.set(px + c.x(), py + c.y(), current.color());
        }

        int lines = board.clearFullLines();
        switch (lines) {
            case 1 -> score += 100;
            case 2 -> score += 300;
            case 3 -> score += 500;
            case 4 -> score += 800;
        }

        spawn();
    }

    /**
     * Создает новую фигуру сверху. Использует алгоритм "7-bag" (7-bag randomizer),
     * чтобы избежать длинных засух нужной фигуры.
     * Также проверяет условие Game Over (если спавн заблокирован).
     */
    private void spawn() {
        if (bag.isEmpty()) {
            bag.addAll(PIECES);
            Collections.shuffle(bag);
        }
        current = bag.remove(0);

        px = (Board.WIDTH - current.boxSize()) / 2;
        py = 0;
        if (!canPlace(current, px, py)) {
            gameOver = true;
        }
    }

    /**
     * Проверяет, можно ли разместить фигуру в указанной позиции без коллизий.
     */
    private boolean canPlace(Tetromino t, int x0, int y0) {
        for (Point c : t.cells()) {
            if (board.isOccupied(x0 + c.x(), y0 + c.y())) return false;
        }
        return true;
    }

    /**
     * Сбрасывает игру в состояние "готов к старту".
     * Полностью очищает доску, сбрасывает очки и флаги.
     */
    public void restart() {
        for (int y = 0; y < Board.HEIGHT; y++)
            for (int x = 0; x < Board.WIDTH; x++)
                board.set(x, y, 0);

        bag.clear();
        score = 0;
        gameOver = false;
        paused = false;
        started = false;
        ghostEnabled = true;
        nextPieceEnabled = true;
        spawn();
    }

    // Пакетно-приватный метод для тестирования
    void addScoreForTesting(int points) {
        this.score += points;
    }

    /**
     * Принудительно устанавливает текущую фигуру и её позицию.
     * Используется ТОЛЬКО в тестах для детерминированного сценария
     * (например, очистка 4 линий I-фигурой).
     */
    void setCurrentPieceForTesting(Tetromino piece, int x, int y) {
        this.current = piece;
        this.px = x;
        this.py = y;
    }
}
