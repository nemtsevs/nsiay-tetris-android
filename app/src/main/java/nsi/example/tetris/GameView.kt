package nsi.example.tetris

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import nsi.example.tetris.config.GameConfig
import nsi.example.tetris.config.ScoreManager
import nsi.example.tetris.core.GameState

/**
 * Кастомный View для отрисовки игры и обработки касаний.
 * <p>
 * Наследуется от [SurfaceView] и использует фоновый поток для обеспечения
 * плавной анимации (отрисовки) без блокировки UI-потока.
 * Реализует [SurfaceHolder.Callback] для отслеживания жизненного цикла поверхности
 * и [Runnable] для игрового цикла.
 */
class GameView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback, Runnable {

    private var gameThread: Thread? = null

    @Volatile
    private var isRunning = false
    private var game = GameState()
    private var lastTick = System.nanoTime()

    private val scoreManager = ScoreManager(context)
    private var highScore = scoreManager.getHighScore()

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    // Динамические размеры, зависящие от разрешения экрана
    private var cellSize = 40f
    private var xOffset = 0f
    private var yOffset = 0f
    private val topPanelHeight = 250f

    // Переменные для обработки свайпов
    private var touchStartX = 0f
    private var touchStartY = 0f
    private var lastMoveX = 0f
    private var isDragging = false

    private var backgroundPaint = Paint().apply {
        shader = LinearGradient(
            0f, 0f, 0f, 2000f, GameConfig.bgColors, GameConfig.bgPositions, Shader.TileMode.CLAMP
        )
    }

    init {
        holder.addCallback(this)
    }

    /**
     * Вызывается при изменении размера поверхности (например, при повороте экрана).
     * Пересчитывает размер клетки и отступы, чтобы игровое поле всегда помещалось на экране.
     */
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // Делаем поле на всю ширину экрана (без пустых полос)
        cellSize = width.toFloat() / 10f
        xOffset = 0f

        // Центрируем по высоте, оставляя место сверху под счет
        val boardHeight = cellSize * 20f
        yOffset = topPanelHeight + (height - topPanelHeight - boardHeight) / 2f
        if (yOffset < topPanelHeight) yOffset = topPanelHeight

