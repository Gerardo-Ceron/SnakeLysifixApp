package com.example.snakelysifix

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.random.Random

// --- MODELOS ---
enum class Direction { UP, DOWN, LEFT, RIGHT }
data class Point(val x: Int, val y: Int)
data class GameRecord(val score: Int, val date: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                SnakeGameScreen()
            }
        }
    }
}

@Composable
fun SnakeGameScreen() {
    val context = LocalContext.current
    val gridSize = 20

    // Estado del juego
    var snake by remember { mutableStateOf(listOf(Point(5, 10), Point(5, 11), Point(5, 12))) }
    var food by remember { mutableStateOf(Point(10, 10)) }
    var direction by remember { mutableStateOf(Direction.UP) }
    var nextDirection by remember { mutableStateOf(Direction.UP) }
    var score by remember { mutableIntStateOf(0) }
    var isGameOver by remember { mutableStateOf(false) }
    var history by remember { mutableStateOf(getGameHistory(context)) }

    // Bucle del juego
    LaunchedEffect(isGameOver) {
        if (!isGameOver) {
            while (true) {
                delay(150.milliseconds)

                // Evitar giros de 180 grados
                val currentDir = direction
                direction = if (
                    (nextDirection == Direction.UP && currentDir != Direction.DOWN) ||
                    (nextDirection == Direction.DOWN && currentDir != Direction.UP) ||
                    (nextDirection == Direction.LEFT && currentDir != Direction.RIGHT) ||
                    (nextDirection == Direction.RIGHT && currentDir != Direction.LEFT)
                ) {
                    nextDirection
                } else {
                    currentDir
                }

                val head = snake.first()
                val newHead = when (direction) {
                    Direction.UP -> Point(head.x, head.y - 1)
                    Direction.DOWN -> Point(head.x, head.y + 1)
                    Direction.LEFT -> Point(head.x - 1, head.y)
                    Direction.RIGHT -> Point(head.x + 1, head.y)
                }
                
                // Verificar colisiones
                val isCollision = newHead.x !in 0 until gridSize || 
                                 newHead.y !in 0 until gridSize || 
                                 snake.dropLast(1).contains(newHead)

                if (isCollision) {
                    isGameOver = true
                    saveGame(context, score)
                    history = getGameHistory(context)
                    break
                }

                if (newHead == food) {
                    score += 10
                    snake = listOf(newHead) + snake
                    // Nueva comida que no esté sobre la serpiente
                    var newFood: Point
                    do {
                        newFood = Point(Random.nextInt(gridSize), Random.nextInt(gridSize))
                    } while (snake.contains(newFood))
                    food = newFood
                } else {
                    snake = listOf(newHead) + snake.dropLast(1)
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black).padding(16.dp)) {
        // MUCHO espacio superior para bajar Lysifix
        Spacer(Modifier.height(60.dp))

        // Marca Lysifix centrada
        Text(
            text = "Lysifix",
            color = Color(0xFFFFE082),
            modifier = Modifier.fillMaxWidth(),
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        // Espacio entre Lysifix y el Tablero
        Spacer(Modifier.height(24.dp))

        // Tablero (ocupa el espacio central)
        Box(modifier = Modifier
            .weight(1f)
            .aspectRatio(1f)
            .align(Alignment.CenterHorizontally)
            .background(Color(0xFF0A0A0A), RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val (x, y) = dragAmount
                    if (abs(x) > abs(y)) {
                        if (x > 10) nextDirection = Direction.RIGHT
                        else if (x < -10) nextDirection = Direction.LEFT
                    } else {
                        if (y > 10) nextDirection = Direction.DOWN
                        else if (y < -10) nextDirection = Direction.UP
                    }
                }
            }) {

            Canvas(modifier = Modifier.fillMaxSize()) {
                val cellSize = size.width / gridSize
                // Dibujar Comida
                drawRect(Color.Red, Offset(food.x * cellSize, food.y * cellSize), Size(cellSize - 1f, cellSize - 1f))
                // Dibujar Serpiente
                snake.forEachIndexed { index, pt ->
                    val color = if (index == 0) Color.Green else Color(0xFF2E7D32)
                    drawRect(color, Offset(pt.x * cellSize, pt.y * cellSize), Size(cellSize - 1f, cellSize - 1f))
                }
            }

            if (isGameOver) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("¡CHISPAS!", color = Color.Red, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        Text("Inténtalo de nuevo", color = Color.White, fontSize = 18.sp)
                        Spacer(Modifier.height(20.dp))
                        Button(onClick = {
                            snake = listOf(Point(5, 10), Point(5, 11), Point(5, 12))
                            score = 0
                            direction = Direction.UP
                            nextDirection = Direction.UP
                            isGameOver = false
                        }, colors = ButtonDefaults.buttonColors(containerColor = Color.Green)) {
                            Text("REINTENTAR", color = Color.Black)
                        }
                    }
                }
            }
        }

        // Espacio entre Tablero y SNAKE/Score
        Spacer(Modifier.height(24.dp))

        // SNAKE y Score centrados
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("SNAKE ", color = Color.Green, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("- ", color = Color.Gray, fontSize = 18.sp)
            Text("Score: $score", color = Color.Yellow, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        // Espacio mayor para subir el historial
        Spacer(Modifier.height(30.dp))

        // Historial
        Text("Historial de Partidas", color = Color.Green, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.fillMaxWidth().height(140.dp)) {
            items(history) { record ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(record.date, color = Color.Gray, fontSize = 12.sp)
                    Text("${record.score} pts", color = Color.White)
                }
            }
        }
        
        // MUCHO margen inferior para alejarlo del borde
        Spacer(Modifier.height(60.dp))
    }
}

// --- PERSISTENCIA ---
fun saveGame(context: Context, score: Int) {
    val sharedPref = context.getSharedPreferences("snake_prefs", Context.MODE_PRIVATE)
    val historyString = sharedPref.getString("history", "") ?: ""
    val date = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date())

    val historyList = historyString.split(";").filter { it.isNotBlank() }.toMutableList()
    historyList.add(0, "$score|$date")

    sharedPref.edit {
        putString("history", historyList.take(10).joinToString(";"))
    }
}

fun getGameHistory(context: Context): List<GameRecord> {
    val historyString = context.getSharedPreferences("snake_prefs", Context.MODE_PRIVATE).getString("history", "") ?: ""
    if (historyString.isBlank()) return emptyList()

    return historyString.split(";").mapNotNull { entry ->
        val parts = entry.split("|")
        if (parts.size == 2) {
            val s = parts[0].toIntOrNull() ?: 0
            GameRecord(s, parts[1])
        } else null
    }
}
