package com.houseroyale.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Background = Color(0xFF080B19)
private val Panel = Color(0xFF151A32)
private val NeonPurple = Color(0xFFB34DFF)
private val NeonBlue = Color(0xFF35D9FF)
private val NeonGold = Color(0xFFFFD166)
private val NeonGreen = Color(0xFF45F5A5)
private val SoftWhite = Color(0xFFF3F4FF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                HousieRoyaleApp()
            }
        }
    }
}

private enum class AppScreen {
    HOME, ONLINE, GAME
}

@Composable
fun HousieRoyaleApp() {
    var screen by remember { mutableStateOf(AppScreen.HOME) }
    var playerName by remember { mutableStateOf("") }
    var gameCode by remember { mutableStateOf("") }
    var bundleSize by remember { mutableIntStateOf(1) }

    var tickets by remember {
        mutableStateOf(TambolaGameEngine.generateTickets(1))
    }
    var selectedTicketIndex by remember { mutableIntStateOf(0) }
    var favouriteTickets by remember { mutableStateOf(setOf<Int>()) }
    val calledNumbers = remember { mutableStateListOf<Int>() }

    val currentTicket = tickets.getOrNull(selectedTicketIndex)
        ?: tickets.first()

    val completedPatterns = remember(currentTicket, calledNumbers.size) {
        TambolaGameEngine.completedPatterns(currentTicket, calledNumbers.toSet())
    }

    fun startNewGame(size: Int = bundleSize) {
        bundleSize = size
        tickets = TambolaGameEngine.generateTickets(size)
        selectedTicketIndex = 0
        favouriteTickets = emptySet()
        calledNumbers.clear()
        screen = AppScreen.GAME
    }

    fun toggleFavourite() {
        favouriteTickets =
            if (selectedTicketIndex in favouriteTickets) {
                favouriteTickets - selectedTicketIndex
            } else {
                favouriteTickets + selectedTicketIndex
            }
    }

    fun openNextFavourite() {
        val next = favouriteTickets
            .filter { it > selectedTicketIndex }
            .minOrNull() ?: favouriteTickets.minOrNull()

        if (next != null) selectedTicketIndex = next
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF11152D), Background, Color(0xFF160B29))
                )
            )
    ) {
        when (screen) {
            AppScreen.HOME -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("👑", fontSize = 65.sp)
                    Text(
                        "HOUSIE ROYALE",
                        color = NeonGold,
                        fontSize = 29.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "VERSION 2.2",
                        color = NeonBlue,
                        fontSize = 13.sp,
                        letterSpacing = 2.sp
                    )
                    Spacer(Modifier.height(30.dp))

                    NeonCard {
                        Text(
                            "WELCOME, PLAYER!",
                            color = NeonGold,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(18.dp))
                        OutlinedTextField(
                            value = playerName,
                            onValueChange = { playerName = it.take(20) },
                            label = { Text("Enter your name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = neonFieldColors()
                        )
                        Spacer(Modifier.height(22.dp))
                        Text(
                            "CHOOSE YOUR TICKET BUNDLE",
                            color = NeonBlue,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(1, 6, 12).forEach { size ->
                                Button(
                                    onClick = { bundleSize = size },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (bundleSize == size) {
                                            NeonPurple
                                        } else {
                                            Color(0xFF252C4A)
                                        },
                                        contentColor = SoftWhite
                                    )
                                ) {
                                    Text("${size}x")
                                }
                            }
                        }
                        Spacer(Modifier.height(22.dp))
                        NeonButton(
                            text = "🎮  PLAY OFFLINE",
                            color = NeonPurple,
                            enabled = playerName.isNotBlank()
                        ) {
                            startNewGame(bundleSize)
                        }
                        Spacer(Modifier.height(12.dp))
                        NeonButton(
                            text = "🌐  PLAY ONLINE",
                            color = NeonBlue,
                            enabled = playerName.isNotBlank()
                        ) {
                            screen = AppScreen.ONLINE
                        }
                    }
                    Spacer(Modifier.height(25.dp))
                    Text(
                        "✨ PLAY  •  WIN  •  CELEBRATE ✨",
                        color = NeonGold,
                        fontSize = 12.sp,
                        letterSpacing = 2.sp
                    )
                }
            }

            AppScreen.ONLINE -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("🌐", fontSize = 55.sp)
                    Text(
                        "ONLINE ROYALE",
                        color = NeonBlue,
                        fontSize = 27.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.height(25.dp))
                    NeonCard {
                        Text(
                            "CREATE OR JOIN A GAME",
                            color = NeonGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(Modifier.height(15.dp))
                        Text(
                            "Online multiplayer is not connected yet. " +
                                "This screen is currently a placeholder.",
                            color = SoftWhite,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(18.dp))
                        OutlinedTextField(
                            value = gameCode,
                            onValueChange = {
                                gameCode = it.filter(Char::isDigit).take(6)
                            },
                            label = { Text("Enter 6-digit game code") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = neonFieldColors()
                        )
                        Spacer(Modifier.height(20.dp))
                        NeonButton(
                            text = "← BACK TO HOME",
                            color = NeonPurple
                        ) {
                            screen = AppScreen.HOME
                        }
                    }
                }
            }

            AppScreen.GAME -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "👑 ROYALE GAME",
                            color = NeonGold,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        )
                        TextButton(onClick = { screen = AppScreen.HOME }) {
                            Text("EXIT", color = NeonBlue)
                        }
                    }

                    Text("Player: $playerName", color = SoftWhite, fontSize = 14.sp)
                    Spacer(Modifier.height(15.dp))

                    NeonCard {
                        Text(
                            "NUMBER CALLER",
                            color = NeonBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Spacer(Modifier.height(15.dp))
                        Text(
                            calledNumbers.lastOrNull()?.toString() ?: "--",
                            color = NeonGold,
                            fontSize = 65.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            "${calledNumbers.size} / 90 NUMBERS CALLED",
                            color = SoftWhite,
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(18.dp))
                        NeonButton(
                            text = "🎲  CALL NEXT NUMBER",
                            color = NeonPurple,
                            enabled = calledNumbers.size < 90
                        ) {
                            TambolaGameEngine.callNextNumber(calledNumbers.toSet())
                                ?.let { calledNumbers.add(it) }
                        }
                        Spacer(Modifier.height(10.dp))
                        NeonButton(text = "↻  NEW GAME", color = NeonBlue) {
                            startNewGame()
                        }
                    }

                    Spacer(Modifier.height(18.dp))
                    NeonCard {
                        Text(
                            "TICKET BUNDLE",
                            color = NeonGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(1, 6, 12).forEach { size ->
                                Button(
                                    onClick = {
                                        if (size != bundleSize) startNewGame(size)
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (bundleSize == size) {
                                            NeonPurple
                                        } else {
                                            Color(0xFF252C4A)
                                        },
                                        contentColor = SoftWhite
                                    )
                                ) {
                                    Text("${size}x")
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "$bundleSize TICKETS IN THIS GAME",
                            color = NeonBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(18.dp))
                    NeonCard {
                        Text(
                            "YOUR ROYAL TAMBOLA TICKET",
                            color = NeonGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "TICKET ${selectedTicketIndex + 1} OF ${tickets.size}",
                            color = NeonBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(12.dp))

                        currentTicket.forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                row.forEach { number ->
                                    val wasCalled = number != null && number in calledNumbers
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(0.85f)
                                            .background(
                                                if (wasCalled) NeonGreen else Color(0xFF252C4A),
                                                RoundedCornerShape(5.dp)
                                            )
                                            .border(
                                                1.dp,
                                                if (wasCalled) NeonGreen
                                                else NeonPurple.copy(alpha = 0.6f),
                                                RoundedCornerShape(5.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            number?.toString() ?: "",
                                            color = if (wasCalled) Background else SoftWhite,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(5.dp))
                        }

                        Spacer(Modifier.height(14.dp))
                        Text(
                            "Green = Number Called",
                            color = NeonGreen,
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(15.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { selectedTicketIndex-- },
                                enabled = selectedTicketIndex > 0
                            ) {
                                Text("← PREVIOUS")
                            }
                            OutlinedButton(
                                onClick = { selectedTicketIndex++ },
                                enabled = selectedTicketIndex < tickets.lastIndex
                            ) {
                                Text("NEXT →")
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        NeonButton(
                            text = if (selectedTicketIndex in favouriteTickets) {
                                "★  REMOVE FAVOURITE"
                            } else {
                                "☆  ADD FAVOURITE"
                            },
                            color = NeonGold
                        ) {
                            toggleFavourite()
                        }
                        NeonButton(
                            text = "★  NEXT FAVOURITE (${favouriteTickets.size})",
                            color = NeonBlue,
                            enabled = favouriteTickets.isNotEmpty()
                        ) {
                            openNextFavourite()
                        }
                    }

                    Spacer(Modifier.height(18.dp))
                    NeonCard {
                        Text(
                            "WINNING CLAIMS",
                            color = NeonGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(Modifier.height(10.dp))
                        if (completedPatterns.isEmpty()) {
                            Text(
                                "No winning pattern completed on this ticket yet.",
                                color = SoftWhite,
                                fontSize = 13.sp
                            )
                        } else {
                            completedPatterns.forEach { pattern ->
                                Text(
                                    "🏆 ${pattern.name.replace('_', ' ')}",
                                    color = NeonGreen,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(18.dp))
                    NeonCard {
                        Text(
                            "CALLED NUMBER HISTORY",
                            color = NeonBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Spacer(Modifier.height(10.dp))
                        if (calledNumbers.isEmpty()) {
                            Text("No numbers called yet.", color = SoftWhite)
                        } else {
                            Text(
                                calledNumbers.joinToString("  •  "),
                                color = SoftWhite,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun NeonCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Panel),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}

@Composable
private fun NeonButton(
    text: String,
    color: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = Background,
            disabledContainerColor = Color(0xFF34364A),
            disabledContentColor = Color(0xFF9999A8)
        )
    ) {
        Text(text, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun neonFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = SoftWhite,
    unfocusedTextColor = SoftWhite,
    focusedBorderColor = NeonBlue,
    unfocusedBorderColor = NeonPurple,
    focusedLabelColor = NeonBlue,
    unfocusedLabelColor = SoftWhite,
    cursorColor = NeonGold
)

