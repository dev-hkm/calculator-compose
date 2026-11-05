package com.devhkm.calculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devhkm.calculator.logic.Evaluator
import com.devhkm.calculator.ui.theme.CalculatorTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalculatorTheme { CalculatorScreen() }
        }
    }
}

// ---------------- Kiểu phím ----------------

private enum class KeyKind { Number, Operator, Action, Equals }

private data class Key(
    val label: String,
    val kind: KeyKind,
    val insert: String? = null,
    val icon: (@Composable () -> Unit)? = null
)

private val KEYS = listOf(
    listOf(
        Key("AC", KeyKind.Action),
        Key("( )", KeyKind.Action),
        Key("%", KeyKind.Action, "%"),
        Key("÷", KeyKind.Operator, "÷")
    ),
    listOf(
        Key("7", KeyKind.Number, "7"),
        Key("8", KeyKind.Number, "8"),
        Key("9", KeyKind.Number, "9"),
        Key("×", KeyKind.Operator, "×")
    ),
    listOf(
        Key("4", KeyKind.Number, "4"),
        Key("5", KeyKind.Number, "5"),
        Key("6", KeyKind.Number, "6"),
        Key("−", KeyKind.Operator, "−")
    ),
    listOf(
        Key("1", KeyKind.Number, "1"),
        Key("2", KeyKind.Number, "2"),
        Key("3", KeyKind.Number, "3"),
        Key("+", KeyKind.Operator, "+")
    ),
    listOf(
        Key("√", KeyKind.Action, "√"),
        Key("0", KeyKind.Number, "0"),
        Key(",", KeyKind.Number, ","),
        Key("=", KeyKind.Equals)
    )
)

// ---------------- Màn hình chính ----------------

@Composable
fun CalculatorScreen() {
    var expression by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("0") }
    var justEvaluated by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showHistory by remember { mutableStateOf(false) }
    val history = remember { mutableStateListOf<Pair<String, String>>() }

    fun recalc() {
        val r = Evaluator.evaluate(expression.replace(",", "."))
        when (r) {
            is Evaluator.Result.Ok -> { result = r.value; errorMsg = null }
            is Evaluator.Result.Err -> { errorMsg = r.message; result = "" }
        }
    }

    fun onKey(k: Key) {
        errorMsg = null
        when (k.kind) {
            KeyKind.Number, KeyKind.Operator -> {
                if (justEvaluated) {
                    // nếu vừa có kết quả mà bấm số => bắt đầu biểu thức mới
                    expression = if (k.kind == KeyKind.Number) k.insert!! else result.replace(".", "") + k.insert!!
                    justEvaluated = false
                } else {
                    expression += k.insert!!
                }
                recalc()
            }
            KeyKind.Action -> when (k.label) {
                "AC" -> { expression = ""; result = "0"; errorMsg = null; justEvaluated = false }
                "( )" -> {
                    val open = expression.count { it == '(' }
                    val close = expression.count { it == ')' }
                    expression += if (open > close) ")" else "("
                    recalc()
                }
                "√" -> { expression += "√(" ; recalc() }
                "%" -> { expression += "%" ; recalc() }
            }
            KeyKind.Equals -> {
                if (expression.isNotBlank()) {
                    val r = Evaluator.evaluate(expression.replace(",", "."))
                    if (r is Evaluator.Result.Ok) {
                        history.add(0, expression to r.value)
                        result = r.value
                        expression = r.value.replace(".", ",")
                        justEvaluated = true
                        errorMsg = null
                    } else if (r is Evaluator.Result.Err) {
                        errorMsg = r.message
                    }
                }
            }
        }
    }

    val bg = MaterialTheme.colorScheme.background
    val primaryColor = MaterialTheme.colorScheme.primary
    val gradient = remember(bg, primaryColor) {
        Brush.verticalGradient(
            colors = listOf(bg, bg.copy(alpha = 0.97f), primaryColor.copy(alpha = 0.06f))
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (dragAmount < -60f) showHistory = true
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(36.dp))

            // ---- Header ----
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Calculate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.size(10.dp))
                    Text(
                        "Máy tính",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { showHistory = true }) {
                    Icon(
                        Icons.Rounded.History,
                        contentDescription = "Lịch sử",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // ---- Vùng hiển thị ----
            Text(
                text = if (expression.isBlank()) " " else expression,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(12.dp))

            AnimatedContent(
                targetState = errorMsg ?: result,
                transitionSpec = {
                    (slideInVertically(tween(180)) { it / 2 } + fadeIn(tween(180)))
                        .togetherWith(slideOutVertically(tween(160)) { -it / 2 } + fadeOut(tween(160)))
                },
                label = "result"
            ) { value ->
                Text(
                    text = value.ifBlank { "0" },
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = if (value.length > 12) 40.sp else 64.sp
                    ),
                    color = if (errorMsg != null) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(24.dp))

            // ---- Bàn phím ----
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                KEYS.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        row.forEach { key ->
                            KeyButton(
                                key = key,
                                modifier = Modifier.weight(1f),
                                onClick = { onKey(key) },
                                onLongClick = { expression = ""; result = "0"; errorMsg = null }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
        }
    }

    if (showHistory) {
        HistorySheet(
            items = history,
            onDismiss = { showHistory = false },
            onClear = { history.clear() },
            onPick = { expr ->
                expression = expr
                justEvaluated = false
                showHistory = false
                val r = Evaluator.evaluate(expr.replace(",", "."))
                if (r is Evaluator.Result.Ok) { result = r.value; errorMsg = null }
            }
        )
    }
}

// ---------------- Nút bấm ----------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KeyButton(
    key: Key,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val container = when (key.kind) {
        KeyKind.Number -> scheme.surface
        KeyKind.Operator -> scheme.primaryContainer
        KeyKind.Action -> scheme.surfaceVariant
        KeyKind.Equals -> Color.Transparent
    }
    val contentColor = when (key.kind) {
        KeyKind.Number -> scheme.onSurface
        KeyKind.Operator -> scheme.primary
        KeyKind.Action -> scheme.onSurfaceVariant
        KeyKind.Equals -> scheme.onPrimary
    }

    Box(
        modifier = modifier
            .height(68.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(if (key.kind == KeyKind.Equals) Color.Transparent else container)
            .then(
                if (key.kind == KeyKind.Equals) Modifier.background(
                    Brush.linearGradient(listOf(scheme.primary, scheme.secondary))
                ) else Modifier
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = key.label,
            color = contentColor,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = if (key.kind == KeyKind.Number || key.kind == KeyKind.Operator) 26.sp else 20.sp
            ),
            textAlign = TextAlign.Center
        )
    }
}

// ---------------- Bottom sheet lịch sử ----------------

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun HistorySheet(
    items: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
    onPick: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Lịch sử",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.weight(1f))
                if (items.isNotEmpty()) {
                    TextButton(onClick = onClear) { Text("Xoá hết") }
                }
                IconButton(onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                }) {
                    Icon(Icons.Rounded.Close, contentDescription = "Đóng")
                }
            }

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Chưa có phép tính nào",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.height(360.dp)) {
                    items(items) { (expr, res) ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .padding(vertical = 12.dp, horizontal = 8.dp)
                        ) {
                            Text(
                                expr,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                res,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.headlineMedium,
                                fontSize = 24.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun CalcPreview() {
    CalculatorTheme { CalculatorScreen() }
}
