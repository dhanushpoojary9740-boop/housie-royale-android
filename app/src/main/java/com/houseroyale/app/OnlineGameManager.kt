
package com.houseroyale.app

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.random.Random

data class OnlineRoom(
    val roomId: String,
    val roomCode: String,
    val hostUid: String,
    val status: String
)

data class OnlinePlayer(
    val uid: String,
    val name: String,
    val isHost: Boolean
)

class OnlineGameManager {

    private val auth = FirebaseAuth.getInstance()

    private val database = FirebaseDatabase.getInstance(
        "https://housie-royale-default-rtdb.asia-southeast1.firebasedatabase.app"
    ).reference

    private var roomListener: ValueEventListener? = null
    private var playersListener: ValueEventListener? = null
    private var numbersListener: ValueEventListener? = null

    private var activeRoomId: String? = null

    fun getCurrentPlayerId(): String? {
        return auth.currentUser?.uid
    }

    fun signIn(
        callback: (Result<String>) -> Unit
    ) {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            callback(Result.success(currentUser.uid))
            return
        }

        auth.signInAnonymously()
            .addOnSuccessListener { result ->
                val uid = result.user?.uid

                if (uid != null) {
                    callback(Result.success(uid))
                } else {
                    callback(
                        Result.failure(
                            Exception("Unable to get player ID")
                        )
                    )
                }
            }
            .addOnFailureListener { error ->
                callback(Result.failure(error))
            }
    }

    fun createRoom(
        playerName: String,
        callback: (Result<OnlineRoom>) -> Unit
    ) {
        if (playerName.isBlank()) {
            callback(
                Result.failure(
                    Exception("Enter your player name")
                )
            )
            return
        }

        signIn { authResult ->

            authResult.onFailure {
                callback(Result.failure(it))
            }

            authResult.onSuccess { uid ->

                val roomRef = database
                    .child("rooms")
                    .push()

                val roomId = roomRef.key

                if (roomId == null) {
                    callback(
                        Result.failure(
                            Exception("Unable to create room ID")
                        )
                    )
                    return@onSuccess
                }

                val meta = mapOf(
                    "hostUid" to uid,
                    "status" to "waiting",
                    "createdAt" to System.currentTimeMillis(),
                    "maxPlayers" to 20
                )

                roomRef.child("meta")
                    .setValue(meta)
                    .addOnSuccessListener {

                        val playerData = mapOf(
                            "name" to playerName.trim(),
                            "isHost" to true,
                            "joinedAt" to System.currentTimeMillis()
                        )

                        roomRef.child("players")
                            .child(uid)
                            .setValue(playerData)
                            .addOnSuccessListener {

                                createRoomCode(
                                    roomId = roomId,
                                    hostUid = uid,
                                    callback = callback
                                )
                            }
                            .addOnFailureListener {
                                callback(Result.failure(it))
                            }
                    }
                    .addOnFailureListener {
                        callback(Result.failure(it))
                    }
            }
        }
    }

    private fun createRoomCode(
        roomId: String,
        hostUid: String,
        callback: (Result<OnlineRoom>) -> Unit
    ) {
        val code = Random.nextInt(100000, 1000000).toString()

        val codeRef = database
            .child("codes")
            .child(code)

        codeRef.get()
            .addOnSuccessListener { snapshot ->

                if (snapshot.exists()) {
                    createRoomCode(
                        roomId,
                        hostUid,
                        callback
                    )
                    return@addOnSuccess
                }

                val codeData = mapOf(
                    "roomId" to roomId
                )

                codeRef.setValue(codeData)
                    .addOnSuccessListener {

                        activeRoomId = roomId

                        callback(
                            Result.success(
                                OnlineRoom(
                                    roomId = roomId,
                                    roomCode = code,
                                    hostUid = hostUid,
                                    status = "waiting"
                                )
                            )
                        )
                    }
                    .addOnFailureListener {
                        callback(Result.failure(it))
                    }
            }
            .addOnFailureListener {
                callback(Result.failure(it))
            }
    }

    fun joinRoom(
        roomCode: String,
        playerName: String,
        callback: (Result<OnlineRoom>) -> Unit
    ) {
        if (roomCode.length != 6 || !roomCode.all { it.isDigit() }) {
            callback(
                Result.failure(
                    Exception("Enter a valid 6-digit room code")
                )
            )
            return
        }

        if (playerName.isBlank()) {
            callback(
                Result.failure(
                    Exception("Enter your player name")
                )
            )
            return
        }

        signIn { authResult ->

            authResult.onFailure {
                callback(Result.failure(it))
            }

            authResult.onSuccess { uid ->

                database.child("codes")
                    .child(roomCode)
                    .get()
                    .addOnSuccessListener { codeSnapshot ->

                        if (!codeSnapshot.exists()) {
                            callback(
                                Result.failure(
                                    Exception("Room code not found")
                                )
                            )
                            return@addOnSuccess
                        }

                        val roomId = codeSnapshot
                            .child("roomId")
                            .getValue(String::class.java)

                        if (roomId.isNullOrBlank()) {
                            callback(
                                Result.failure(
                                    Exception("Invalid room information")
                                )
                            )
                            return@addOnSuccess
                        }

                        val roomRef = database
                            .child("rooms")
                            .child(roomId)

                        roomRef.child("meta")
                            .get()
                            .addOnSuccessListener { metaSnapshot ->

                                val hostUid = metaSnapshot
                                    .child("hostUid")
                                    .getValue(String::class.java)

                                val status = metaSnapshot
                                    .child("status")
                                    .getValue(String::class.java)

                                if (hostUid == null || status != "waiting") {
                                    callback(
                                        Result.failure(
                                            Exception("This room is not accepting players")
                                        )
                                    )
                                    return@addOnSuccessListener
                                }

                                val playerData = mapOf(
                                    "name" to playerName.trim(),
                                    "isHost" to false,
                                    "joinedAt" to System.currentTimeMillis()
                                )

                                roomRef.child("players")
                                    .child(uid)
                                    .setValue(playerData)
                                    .addOnSuccessListener {

                                        activeRoomId = roomId

                                        callback(
                                            Result.success(
                                                OnlineRoom(
                                                    roomId = roomId,
                                                    roomCode = roomCode,
                                                    hostUid = hostUid,
                                                    status = status
                                                )
                                            )
                                        )
                                    }
                                    .addOnFailureListener {
                                        callback(Result.failure(it))
                                    }
                            }
                            .addOnFailureListener {
                                callback(Result.failure(it))
                            }
                    }
                    .addOnFailureListener {
                        callback(Result.failure(it))
                    }
            }
        }
    }

    fun listenToPlayers(
        roomId: String,
        callback: (List<OnlinePlayer>) -> Unit
    ) {
        removePlayersListener()

        val ref = database
            .child("rooms")
            .child(roomId)
            .child("players")

        playersListener = object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                val players = snapshot.children.mapNotNull { child ->

                    val uid = child.key ?: return@mapNotNull null

                    val name = child.child("name")
                        .getValue(String::class.java)
                        ?: "Player"

                    val isHost = child.child("isHost")
                        .getValue(Boolean::class.java)
                        ?: false

                    OnlinePlayer(
                        uid = uid,
                        name = name,
                        isHost = isHost
                    )
                }

                callback(players)
            }

            override fun onCancelled(error: DatabaseError) {
                callback(emptyList())
            }
        }

        ref.addValueEventListener(playersListener!!)
    }

    fun listenToRoom(
        roomId: String,
        callback: (String) -> Unit
    ) {
        removeRoomListener()

        val ref = database
            .child("rooms")
            .child(roomId)
            .child("meta")
            .child("status")

        roomListener = object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                callback(
                    snapshot.getValue(String::class.java) ?: "unknown"
                )
            }

            override fun onCancelled(error: DatabaseError) {
                callback("error")
            }
        }

        ref.addValueEventListener(roomListener!!)
    }

    fun startGame(
        roomId: String,
        callback: (Result<Unit>) -> Unit
    ) {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            callback(
                Result.failure(
                    Exception("Player is not signed in")
                )
            )
            return
        }

        val metaRef = database
            .child("rooms")
            .child(roomId)
            .child("meta")

        metaRef.child("hostUid")
            .get()
            .addOnSuccessListener { snapshot ->

                val hostUid = snapshot.getValue(String::class.java)

                if (hostUid != uid) {
                    callback(
                        Result.failure(
                            Exception("Only the host can start the game")
                        )
                    )
                    return@addOnSuccessListener
                }

                metaRef.child("status")
                    .setValue("playing")
                    .addOnSuccessListener {
                        callback(Result.success(Unit))
                    }
                    .addOnFailureListener {
                        callback(Result.failure(it))
                    }
            }
            .addOnFailureListener {
                callback(Result.failure(it))
            }
    }

    fun callNumber(
        roomId: String,
        number: Int,
        callback: (Result<Unit>) -> Unit
    ) {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            callback(
                Result.failure(
                    Exception("Player is not signed in")
                )
            )
            return
        }

        if (number !in 1..90) {
            callback(
                Result.failure(
                    Exception("Number must be between 1 and 90")
                )
            )
            return
        }

        val metaRef = database
            .child("rooms")
            .child(roomId)
            .child("meta")

        metaRef.child("hostUid")
            .get()
            .addOnSuccessListener { snapshot ->

                if (snapshot.getValue(String::class.java) != uid) {
                    callback(
                        Result.failure(
                            Exception("Only the host can call numbers")
                        )
                    )
                    return@addOnSuccessListener
                }

                val numbersRef = database
                    .child("rooms")
                    .child(roomId)
                    .child("calledNumbers")

                numbersRef.child(number.toString())
                    .setValue(true)
                    .addOnSuccessListener {
                        callback(Result.success(Unit))
                    }
                    .addOnFailureListener {
                        callback(Result.failure(it))
                    }
            }
            .addOnFailureListener {
                callback(Result.failure(it))
            }
    }

    fun listenToCalledNumbers(
        roomId: String,
        callback: (Set<Int>) -> Unit
    ) {
        removeNumbersListener()

        val ref = database
            .child("rooms")
            .child(roomId)
            .child("calledNumbers")

        numbersListener = object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                val numbers = snapshot.children.mapNotNull { child ->
                    child.key?.toIntOrNull()
                }.toSet()

                callback(numbers)
            }

            override fun onCancelled(error: DatabaseError) {
                callback(emptySet())
            }
        }

        ref.addValueEventListener(numbersListener!!)
    }

    fun leaveRoom(
        roomId: String,
        callback: (Result<Unit>) -> Unit
    ) {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            callback(Result.success(Unit))
            return
        }

        removeAllListeners()

        database.child("rooms")
            .child(roomId)
            .child("players")
            .child(uid)
            .removeValue()
            .addOnSuccessListener {
                activeRoomId = null
                callback(Result.success(Unit))
            }
            .addOnFailureListener {
                callback(Result.failure(it))
            }
    }

    fun removeAllListeners() {
        removePlayersListener()
        removeRoomListener()
        removeNumbersListener()
    }

    private fun removePlayersListener() {
        playersListener?.let {
            activeRoomId?.let { roomId ->
                database.child("rooms")
                    .child(roomId)
                    .child("players")
                    .removeEventListener(it)
            }
        }
        playersListener = null
    }

    private fun removeRoomListener() {
        roomListener?.let {
            activeRoomId?.let { roomId ->
                database.child("rooms")
                    .child(roomId)
                    .child("meta")
                    .child("status")
                    .removeEventListener(it)
            }
        }
        roomListener = null
    }

    private fun removeNumbersListener() {
        numbersListener?.let {
            activeRoomId?.let { roomId ->
                database.child("rooms")
                    .child(roomId)
                    .child("calledNumbers")
                    .removeEventListener(it)
            }
        }
        numbersListener = null
    }
}
