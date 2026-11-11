package nsi.example.tetris.core;

import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Unit-тесты для класса {@link Tetromino}.
 * <p>
 * Проверяют математическую корректность поворотов и структуру фигур.
 */
public class TetrominoTest {

    /**
     * Проверяет, что в наборе ровно 7 фигур и каждая состоит из 4 клеток.
     */
    @Test
    public void allPiecesHaveFourCells() {
        List<Tetromino> pieces = Tetromino.all();
        assertEquals(7, pieces.size());
        for (Tetromino t : pieces) {
            assertEquals(4, t.cells().size());
        }
    }

    /**
     * Проверяет, что поворот по часовой стрелке (CW) правильно меняет координаты T-фигуры.
     */
    @Test
    public void rotateCWRotatesTPieceCorrectly() {
        // Исходная T-фигура:
        //  . X .
        //  X X X
        Tetromino t = new Tetromino(1, List.of(
                new Point(1, 0), new Point(0, 1), new Point(1, 1), new Point(2, 1)
        ), 3);

        // Ожидаемая T-фигура после поворота CW:
        //  . X .
        //  . X X
        //  . X .
        Set<Point> expected = Set.of(
                new Point(2, 1), new Point(1, 0), new Point(1, 1), new Point(1, 2)
        );
        assertEquals(expected, Set.copyOf(t.rotatedCW().cells()));
    }

    /**
     * Проверяет, что поворот CW и последующий поворот CCW возвращают фигуру
     * в исходное состояние (они взаимно обратны).
     */
    @Test
    public void rotationsAreInversePair() {
        for (Tetromino t : Tetromino.all()) {
            assertEquals(Set.copyOf(t.cells()), Set.copyOf(t.rotatedCW().rotatedCCW().cells()));
        }
    }

    /**
     * Проверяет, что при повороте не теряются и не дублируются клетки
     * (фигура всегда состоит из 4 клеток).
     */
    @Test
    public void rotationPreservesCellCount() {
        for (Tetromino t : Tetromino.all()) {
            assertEquals(4, t.rotatedCW().cells().size());
        }
    }
}
