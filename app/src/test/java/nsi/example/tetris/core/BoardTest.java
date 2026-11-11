package nsi.example.tetris.core;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit-тесты для класса {@link Board}.
 * <p>
 * Проверяют логику игрового поля: очистку линий, коллизии со стенами
 * и корректность установки/получения значений ячеек.
 */
public class BoardTest {

    /**
     * Проверяет, что при заполнении линии она удаляется, а все блоки выше сдвигаются вниз.
     */
    @Test
    public void clearsFullLineAndShiftsAboveDown() {
        Board b = new Board();
        // Заполняем нижнюю строку
        for (int x = 0; x < Board.WIDTH; x++) b.set(x, Board.HEIGHT - 1, 1);
        // Ставим маркерный блок на одну строку выше
        b.set(0, Board.HEIGHT - 2, 2);

        assertEquals(1, b.clearFullLines());

        // Маркер должен упасть на самое дно
        assertEquals(2, b.get(0, Board.HEIGHT - 1));
        // Верхняя часть доски должна быть пуста
        assertEquals(0, b.get(0, 0));
    }

    /**
     * Проверяет, что выход за границы поля считается занятой клеткой.
     * Это критически важно для работы проверки коллизий со стенами.
     */
    @Test
    public void isOccupiedOutOfBoundsReturnsTrue() {
        Board b = new Board();
        assertTrue(b.isOccupied(-1, 0));
        assertTrue(b.isOccupied(0, -1));
        assertTrue(b.isOccupied(Board.WIDTH, 0));
        assertTrue(b.isOccupied(0, Board.HEIGHT));
    }

    /**
     * Проверяет базовую работу сеттера и геттера ячеек.
     */
    @Test
    public void setThenGetReturnsSameValue() {
        Board b = new Board();
        b.set(3, 4, 5);
        assertEquals(5, b.get(3, 4));
    }

    /**
     * Проверяет, что очистка пустой доски возвращает 0 (нет удаленных линий).
     */
    @Test
    public void clearFullLinesOnEmptyBoardReturnsZero() {
        assertEquals(0, new Board().clearFullLines());
    }

    /**
     * Проверяет корректную очистку сразу нескольких заполненных линий за один ход.
     * После очистки доска должна стать полностью пустой.
     */
    @Test
    public void clearFullLinesMultipleLines() {
        Board b = new Board();
        for (int x = 0; x < Board.WIDTH; x++) {
            b.set(x, Board.HEIGHT - 1, 1);
            b.set(x, Board.HEIGHT - 2, 1);
        }
        assertEquals(2, b.clearFullLines());

        for (int y = 0; y < Board.HEIGHT; y++)
            for (int x = 0; x < Board.WIDTH; x++)
                assertEquals(0, b.get(x, y));
    }
}
