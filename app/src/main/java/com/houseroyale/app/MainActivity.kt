
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.database.FirebaseDatabase

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
    val context = LocalContext.current
    val onlineManager = remember { OnlineGameManager() }

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

    var voiceEnabled by remember { mutableStateOf(true) }
    var voiceReady by remember { mutableStateOf(false) }
    var soundEnabled by remember { mutableStateOf(true) }
    var speechRate by remember { mutableFloatStateOf(1.0f) }

    var celebrationMessage by remember { mutableStateOf<String?>(null) }
    val announcedWins = remember { mutableStateOf(setOf<String>()) }

    // Online multiplayer state.
    var onlineBusy by remember { mutableStateOf(false) }
    var onlineMessage by remember { mutableStateOf("") }
    var onlineRoomId by remember { mutableStateOf("") }
    var onlineRoomCode by remember { mutableStateOf("") }
    var onlineRoomState by remember { mutableStateOf<OnlineRoomState?>(null) }
    var onlinePlayerId by remember { mutableStateOf("") }

    val onlineCalledNumbers = onlineRoomState?.calledNumbers ?: emptyList()
    val onlineCalledSet = onlineCalledNumbers.toSet()
    val onlineIsHost = onlineRoomState?.hostId == onlinePlayerId

    val caller = remember(context) {
        SmartNumberCaller(context) { ready ->
            voiceReady = ready
        }
    }

    DisposableEffect(caller) {
        onDispose {
            caller.release()
            onlineManager.stopListening()
        }
    }

    LaunchedEffect(speechRate) {
        caller.setSpeechRate(speechRate)
    }

    LaunchedEffect(soundEnabled) {
        caller.setSoundEnabled(soundEnabled)
    }

    val currentTicket = tickets.getOrNull(selectedTicketIndex)
        ?: tickets.first()

    val calledSet = calledNumbers.toSet()

    val winningResults = remember(tickets, calledNumbers.size) {
        TambolaGameEngine.getAllTicketWinningResults(
            tickets,
            calledSet
        )
    }

    LaunchedEffect(winningResults) {
        val newWins = winningResults.flatMap { result ->
            result.completedPatterns.map { pattern ->
                Triple(
                    result.ticketIndex,
                    pattern,
                    "${result.ticketIndex}_${pattern.name}"
                )
            }
        }.filter { (_, _, key) ->
            key !in announcedWins.value
        }

        if (newWins.isNotEmpty()) {
            announcedWins.value = announcedWins.value +
                newWins.map { it.third }

            val firstWin = newWins.first()
            celebrationMessage =
                "Ticket ${firstWin.first + 1} completed " +
                    firstWin.second.name.replace('_', ' ') + "!"
        }
    }

    val currentTicketPatterns = winningResults
        .firstOrNull { it.ticketIndex == selectedTicketIndex }
        ?.completedPatterns
        ?: emptySet()

    fun startNewGame(size: Int = bundleSize) {
        bundleSize = size
        tickets = TambolaGameEngine.generateTickets(size)
        selectedTicketIndex = 0
        favouriteTickets = emptySet()
        calledNumbers.clear()
        announcedWins.value = emptySet()
        celebrationMessage = null
        caller.stopAnnouncement()
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

    fun listenToOnlineRoom(roomId: String, code: String) {
        onlineRoomId = roomId
        onlineRoomCode = code
        onlineMessage = "Connecting to room..."

        onlineManager.listenToRoom(
            roomId = roomId,
            code = code,
            onUpdate = { state ->
                onlineRoomState = state
                onlineMessage = ""
            },
            onError = { error ->
                onlineMessage = error
            }
        )
    }

    fun createOnlineRoom() {
        if (playerName.isBlank()) {
            onlineMessage = "Enter your player name first."
            return
        }

        onlineBusy = true
        onlineMessage = "Signing in..."

        onlineManager.signIn { success, result ->
            if (!success) {
                onlineBusy = false
                onlineMessage = result
                return@signIn
            }

            onlinePlayerId = result

            onlineManager.createRoom(
                playerName = playerName.trim(),
                tickets = tickets
            ) { created, roomCodeOrError ->

                if (!created) {
                    onlineBusy = false
                    onlineMessage = roomCodeOrError
                    return@createRoom
                }

                val code = roomCodeOrError

                FirebaseDatabase.getInstance(
                    "https://housie-royale-default-rtdb.asia-southeast1.firebasedatabase.app"
                ).reference
                    .child("codes")
                    .child(code)
                    .child("roomId")
                    .get()
                    .addOnSuccessListener { snapshot ->

                        val roomId = snapshot.getValue(String::class.java)

                        if (roomId.isNullOrBlank()) {
                            onlineBusy = false
                            onlineMessage = "Room created, but its ID could not be found."
                        } else {
                            onlineBusy = false
                            listenToOnlineRoom(roomId, code)
                        }
                    }
                    .addOnFailureListener {
                        onlineBusy = false
                        onlineMessage = it.message ?: "Could not open the room."
                    }
            }
        }
    }

    fun joinOnlineRoom() {
        if (playerName.isBlank()) {
            onlineMessage = "Enter your player name first."
            return
        }

        if (gameCode.length != 6) {
            onlineMessage = "Enter a valid 6-digit room code."
            return
        }

        onlineBusy = true
        onlineMessage = "Signing in..."

        onlineManager.signIn { success, result ->
            if (!success) {
                onlineBusy = false
                onlineMessage = result
                return@signIn
            }

            onlinePlayerId = result

            onlineManager.joinRoom(
                code = gameCode,
                playerName = playerName.trim(),
                tickets = tickets
            ) { joined, roomIdOrError ->

                onlineBusy = false

                if (!joined) {
                    onlineMessage = roomIdOrError
                } else {
                    listenToOnlineRoom(roomIdOrError, gameCode)
                }
            }
        }
    }

    fun leaveOnlineRoom() {
        val roomId = onlineRoomId

        if (roomId.isBlank()) {
            onlineManager.stopListening()
            onlineRoomState = null
            onlineRoomCode = ""
            onlineRoomId = ""
            onlineMessage = ""
            screen = AppScreen.HOME
            return
        }

        onlineBusy = true

        onlineManager.leaveRoom(roomId) { success, message ->
            onlineBusy = false
            onlineManager.stopListening()
            onlineRoomState = null
            onlineRoomCode = ""
            onlineRoomId = ""
            onlineMessage = if (success) "" else message
            screen = AppScreen.HOME
        }
    }

    fun startOnlineGame() {
        if (onlineRoomId.isBlank()) return

        onlineBusy = true

        onlineManager.startGame(onlineRoomId) { success, message ->
            onlineBusy = false
            onlineMessage = if (success) "" else message
        }
    }

    fun callOnlineNumber() {
        val room = onlineRoomState ?: return
        if (!onlineIsHost || onlineRoomId.isBlank()) return

        val next = TambolaGameEngine.callNextNumber(
            room.calledNumbers.toSet()
        ) ?: return

        val updatedNumbers = room.calledNumbers + next
        onlineBusy = true

        onlineManager.callNumber(
            onlineRoomId,
            updatedNumbers
        ) { success, message ->
            onlineBusy = false

            if (success) {
                caller.announce(next, voiceEnabled)
                onlineMessage = ""
            } else {
                onlineMessage = message
            }
        }
    }

    fun submitOnlineClaim(pattern: WinningPattern, ticketIndex: Int) {
        if (onlineRoomId.isBlank()) return

        onlineBusy = true

        onlineManager.submitClaim(
            roomId = onlineRoomId,
            patternName = pattern.name,
            ticketIndex = ticketIndex
        ) { success, message ->
            onlineBusy = false
            onlineMessage = message
        }
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
                        "VERSION 2.4",
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
                                        containerColor =
                                            if (bundleSize == size) NeonPurple
                                            else Color(0xFF252C4A),
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
                            onlineMessage = ""
                            onlineRoomState = null
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
                val room = onlineRoomState

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🌐", fontSize = 55.sp)

                    Text(
                        "ONLINE ROYALE",
                        color = NeonBlue,
                        fontSize = 27.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Spacer(Modifier.height(18.dp))

                    if (room == null) {
                        NeonCard {
                            Text(
                                "CREATE OR JOIN A GAME",
                                color = NeonGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )

                            Spacer(Modifier.height(15.dp))

                            Text(
                                "Create a room and invite friends, or join a friend's room using their six-digit code.",
                                color = SoftWhite,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )

                            Spacer(Modifier.height(18.dp))

                            NeonButton(
                                text = "👑  CREATE NEW ROOM",
                                color = NeonPurple,
                                enabled = !onlineBusy
                            ) {
                                createOnlineRoom()
                            }

                            Spacer(Modifier.height(18.dp))

                            Text(
                                "OR JOIN A ROOM",
                                color = NeonGold,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(Modifier.height(10.dp))

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

                            Spacer(Modifier.height(12.dp))

                            NeonButton(
                                text = "🚀  JOIN ROOM",
                                color = NeonBlue,
                                enabled = !onlineBusy && gameCode.length == 6
                            ) {
                                joinOnlineRoom()
                            }
                        }
                    } else {
                        NeonCard {
                            Text(
                                "ROOM CODE",
                                color = NeonGold,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                room.code,
                                color = NeonBlue,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 5.sp
                            )

                            Text(
                                "Share this code with your friends.",
                                color = SoftWhite,
                                fontSize = 13.sp
                            )

                            Spacer(Modifier.height(18.dp))

                            Text(
                                "ROOM STATUS: ${room.status.uppercase()}",
                                color = NeonGreen,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(Modifier.height(15.dp))

                            Text(
                                "PLAYERS (${room.players.size})",
                                color = NeonGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )

                            room.players.forEach { player ->
                                Text(
                                    (if (player.isHost) "👑 " else "🎮 ") +
                                        player.name +
                                        if (player.id == onlinePlayerId) " (YOU)" else "",
                                    color = if (player.isHost) NeonGold else SoftWhite,
                                    modifier = Modifier.padding(vertical = 5.dp)
                                )
                            }

                            Spacer(Modifier.height(18.dp))

                            if (room.status == "lobby") {
                                Text(
                                    if (onlineIsHost) {
                                        "You are the host. Start when everyone has joined."
                                    } else {
                                        "Waiting for the host to start the game..."
                                    },
                                    color = SoftWhite,
                                    textAlign = TextAlign.Center
                                )

                                if (onlineIsHost) {
                                    Spacer(Modifier.height(14.dp))

                                    NeonButton(
                                        text = "▶  START ONLINE GAME",
                                        color = NeonGreen,
                                        enabled = !onlineBusy
                                    ) {
                                        startOnlineGame()
                                    }
                                }
                            } else {
                                Text(
                                    "🎉 GAME IN PROGRESS",
                                    color = NeonGreen,
                                    fontWeight = FontWeight.ExtraBold
                                )

                                Spacer(Modifier.height(15.dp))

                                Text(
                                    onlineCalledNumbers.lastOrNull()?.toString() ?: "--",
                                    color = NeonGold,
                                    fontSize = 60.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )

                                Text(
                                    "${onlineCalledNumbers.size} / 90 NUMBERS CALLED",
                                    color = SoftWhite
                                )

                                if (onlineIsHost) {
                                    Spacer(Modifier.height(14.dp))

                                    NeonButton(
                                        text = "🎲  CALL NEXT NUMBER",
                                        color = NeonPurple,
                                        enabled = !onlineBusy &&
                                            onlineCalledNumbers.size < 90
                                    ) {
                                        callOnlineNumber()
                                    }
                                } else {
                                    Text(
                                        "The host is calling the numbers.",
                                        color = NeonBlue,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }

                                Spacer(Modifier.height(16.dp))

                                Text(
                                    "YOUR ONLINE TICKET",
                                    color = NeonGold,
                                    fontWeight = FontWeight.Bold
                                )

                                val onlineTicket = tickets.firstOrNull()

                                onlineTicket?.forEach { row ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        row.forEach { number ->
                                            val wasCalled =
                                                number != null && number in onlineCalledSet

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
                                                        NeonPurple.copy(alpha = 0.6f),
                                                        RoundedCornerShape(5.dp)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    number?.toString() ?: "",
                                                    color = if (wasCalled) Background
                                                    else SoftWhite,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(5.dp))
                                }

                                val onlineResults =
                                    TambolaGameEngine.getAllTicketWinningResults(
                                        tickets,
                                        onlineCalledSet
                                    )

                                onlineResults.forEach { result ->
                                    result.completedPatterns.forEach { pattern ->
                                        Spacer(Modifier.height(8.dp))
                                        NeonButton(
                                            text = "🏆 CLAIM ${pattern.name.replace('_', ' ')}",
                                            color = NeonGold,
                                            enabled = !onlineBusy
                                        ) {
                                            submitOnlineClaim(
                                                pattern,
                                                result.ticketIndex
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(15.dp))

                                Text(
                                    "CALLED NUMBERS",
                                    color = NeonBlue,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    onlineCalledNumbers.joinToString("  •  ")
                                        .ifBlank { "No numbers called yet." },
                                    color = SoftWhite,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    if (onlineBusy) {
                        Spacer(Modifier.height(12.dp))
                        CircularProgressIndicator(color = NeonBlue)
                    }

                    if (onlineMessage.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            onlineMessage,
                            color = NeonGold,
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(Modifier.height(15.dp))

                    NeonButton(
                        text = if (room == null) "← BACK TO HOME" else "LEAVE ROOM",
                        color = NeonPurple,
                        enabled = !onlineBusy
                    ) {
                        if (room == null) {
                            screen = AppScreen.HOME
                        } else {
                            leaveOnlineRoom()
                        }
                    }

                    Spacer(Modifier.height(25.dp))
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
                                caller.stopAnnouncement()
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
                            TambolaGameEngine.callNextNumber(
                                calledNumbers.toSet()
                            )?.let { next ->
                                calledNumbers.add(next)
                                caller.announce(next, voiceEnabled)
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        NeonButton(
                            text = "↻  NEW GAME",
                            color = NeonBlue
                        ) {
                            startNewGame()
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    NeonCard {
                        Text(
                            "SMART NUMBER CALLER",
                            color = NeonGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            if (voiceReady) "Voice engine ready"
                            else "Preparing voice engine...",
                            color = if (voiceReady) NeonGreen else NeonBlue,
                            fontSize = 12.sp
                        )

                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "VOICE ANNOUNCEMENTS",
                                color = SoftWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )

                            Switch(
                                checked = voiceEnabled,
                                onCheckedChange = {
                                    voiceEnabled = it
                                    if (!it) caller.stopAnnouncement()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = NeonGold,
                                    checkedTrackColor = NeonPurple
                                )
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "SOUND EFFECTS",
                                color = SoftWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )

                            Switch(
                                checked = soundEnabled,
                                onCheckedChange = { soundEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = NeonGold,
                                    checkedTrackColor = NeonPurple
                                )
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Text(
                            "SPEAKING SPEED",
                            color = NeonBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )

                        Slider(
                            value = speechRate,
                            onValueChange = { speechRate = it },
                            valueRange = 0.5f..1.5f,
                            steps = 3,
                            colors = SliderDefaults.colors(
                                thumbColor = NeonGold,
                                activeTrackColor = NeonPurple,
                                inactiveTrackColor = Color(0xFF343A5A)
                            )
                        )

                        Text(
                            when {
                                speechRate < 0.8f -> "Slow"
                                speechRate > 1.2f -> "Fast"
                                else -> "Normal"
                            },
                            color = SoftWhite,
                            fontSize = 12.sp
                        )

                        Spacer(Modifier.height(12.dp))

                        NeonButton(
                            text = "🔊  TEST VOICE",
                            color = NeonBlue,
                            enabled = voiceReady && voiceEnabled
                        ) {
                            caller.announce(25, true)
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
                                        containerColor =
                                            if (bundleSize == size) NeonPurple
                                            else Color(0xFF252C4A),
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
                                    val wasCalled =
                                        number != null && number in calledSet

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
                                            color = if (wasCalled) Background
                                            else SoftWhite,
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
                            "ALL TICKET WINNING RESULTS",
                            color = NeonGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(12.dp))

                        if (winningResults.isEmpty()) {
                            Text(
                                "No winning pattern completed yet.",
                                color = SoftWhite,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            winningResults.forEach { result ->
                                Text(
                                    "🎟 TICKET ${result.ticketIndex + 1}",
                                    color = NeonBlue,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 10.dp)
                                )

                                result.completedPatterns.forEach { pattern ->
                                    Text(
                                        "🏆 ${pattern.name.replace('_', ' ')}",
                                        color = NeonGreen,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    NeonCard {
                        Text(
                            "SELECTED TICKET CLAIMS",
                            color = NeonGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )

                        Spacer(Modifier.height(10.dp))

                        if (currentTicketPatterns.isEmpty()) {
                            Text(
                                "No winning pattern completed on Ticket ${selectedTicketIndex + 1}.",
                                color = SoftWhite,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            currentTicketPatterns.forEach { pattern ->
                                val isValid = TambolaGameEngine.validateBundleClaim(
                                    tickets = tickets,
                                    ticketIndex = selectedTicketIndex,
                                    pattern = pattern,
                                    calledNumbers = calledSet
                                )

                                Text(
                                    if (isValid) {
                                        "✅ ${pattern.name.replace('_', ' ')} — VALID"
                                    } else {
                                        "❌ ${pattern.name.replace('_', ' ')} — INVALID"
                                    },
                                    color = if (isValid) NeonGreen else Color.Red,
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
                            Text(
                                "No numbers called yet.",
                                color = SoftWhite
                            )
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

        if (celebrationMessage != null) {
            AlertDialog(
                onDismissRequest = { celebrationMessage = null },
                containerColor = Panel,
                title = {
                    Text(
                        "🏆 ROYALE VICTORY!",
                        color = NeonGold,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Text(
                        celebrationMessage ?: "",
                        color = SoftWhite,
                        textAlign = TextAlign.Center,
                        fontSize = 17.sp
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = { celebrationMessage = null }
                    ) {
                        Text("CONTINUE", color = NeonBlue)
                    }
                }
            )
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
