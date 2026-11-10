
package com.houseroyale.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val RoyalPurple = Color(0xFF210E3B)
private val RoyalGold = Color(0xFFFFD166)
private val CardColor = Color(0xFF382052)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            HousieApp()
        }
    }
}

@Composable
fun HousieApp() {

    var playerName by remember { mutableStateOf("") }
    var gameCode by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = RoyalPurple
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = "👑",
                fontSize = 55.sp
            )

            Text(
                text = "HOUSIE ROYALE",
                color = RoyalGold,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                text = "The Royal Tambola Experience",
                color = Color.White,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(45.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = CardColor
                ),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        "Welcome, Player!",
                        color = RoyalGold,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = playerName,
                        onValueChange = { playerName = it },
                        label = { Text("Enter your name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { },
                        enabled = playerName.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(55.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RoyalGold,
                            contentColor = RoyalPurple
                        )
                    ) {
                        Text(
                            "CREATE GAME",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    HorizontalDivider(
                        color = RoyalGold.copy(alpha = 0.4f)
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    Text(
                        "JOIN EXISTING GAME",
                        color = RoyalGold,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = gameCode,
                        onValueChange = {
                            gameCode = it.filter(Char::isDigit)
                                .take(6)
                        },
                        label = { Text("Enter 6-digit game code") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(15.dp))

                    Button(
                        onClick = { },
                        enabled = playerName.isNotBlank() &&
                            gameCode.length == 6,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(55.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RoyalGold,
                            contentColor = RoyalPurple
                        )
                    ) {
                        Text(
                            "JOIN GAME",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                "PLAY • WIN • CELEBRATE",
                color = RoyalGold,
                fontSize = 12.sp,
                letterSpacing = 3.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