        // Обновляем градиент под актуальную высоту экрана
        backgroundPaint.shader = LinearGradient(
            0f,
            0f,
            0f,
            height.toFloat(),
            GameConfig.bgColors,
            GameConfig.bgPositions,
            Shader.TileMode.CLAMP
        )
    }

    /**
     * Обрабатывает касания экрана.
     * <p>
     * Действия:
     * - Свайп влево/вправо двигает фигуру.
     * - Свайп вниз (от 2 клеток) делает Hard Drop.
     * - Обычный тап (без движения) поворачивает фигуру.
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Старт игры из меню
        if (!game.isStarted) {
            if (event.action == MotionEvent.ACTION_UP) {
                game.startGame()
                lastTick = System.nanoTime()  // ← сброс таймера
            }
            return true
        }

        // Рестарт после Game Over
        if (game.isGameOver) {
            if (event.action == MotionEvent.ACTION_UP) {
                game.restart()
                game.startGame()
                lastTick = System.nanoTime()  // ← сброс таймера
            }
            return true
        }

        // Игровое управление (свайпы)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStartX = event.x
                touchStartY = event.y
                lastMoveX = event.x
                isDragging = false
            }

            MotionEvent.ACTION_MOVE -> {
                val diffX = event.x - lastMoveX
                // Двигаем фигуру, как только палец сместился на 80% ширины клетки
                if (Math.abs(diffX) > cellSize * 0.8f) {
                    if (diffX > 0) game.moveRight() else game.moveLeft()
                    lastMoveX = event.x
                    isDragging = true // Запоминаем, что это был свайп, а не тап
                }
            }

            MotionEvent.ACTION_UP -> {
                val totalDiffX = event.x - touchStartX
                val totalDiffY = event.y - touchStartY

                // Если палец не двигался по горизонтали (это был свайп вниз или тап)
                if (!isDragging) {
                    if (totalDiffY > cellSize * 2 && Math.abs(totalDiffY) > Math.abs(totalDiffX)) {
                        game.hardDrop() // Свайп вниз
                    } else if (Math.abs(totalDiffX) < 30 && Math.abs(totalDiffY) < 30) {
                        game.rotateCW() // Легкий тап
                    }
                }
            }
        }
        return true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        isRunning = true
        gameThread = Thread(this)
        gameThread?.start()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isRunning = false
        gameThread?.join()
    }

    /**
     * Игровой цикл (выполняется в фоновом потоке).
     * Вызывает логику игры [GameState.tick] через определенные промежутки времени
     * и перерисовывает экран на каждом кадре.
     */
    override fun run() {
        while (isRunning) {
            val now = System.nanoTime()
            if (game.isStarted && !game.isGameOver) {
                if (now - lastTick >= game.tickDurationNanos) {
                    game.tick()
                    lastTick = now
                }
            }

            if (game.isGameOver) {
                if (scoreManager.saveHighScore(game.score)) {
                    highScore = game.score
                }
            }
            draw()
        }
    }

    private fun draw() {
        val canvas: Canvas = holder.lockCanvas() ?: return
        try {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

            if (!game.isStarted) {
                drawStartMenu(canvas)
                return
            }

            drawGrid(canvas)
            drawBoard(canvas)
            drawGhost(canvas)
            drawCurrentPiece(canvas)
            drawTopPanel(canvas)

            if (game.isGameOver) drawGameOver(canvas)
        } finally {
            holder.unlockCanvasAndPost(canvas)
        }
    }

    /**
     * Рисует стартовое меню с названием игры, рекордом и инструкцией по управлению.
     */
    private fun drawStartMenu(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        paint.textAlign = Paint.Align.CENTER

        paint.color = Color.parseColor("#F9D982")
        paint.textSize = 90f
        canvas.drawText("NSIAY TETRIS", width / 2f, 250f, paint)

        paint.color = Color.WHITE
        paint.textSize = 50f
        canvas.drawText("BEST SCORE: $highScore", width / 2f, 350f, paint)

        paint.color = Color.parseColor("#E0E0E0")
        paint.textSize = 45f
        val controls = arrayOf(
            "Swipe Left / Right : Move", "Swipe Down : Hard Drop", "Tap : Rotate"
        )
        var cy = 480f
        for (line in controls) {
            canvas.drawText(line, width / 2f, cy, paint)
            cy += 70f
        }

        paint.color = Color.parseColor("#9CCDCD")
        paint.textSize = 60f
        canvas.drawText("Tap to Start", width / 2f, cy + 100f, paint)
    }

    /**
     * Рисует верхнюю панель: текущий уровень, счет, рекорд и превью следующей фигуры.
     */
    private fun drawTopPanel(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        paint.textAlign = Paint.Align.LEFT

        paint.color = Color.WHITE
        paint.textSize = 40f
        canvas.drawText("LEVEL ${game.level}", 20f, 60f, paint)

        paint.textSize = 60f
        canvas.drawText("SCORE: ${game.score}", 20f, 130f, paint)

        paint.color = Color.parseColor("#F9D982")
        paint.textSize = 40f
        canvas.drawText("BEST: $highScore", 20f, 190f, paint)

        // Отрисовка превью следующей фигуры (справа)
        val nextPiece = game.nextPiece
        if (nextPiece != null) {
            val previewCell = 35f
            val startX = width - 180f
            val startY = 50f

            paint.color = Color.WHITE
            paint.textSize = 30f
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("NEXT", width - 20f, 60f, paint)

            for (point in nextPiece.cells()) {
                val px = startX + (point.x() * previewCell)
                val py = startY + (point.y() * previewCell)
                rect.set(px, py, px + previewCell - 2, py + previewCell - 2)
                paint.color = GameConfig.pieceColors[nextPiece.color()]
                canvas.drawRoundRect(rect, 4f, 4f, paint)
            }
        }
    }

    private fun drawGrid(canvas: Canvas) {
        paint.style = Paint.Style.STROKE

        // Делаем линию в 2 раза тоньше (0.5 пикселя)
        paint.strokeWidth = 0.5f

        // Уменьшаем прозрачность (альфа-канал). #11 (около 6% белого)
        paint.color = Color.parseColor("#11FFFFFF")

        for (x in 0..10) {
            val px = x * cellSize + xOffset
            canvas.drawLine(px, yOffset, px, yOffset + 20 * cellSize, paint)
        }
        for (y in 0..20) {
            val py = y * cellSize + yOffset
            canvas.drawLine(xOffset, py, xOffset + 10 * cellSize, py, paint)
        }
    }

    private fun drawBoard(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        for (y in 0 until 20) {
            for (x in 0 until 10) {
                val colorId = game.board.get(x, y)
                if (colorId != 0) {
                    drawBlock(
                        canvas,
                        x * cellSize + xOffset,
                        y * cellSize + yOffset,
                        GameConfig.pieceColors[colorId]
                    )
                }
            }
        }
    }

    /**
     * Рисует "призрак" фигуры — полупрозрачный силуэт, показывающий, куда упадет фигура.
     */
    private fun drawGhost(canvas: Canvas) {
        if (!game.isGhostEnabled) return
        val current = game.current ?: return
        val ghostY = game.ghostPy

        if (ghostY > game.py) {
            val pieceColor = GameConfig.pieceColors[current.color()]

            // Берем оригинальный цвет фигуры и делаем его прозрачным (альфа 80 из 255).
            val ghostColor = Color.argb(
                80, Color.red(pieceColor), Color.green(pieceColor), Color.blue(pieceColor)
            )

            paint.style = Paint.Style.FILL
            paint.color = ghostColor

            for (point in current.cells()) {
                val px = (game.px + point.x()) * cellSize + xOffset
                val py = (ghostY + point.y()) * cellSize + yOffset

                // Добавляем +1.5f. Это заставляет прозрачные кубики
                // накладываться друг на друга, перекрывая сетку внутри фигуры.
                rect.set(px, py, px + cellSize + 1.5f, py + cellSize + 1.5f)
                canvas.drawRect(rect, paint)
            }
        }
    }

    private fun drawCurrentPiece(canvas: Canvas) {
        val current = game.current ?: return
        for (point in current.cells()) {
            val px = (game.px + point.x()) * cellSize + xOffset
            val py = (game.py + point.y()) * cellSize + yOffset
            drawBlock(canvas, px, py, GameConfig.pieceColors[current.color()])
        }
    }

    /**
     * Рисует один блок (кубик) с заливкой и обводкой.
     * Заливка рисуется с небольшим запасом, чтобы перекрыть сетку на стыках.
     */
    private fun drawBlock(canvas: Canvas, px: Float, py: Float, color: Int) {
        // 1. Заливка (с запасом в 1 пиксель для перекрытия сетки)
        paint.style = Paint.Style.FILL
        paint.color = color
        rect.set(px, py, px + cellSize + 1f, py + cellSize + 1f)
        canvas.drawRect(rect, paint)

        // 2. Обводка (рисуем внутри, делаем цвет темнее на 30%)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = Color.argb(
            255,
            (Color.red(color) * 0.7).toInt(),
            (Color.green(color) * 0.7).toInt(),
            (Color.blue(color) * 0.7).toInt()
        )

        // Отступаем 1 пиксель от краев
        rect.set(px + 1f, py + 1f, px + cellSize - 1f, py + cellSize - 1f)
        canvas.drawRoundRect(rect, 3f, 3f, paint)
    }

    /**
     * Рисует экран окончания игры (Game Over) с предложением рестарта.
     */
    private fun drawGameOver(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#CC000000")
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 100f
        canvas.drawText("GAME OVER", width / 2f, height / 2f - 50f, paint)

        paint.color = Color.parseColor("#F9D982")
        paint.textSize = 60f
        canvas.drawText("Tap to Restart", width / 2f, height / 2f + 80f, paint)
    }
}
