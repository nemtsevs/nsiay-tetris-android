package nsi.example.tetris.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Неизменяемое представление тетромино (фигуры из 4 клеток).
 * <p>
 * Фигура задается списком координат {@link Point} внутри квадратного bounding box
 * размером {@code boxSize}. Это позволяет использовать единую математику поворота
 * для всех фигур (2x2 для O, 3x3 для T/S/Z/J/L, 4x4 для I).
 *
 * @param color   ID цвета (1-7), соответствующий индексу в палитре
 * @param cells   список из 4 координат клеток относительно левого верхнего угла фигуры
 * @param boxSize размер квадратной матрицы, в которую вписана фигура (2, 3 или 4)
 */
public record Tetromino(int color, List<Point> cells, int boxSize) {

    public Tetromino {
        // Защита от мутаций: создаем неизменяемую копию списка
        cells = Collections.unmodifiableList(cells);
    }

    /**
     * Возвращает новую фигуру, повернутую на 90 градусов по часовой стрелке.
     * Использует матричное преобразование: {@code (x, y) -> (boxSize - 1 - y, x)}.
     *
     * @return новая повернутая фигура
     */
    public Tetromino rotatedCW() {
        return new Tetromino(color, cells.stream()
                .map(p -> new Point(boxSize - 1 - p.y(), p.x()))
                .collect(Collectors.toList()), boxSize);
    }

    /**
     * Возвращает новую фигуру, повернутую на 90 градусов против часовой стрелки.
     * Использует матричное преобразование: {@code (x, y) -> (y, boxSize - 1 - x)}.
     *
     * @return новая повернутая фигура
     */
    public Tetromino rotatedCCW() {
        return new Tetromino(color, cells.stream()
                .map(p -> new Point(p.y(), boxSize - 1 - p.x()))
                .collect(Collectors.toList()), boxSize);
    }

    /**
     * Возвращает стандартный набор из 7 тетромино (I, O, T, S, Z, J, L) в их начальной ориентации.
     *
     * @return неизменяемый список из 7 фигур
     */
    public static List<Tetromino> all() {
        return Arrays.asList(
                new Tetromino(1, Arrays.asList(p(0, 0), p(1, 0), p(0, 1), p(1, 1)), 2), // O
                new Tetromino(2, Arrays.asList(p(0, 1), p(1, 1), p(2, 1), p(3, 1)), 4), // I
                new Tetromino(3, Arrays.asList(p(1, 0), p(0, 1), p(1, 1), p(2, 1)), 3), // T
                new Tetromino(4, Arrays.asList(p(1, 0), p(2, 0), p(0, 1), p(1, 1)), 3), // S
                new Tetromino(5, Arrays.asList(p(0, 0), p(1, 0), p(1, 1), p(2, 1)), 3), // Z
                new Tetromino(6, Arrays.asList(p(0, 0), p(0, 1), p(1, 1), p(2, 1)), 3), // J
                new Tetromino(7, Arrays.asList(p(2, 0), p(0, 1), p(1, 1), p(2, 1)), 3)  // L
        );
    }

    private static Point p(int x, int y) {
        return new Point(x, y);
    }
}
