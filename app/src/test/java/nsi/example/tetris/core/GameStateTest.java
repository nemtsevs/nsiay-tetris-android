package nsi.example.tetris.core;

import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

/**
 * Unit-тесты для главного класса игровой логики {@link GameState}.
 * <p>
 * Покрывают падение фигур, фиксацию, коллизии, работу паузы, смену уровней
 * и условия окончания игры (Game Over).
 */
public class GameStateTest {

    /**
     * Вспомогательный метод для создания и запуска игры перед тестами.
     */
    private GameState createState() {
        GameState state = new GameState();
        state.startGame();
        return state;
    }

    /**
     * Проверяет, что фигура падает при тике, фиксируется на дне
     * и новая фигура появляется сверху.
     */
    @Test
    public void pieceFallsThenLocksAndNewPieceSpawnsAtTop() {
        GameState state = createState();
        assertEquals(0, state.getPy());
        state.tick();
        assertEquals(1, state.getPy());

        // Предохранитель от бесконечного цикла
        int guard = 100;
        while (state.getPy() != 0 && guard-- > 0) state.tick();

        assertTrue(state.getBoard().isOccupied(4, Board.HEIGHT - 1) || state.getBoard().isOccupied(5, Board.HEIGHT - 1));
        assertEquals(0, state.getPy());
    }

    /**
     * Проверяет, что метод Hard Drop мгновенно опускает фигуру на дно
     * и фиксирует её.
     */
    @Test
    public void hardDropLocksPieceAtBottomAndSpawnsNew() {
        GameState state = createState();
        state.hardDrop();

        boolean locked = false;
        for (int x = 0; x < Board.WIDTH; x++) {
            if (state.getBoard().isOccupied(x, Board.HEIGHT - 1)) locked = true;
        }
        assertTrue(locked);
        assertEquals(0, state.getPy());
    }

    /**
     * Проверяет, что на паузе игровой тик не двигает фигуру,
     * а после снятия паузы — двигает.
     */
    @Test
    public void tickIsNoopWhilePausedAndWorksAfterUnpause() {
        GameState state = createState();
        state.togglePause();
        state.tick();
        assertEquals(0, state.getPy());

        state.togglePause();
        state.tick();
        assertEquals(1, state.getPy());
    }

    /**
     * Проверяет, что фигура не может выйти за левую границу поля.
     */
    @Test
    public void moveLeftBlockedAtLeftWall() {
        GameState state = createState();
        for (int i = 0; i < 10; i++) state.moveLeft();
        assertEquals(0, state.getPx());
    }

    /**
     * Проверяет, что фигура не может выйти за правую границу поля.
     * Максимальный X зависит от ширины (boxSize) конкретной фигуры.
     */
    @Test
    public void moveRightBlockedAtRightWall() {
        GameState state = createState();
        for (int i = 0; i < 10; i++) state.moveRight();

        int expectedMaxPx = Board.WIDTH - state.getCurrent().boxSize();
        assertEquals(expectedMaxPx, state.getPx());
    }

    /**
     * Проверяет, что все действия игрока (движение, поворот, сброс)
     * игнорируются, пока игра стоит на паузе.
     */
    @Test
    public void actionsAreNoopWhilePaused() {
        GameState state = createState();
        int px = state.getPx();
        state.togglePause();

        state.moveLeft();
        state.rotateCW();
        state.hardDrop();

        assertEquals(px, state.getPx());
    }

    /**
     * Проверяет, что игра заканчивается, если зона спавна новой фигуры заблокирована.
     */
    @Test
    public void gameOverWhenSpawnAreaOccupied() {
        GameState state = createState();
        Board b = state.getBoard();

        // Блокируем центральные колонки, где появляются фигуры
        b.set(4, 0, 1);
        b.set(5, 0, 1);
        b.set(4, 1, 1);
        b.set(5, 1, 1);

        state.hardDrop();

        assertTrue(state.isGameOver());
    }

    /**
     * Проверяет, что после Game Over нельзя поставить игру на паузу.
     */
    @Test
    public void togglePauseIsNoopWhenGameOver() {
        GameState state = createState();
        Board b = state.getBoard();
        b.set(4, 0, 1);
        b.set(5, 0, 1);
        b.set(4, 1, 1);
        b.set(5, 1, 1);
        state.hardDrop();

        state.togglePause();
        assertFalse(state.isPaused());
    }

    /**
     * Проверяет корректный расчет уровня и скорости падения фигур
     * в зависимости от набранных очков.
     */
    @Test
    public void levelAndSpeedChangeWithScore() {
        GameState state = createState();
        assertEquals(1, state.getLevel());
        assertEquals(1_600_000_000L, state.getTickDurationNanos());

        // Уровень 2: 1600 * 0.85 = 1360 мс
        state.addScoreForTesting(500);
        assertEquals(2, state.getLevel());
        assertEquals(1_360_000_000L, state.getTickDurationNanos());

        // Уровень 11: 5000 очков. 1600 * 0.85^10 = ~315 мс
        state.addScoreForTesting(4500);
        assertEquals(11, state.getLevel());

        // Проверяем, что скорость стала быстрее и не пробила минимум (100 мс)
        long speedAtLevel11 = state.getTickDurationNanos();
        assertTrue("Скорость должна быть меньше 500 мс", speedAtLevel11 < 500_000_000L);
        assertTrue("Скорость не должна быть меньше минимума", speedAtLevel11 >= 100_000_000L);
    }

