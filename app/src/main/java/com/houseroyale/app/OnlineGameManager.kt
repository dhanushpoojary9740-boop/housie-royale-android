
package com.houseroyale.app

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.security.SecureRandom

data class OnlinePlayer(
    val id: String,
    val name: String,
    val isHost: Boolean
)

data class OnlineRoomState(
    val roomId: String = "",
    val code: String = "",
    val hostId: String = "",
    val status: String = "lobby",
    val players: List<OnlinePlayer> = emptyList(),
    val calledNumbers: List<Int> = emptyList(),
    val tickets: Map<String, List<List<Int>>> = emptyMap()
)

class OnlineGameManager {

    private val auth = FirebaseAuth.getInstance()

    private val database = FirebaseDatabase.getInstance(
        "https://housie-royale-default-rtdb.asia-southeast1.firebasedatabase.app"
    )

    private val root = database.reference

    private var roomListener: ValueEventListener? = null
    private var roomReference: DatabaseReference? = null

    val playerId: String?
        get() = auth.currentUser?.uid

    fun signIn(onResult: (Boolean, String) -> Unit) {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            onResult(true, currentUser.uid)
            return
        }

        auth.signInAnonymously()
            .addOnSuccessListener {
                onResult(true, it.user?.uid ?: "")
            }
            .addOnFailureListener {
                onResult(false, it.message ?: "Anonymous sign-in failed.")
            }
    }

    fun createRoom(
        playerName: String,
        tickets: List<TambolaTicket>,
        onResult: (Boolean, String) -> Unit
    ) {
        val uid = playerId

        if (uid == null) {
            onResult(false, "Please sign in first.")
            return
        }

        val roomId = root.child("rooms").push().key

        if (roomId == null) {
            onResult(false, "Could not create a room.")
            return
        }

        val code = generateRoomCode()
        val room = root.child("rooms").child(roomId)

        val meta = mapOf(
            "hostId" to uid,
            "hostName" to playerName,
            "status" to "lobby",
            "createdAt" to System.currentTimeMillis()
        )

        val player = mapOf(
            "name" to playerName,
            "joinedAt" to System.currentTimeMillis()
        )

        val ticketData = tickets.map { ticket ->
            ticket.map { row ->
                row.map { number -> number ?: 0 }
            }
        }

        room.child("meta").setValue(meta)
            .addOnSuccessListener {
                room.child("players").child(uid).setValue(player)
                    .addOnSuccessListener {
                        room.child("tickets").child(uid).setValue(ticketData)
                            .addOnSuccessListener {
                                root.child("codes").child(code)
                                    .setValue(mapOf("roomId" to roomId))
                                    .addOnSuccessListener {
                                        onResult(true, code)
                                    }
                                    .addOnFailureListener {
                                        onResult(
                                            false,
                                            it.message ?: "Could not publish room code."
                                        )
                                    }
                            }
                            .addOnFailureListener {
                                onResult(
                                    false,
                                    it.message ?: "Could not save host tickets."
                                )
                            }
                    }
                    .addOnFailureListener {
                        onResult(
                            false,
                            it.message ?: "Could not add host to room."
                        )
                    }
            }
            .addOnFailureListener {
                onResult(
                    false,
                    it.message ?: "Could not create room."
                )
            }
    }

    fun joinRoom(
        code: String,
        playerName: String,
        tickets: List<TambolaTicket>,
        onResult: (Boolean, String) -> Unit
    ) {
        val uid = playerId

        if (uid == null) {
            onResult(false, "Please sign in first.")
            return
        }

        if (code.length != 6) {
            onResult(false, "Enter a valid 6-digit room code.")
            return
        }

        root.child("codes").child(code).get()
            .addOnSuccessListener { codeSnapshot ->

                val roomId = codeSnapshot.child("roomId")
                    .getValue(String::class.java)

                if (roomId.isNullOrBlank()) {
                    onResult(false, "Room code not found.")
                    return@addOnSuccessListener
                }

                val room = root.child("rooms").child(roomId)

                room.child("meta").get()
                    .addOnSuccessListener { meta ->

                        val status = meta.child("status")
                            .getValue(String::class.java) ?: "lobby"

                        if (status != "lobby" && status != "playing") {
                            onResult(false, "This game is no longer available.")
                            return@addOnSuccessListener
                        }

                        val player = mapOf(
                            "name" to playerName,
                            "joinedAt" to System.currentTimeMillis()
                        )

                        room.child("players").child(uid).setValue(player)
                            .addOnSuccessListener {

                                val ticketData = tickets.map { ticket ->
                                    ticket.map { row ->
                                        row.map { number -> number ?: 0 }
                                    }
                                }

                                room.child("tickets").child(uid)
                                    .setValue(ticketData)
                                    .addOnSuccessListener {
                                        onResult(true, roomId)
                                    }
                                    .addOnFailureListener {
                                        onResult(
                                            false,
                                            it.message ?: "Could not save your tickets."
                                        )
                                    }
                            }
                            .addOnFailureListener {
                                onResult(
                                    false,
                                    it.message ?: "Could not join room."
                                )
                            }
                    }
                    .addOnFailureListener {
                        onResult(
                            false,
                            it.message ?: "Could not read room details."
                        )
                    }
            }
            .addOnFailureListener {
                onResult(
                    false,
                    it.message ?: "Could not look up room code."
                )
            }
    }

    fun listenToRoom(
        roomId: String,
        code: String,
        onUpdate: (OnlineRoomState) -> Unit,
        onError: (String) -> Unit
    ) {
        stopListening()

        val reference = root.child("rooms").child(roomId)
        roomReference = reference

        val listener = object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val meta = snapshot.child("meta")

                    val hostId = meta.child("hostId")
                        .getValue(String::class.java) ?: ""

                    val status = meta.child("status")
                        .getValue(String::class.java) ?: "lobby"

                    val players = snapshot.child("players")
                        .children.mapNotNull { playerSnapshot ->

                            val id = playerSnapshot.key
                                ?: return@mapNotNull null

                            val name = playerSnapshot.child("name")
                                .getValue(String::class.java) ?: "Player"

                            OnlinePlayer(
                                id = id,
                                name = name,
                                isHost = id == hostId
                            )
                        }

                    val calledNumbers = snapshot.child("calledNumbers")
                        .children.mapNotNull {
                            it.getValue(Int::class.java)
                        }

                    // Correctly read all three ticket levels:
                    // player -> ticket -> row -> number.
                    val tickets = mutableMapOf<String, List<List<Int>>>()

                    snapshot.child("tickets").children.forEach { playerTickets ->

                        val playerId = playerTickets.key
                            ?: return@forEach

                        val allRows = mutableListOf<List<Int>>()

                        playerTickets.children.forEach { ticketSnapshot ->

                            ticketSnapshot.children.forEach { rowSnapshot ->

                                val row = rowSnapshot.children.mapNotNull { numberSnapshot ->
                                    numberSnapshot.getValue(Int::class.java)
                                }

                                allRows.add(row)
                            }
                        }

                        tickets[playerId] = allRows
                    }

                    onUpdate(
                        OnlineRoomState(
                            roomId = roomId,
                            code = code,
                            hostId = hostId,
                            status = status,
                            players = players,
                            calledNumbers = calledNumbers,
                            tickets = tickets
                        )
                    )

                } catch (e: Exception) {
                    onError(
                        "Could not read room data: " +
                            (e.message ?: "Unknown error")
                    )
                }
            }

            override fun onCancelled(error: DatabaseError) {
                onError("Firebase listener error: ${error.message}")
            }
        }

        roomListener = listener
        reference.addValueEventListener(listener)
    }

    fun startGame(
        roomId: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val uid = playerId

        if (uid == null) {
            onResult(false, "Sign-in required.")
            return
        }

        val meta = root.child("rooms")
            .child(roomId)
            .child("meta")

        meta.child("hostId").get()
            .addOnSuccessListener { snapshot ->

                if (snapshot.getValue(String::class.java) != uid) {
                    onResult(false, "Only the host can start the game.")
                    return@addOnSuccessListener
                }

                meta.child("status").setValue("playing")
                    .addOnSuccessListener {
                        onResult(true, "Game started.")
                    }
                    .addOnFailureListener {
                        onResult(
                            false,
                            it.message ?: "Could not start game."
                        )
                    }
            }
            .addOnFailureListener {
                onResult(
                    false,
                    it.message ?: "Could not verify host."
                )
            }
    }

    fun callNumber(
        roomId: String,
        numbers: List<Int>,
        onResult: (Boolean, String) -> Unit
    ) {
        val uid = playerId

        if (uid == null) {
            onResult(false, "Sign-in required.")
            return
        }

        val meta = root.child("rooms").child(roomId).child("meta")

        meta.child("hostId").get()
            .addOnSuccessListener { snapshot ->

                if (snapshot.getValue(String::class.java) != uid) {
                    onResult(false, "Only the host can call numbers.")
                    return@addOnSuccessListener
                }

                root.child("rooms").child(roomId)
                    .child("calledNumbers")
                    .setValue(numbers)
                    .addOnSuccessListener {
                        onResult(true, "Number updated.")
                    }
                    .addOnFailureListener {
                        onResult(
                            false,
                            it.message ?: "Could not update number."
                        )
                    }
            }
            .addOnFailureListener {
                onResult(
                    false,
                    it.message ?: "Could not verify host."
                )
            }
    }

    fun submitClaim(
        roomId: String,
        patternName: String,
        ticketIndex: Int,
        onResult: (Boolean, String) -> Unit
    ) {
        val uid = playerId

        if (uid == null) {
            onResult(false, "Sign-in required.")
            return
        }

        val claimReference = root.child("rooms")
            .child(roomId)
            .child("claims")
            .child(uid)

        val claimId = claimReference.push().key

        if (claimId == null) {
            onResult(false, "Could not create claim.")
            return
        }

        val claim = mapOf(
            "pattern" to patternName,
            "ticketIndex" to ticketIndex,
            "playerId" to uid,
            "createdAt" to System.currentTimeMillis(),
            "status" to "pending"
        )

        claimReference.child(claimId).setValue(claim)
            .addOnSuccessListener {
                onResult(true, "Claim submitted for verification.")
            }
            .addOnFailureListener {
                onResult(
                    false,
                    it.message ?: "Could not submit claim."
                )
            }
    }

    fun leaveRoom(
        roomId: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val uid = playerId

        if (uid == null) {
            onResult(false, "Sign-in required.")
            return
        }

        val room = root.child("rooms").child(roomId)

        room.child("players").child(uid).removeValue()
            .addOnSuccessListener {

                room.child("tickets").child(uid).removeValue()
                    .addOnSuccessListener {
                        stopListening()
                        onResult(true, "You left the room.")
                    }
                    .addOnFailureListener {
                        onResult(
                            false,
                            it.message ?: "Could not remove tickets."
                        )
                    }
            }
            .addOnFailureListener {
                onResult(
                    false,
                    it.message ?: "Could not leave room."
                )
            }
    }

    fun stopListening() {
        val listener = roomListener
        val reference = roomReference

        if (listener != null && reference != null) {
            reference.removeEventListener(listener)
        }

        roomListener = null
        roomReference = null
    }

    private fun generateRoomCode(): String {
        val random = SecureRandom()
        return (100000 + random.nextInt(900000)).toString()
    }
}
