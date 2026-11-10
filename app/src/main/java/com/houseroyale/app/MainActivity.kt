
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

enum class AppScreen {
    HOME, ONLINE, GAME
}

@Composable
fun HousieRoyaleApp() {

    var screen by remember { mutableStateOf(AppScreen.HOME) }
    var playerName by remember { mutableStateOf("") }
    var gameCode by remember { mutableStateOf("") }

    var ticket by remember {
        mutableStateOf(TambolaGameEngine.generateTicket())
    }

    val calledNumbers = remember {
        mutableStateListOf<Int>()
    }

    val completedPatterns =
        TambolaGameEngine.completedPatterns(
            ticket,
            calledNumbers.toSet()
        )

    fun startOfflineGame() {
        calledNumbers.clear()
        ticket = TambolaGameEngine.generateTicket()
        screen = AppScreen.GAME
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF11152D),
                        Background,
                        Color(0xFF160B29)
                    )
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

                    Spacer(Modifier.height(35.dp))

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
                        "VERSION 2.2 DEVELOPMENT",
                        color = NeonBlue,
                        fontSize = 13.sp,
                        letterSpacing = 2.sp
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        "THE ROYAL TAMBOLA EXPERIENCE",
                        color = SoftWhite,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )

                    Spacer(Modifier.height(35.dp))

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
                            onValueChange = {
                                playerName = it.take(20)
                            },
                            label = { Text("Enter your name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = neonFieldColors()
                        )

                        Spacer(Modifier.height(24.dp))

                        NeonButton(
                            text = "🎮  PLAY OFFLINE",
                            color = NeonPurple,
                            enabled = playerName.isNotBlank()
                        ) {
                            startOfflineGame()
                        }

                        Spacer(Modifier.height(14.dp))

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
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(30.dp))
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
                            "Online multiplayer connection is planned " +
                                "for a future development stage.",
                            color = SoftWhite,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(18.dp))

                        OutlinedTextField(
                            value = gameCode,
                            onValueChange = {
                                gameCode = it.filter { c ->
                                    c.isDigit()
                                }.take(6)
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

                        TextButton(
                            onClick = {
                                screen = AppScreen.HOME
                            }
                        ) {
                            Text("EXIT", color = NeonBlue)
                        }
                    }

                    Text(
                        "Player: $playerName",
                        color = SoftWhite,
                        fontSize = 14.sp
                    )

                    Spacer(Modifier.height(18.dp))

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
                            TambolaGameEngine.callNextNumber(
                                calledNumbers.toSet()
                            )?.let { next ->
                                calledNumbers.add(next)
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        NeonButton(
                            text = "↻  NEW GAME",
                            color = NeonBlue
                        ) {
                            startOfflineGame()
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    NeonCard {

                        Text(
                            "YOUR ROYAL TAMBOLA TICKET",
                            color = NeonGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )

                        Spacer(Modifier.height(15.dp))

                        ticket.forEach { row ->

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {

                                row.forEach { number ->

                                    val wasCalled =
                                        number != null &&
                                            number in calledNumbers

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(0.85f)
                                            .background(
                                                if (wasCalled) NeonGreen
                                                else Color(0xFF252C4A),
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
                                            color = if (wasCalled)
                                                Background
                                            else SoftWhite,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(5.dp))
                        }

                        Spacer(Modifier.height(12.dp))

                        Text(
                            "Green = Number Called",
                            color = NeonGreen,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    NeonCard {

                        Text(
                            "🏆 WINNING CLAIMS",
                            color = NeonGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )

                        Spacer(Modifier.height(12.dp))

                        val patternLabels = listOf(
                            WinningPattern.EARLY_FIVE to "Early Five",
                            WinningPattern.TOP_LINE to "Top Line",
                            WinningPattern.MIDDLE_LINE to "Middle Line",
                            WinningPattern.BOTTOM_LINE to "Bottom Line",
                            WinningPattern.FOUR_CORNERS to "Four Corners",
                            WinningPattern.FULL_HOUSE to "Full House"
                        )

                        patternLabels.forEach { (pattern, label) ->

                            val achieved = pattern in completedPatterns

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Text(
                                    label,
                                    color = if (achieved)
                                        NeonGreen
                                    else SoftWhite,
                                    fontSize = 14.sp,
                                    fontWeight = if (achieved)
                                        FontWeight.Bold
                                    else FontWeight.Normal
                                )

                                Text(
                                    if (achieved) "🏆 WON" else "🔒",
                                    color = if (achieved)
                                        NeonGreen
                                    else SoftWhite,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        if (completedPatterns.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))

                            Text(
                                "CONGRATULATIONS, $playerName!",
                                color = NeonGold,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    NeonCard {

                        Text(
                            "CALLED NUMBER HISTORY",
                            color = NeonBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            if (calledNumbers.isEmpty()) {
                                "No numbers called yet"
                            } else {
                                calledNumbers.joinToString("  •  ")
                            },
                            color = SoftWhite,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(Modifier.height(25.dp))
                }
            }
        }
    }
}

@Composable
fun NeonCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                NeonPurple.copy(alpha = 0.8f),
                RoundedCornerShape(22.dp)
            ),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Panel
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}

@Composable
fun NeonButton(
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
            .height(54.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = Background,
            disabledContainerColor = Color.DarkGray
        )
    ) {
        Text(
            text,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp
        )
    }
}

@Composable
fun neonFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = NeonBlue,
    unfocusedBorderColor = NeonPurple,
    focusedLabelColor = NeonBlue,
    unfocusedLabelColor = SoftWhite,
    focusedTextColor = SoftWhite,
    unfocusedTextColor = SoftWhite,
    cursorColor = NeonBlue
)