    /**
     * Проверяет, что "призрак" фигуры (ghost) рассчитывает самую нижнюю
     * доступную позицию для текущей фигуры.
     */
    @Test
    public void ghostPyIsLowestValidPosition() {
        GameState state = createState();
        int ghostPy = state.getGhostPy();

        assertTrue(ghostPy >= state.getPy());

        while (state.getPy() < ghostPy) {
            state.tick();
        }
        assertEquals(ghostPy, state.getPy());

        state.tick();
        assertEquals(0, state.getPy());
    }

    /**
     * Проверяет, что метод предпросмотра следующей фигуры не ломает очередь спавна,
     * и следующая фигура действительно становится текущей после фиксации.
     */
    @Test
    public void nextPiecePeek() {
        GameState state = createState();
        Tetromino next = state.getNextPiece();
        assertNotNull(next);

        state.hardDrop();
        assertEquals(next, state.getCurrent());
    }

    /**
     * Проверяет, что очистка 4 линий одной фигурой (Tetris)
     * начисляет 800 очков (стандарт Guideline), плюс бонус за Hard Drop.
     */
    @Test
    public void tetris_clearsFourLinesAndScores800Points() {
        GameState state = createState();
        Board b = state.getBoard();

        // Заполняем 4 нижние строки, оставляя колонку x=9 пустой
        for (int y = Board.HEIGHT - 4; y < Board.HEIGHT; y++) {
            for (int x = 0; x < Board.WIDTH - 1; x++) {
                b.set(x, y, 1);
            }
        }

        // Принудительно ставим вертикальную I-фигуру в колонку x=9
        Tetromino verticalI = new Tetromino(2, java.util.List.of(new Point(0, 0), new Point(0, 1), new Point(0, 2), new Point(0, 3)), 4);
        state.setCurrentPieceForTesting(verticalI, 9, 0);

        // Hard Drop: фигура падает на 16 клеток вниз (+32 очка)
        // Замок: 4 линии заполнены → +800 очков
        state.hardDrop();

        // Итог: 32 + 800 = 832
        assertEquals(832, state.getScore());
        assertFalse("Игра не должна закончиться", state.isGameOver());
    }

    /**
     * Проверяет, что {@code restart()} полностью сбрасывает состояние игры:
     * очищает доску, обнуляет счет, снимает флаг {@code gameOver} и переводит
     * игру в режим ожидания старта ({@code isStarted() == false}).
     */
    @Test
    public void restart_clearsBoardAndResetsState() {
        GameState state = createState();
        // Набираем очки и фиксируем фигуру на доске
        state.hardDrop();
        state.addScoreForTesting(500);
        assertTrue("Счет должен быть больше 0 перед рестартом", state.getScore() > 0);

        state.restart();

        assertEquals("Счет должен быть обнулен", 0, state.getScore());
        assertFalse("Флаг gameOver должен быть снят", state.isGameOver());
        assertFalse("Игра должна вернуться в режим ожидания старта", state.isStarted());

        // Проверяем, что доска полностью очищена
        for (int y = 0; y < Board.HEIGHT; y++) {
            for (int x = 0; x < Board.WIDTH; x++) {
                assertEquals("Клетка (" + x + "," + y + ") должна быть пуста", 0, state.getBoard().get(x, y));
            }
        }
    }

    /**
     * Проверяет, что {@code rotateCCW()} действительно поворачивает фигуру
     * против часовой стрелки, если игра не находится на паузе.
     * <p>
     * Ключевое отличие от {@code actionsAreNoopWhilePaused}: там метод
     * вызывается на паузе (покрывается только ранний {@code return}),
     * здесь — в активной игре (покрывается вся логика поворота).
     */
    @Test
    public void rotateCCW_actuallyRotatesWhenNotPaused() {
        GameState state = createState();
        Tetromino before = state.getCurrent();

        state.rotateCCW();

        // Метод rotatedCCW() создает новый объект, поэтому ссылки должны различаться
        assertNotSame("Фигура должна быть повернута (создан новый объект)", before, state.getCurrent());
    }

    /**
     * Проверяет, что {@code rotateCW()} действительно поворачивает фигуру
     * по часовой стрелке, если игра не находится на паузе.
     */
    @Test
    public void rotateCW_actuallyRotatesWhenNotPaused() {
        GameState state = createState();
        Tetromino before = state.getCurrent();

        state.rotateCW();

        assertNotSame("Фигура должна быть повернута (создан новый объект)", before, state.getCurrent());
    }

    /**
     * Проверяет переключение флагов видимости подсказок:
     * {@code ghostEnabled} (тень фигуры) и {@code nextPieceEnabled} (превью).
     * <p>
     * Каждый флаг должен корректно инвертироваться и возвращаться
     * в исходное состояние при повторном вызове.
     */
    @Test
    public void toggles_flipGhostAndNextPieceFlags() {
        GameState state = createState();

        // Ghost
        assertTrue("По умолчанию призрак включен", state.isGhostEnabled());
        state.toggleGhost();
        assertFalse("Призрак должен быть выключен", state.isGhostEnabled());
        state.toggleGhost();
        assertTrue("Призрак должен быть снова включен", state.isGhostEnabled());

        // Next Piece
        assertTrue("По умолчанию превью включено", state.isNextPieceEnabled());
        state.toggleNextPiece();
        assertFalse("Превью должно быть выключено", state.isNextPieceEnabled());
        state.toggleNextPiece();
        assertTrue("Превью должно быть снова включено", state.isNextPieceEnabled());
    }
}
