package nsi.example.tetris.core;

import java.util.Arrays;

/**
 * Игровое поле Tetris размером 10x20.
 * <p>
 * Хранит только <b>зафиксированные</b> (упавшие) блоки. Информация о текущей
 * падающей фигуре хранится в {@link GameState}.
 * <p>
 * Координаты: {@code (0,0)} — левый верхний угол, {@code (9,19)} — правый нижний.
 * Каждая ячейка хранит ID цвета (0 означает пустоту).
 */
public final class Board {
    public static final int WIDTH = 10;
    public static final int HEIGHT = 20;

    private final int[][] cells = new int[HEIGHT][WIDTH];

    /**
     * Проверяет, занята ли указанная клетка.
     * <p>
     * <b>Важно:</b> выход за границы поля считается занятым (возвращает {@code true}).
     * Это намеренное решение: благодаря ему проверка коллизий со стенами
     * работает автоматически, без дополнительных условий в {@code canPlace()}.
     *
     * @param x колонка (0..9)
     * @param y строка (0..19)
     * @return {@code true}, если клетка занята блоком или находится за пределами поля
     */
    public boolean isOccupied(int x, int y) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) return true;
        return cells[y][x] != 0;
    }

    /**
     * Возвращает ID цвета блока в указанной клетке.
     *
     * @param x колонка
     * @param y строка
     * @return ID цвета (0, если клетка пуста)
     */
    public int get(int x, int y) {
        return cells[y][x];
    }

    /**
     * Устанавливает цвет блока в указанной клетке (фиксация фигуры).
     *
     * @param x     колонка
     * @param y     строка
     * @param color ID цвета фигуры
     */
    public void set(int x, int y, int color) {
        cells[y][x] = color;
    }

    /**
     * Находит и удаляет все заполненные горизонтальные линии, сдвигая блоки выше вниз.
     * <p>
     * Алгоритм проходит снизу вверх. Если строка полная, она сдвигается вниз,
     * а счетчик {@code y} увеличивается на 1, чтобы повторно проверить эту же
     * позицию (так как в нее уже сдвинулась строка сверху).
     *
     * @return количество очищенных линий (от 0 до 4)
     */
    public int clearFullLines() {
        int cleared = 0;
        for (int y = HEIGHT - 1; y >= 0; y--) {
            if (isFull(y)) {
                shiftDownFrom(y);
                y++; // Перепроверить эту же строку (в нее сдвинулась верхняя)
                cleared++;
            }
        }
        return cleared;
    }

    private boolean isFull(int y) {
        for (int x = 0; x < WIDTH; x++) if (cells[y][x] == 0) return false;
        return true;
    }

    private void shiftDownFrom(int y) {
        for (int row = y; row > 0; row--) {
            System.arraycopy(cells[row - 1], 0, cells[row], 0, WIDTH);
        }
        Arrays.fill(cells[0], 0);
    }
}
