package com.example.vinylog.games

data class GameSystem(val name: String, val labelColor: Long)

val GameSystems = listOf(
    GameSystem("Nintendo Switch 2", 0xFFE60012),
    GameSystem("Nintendo Switch", 0xFFE60012),
    GameSystem("PlayStation 5", 0xFF2D6CDF),
    GameSystem("PlayStation 4", 0xFF003087),
    GameSystem("Xbox Series", 0xFF107C10),
    GameSystem("Xbox One", 0xFF0B5C0B),
    GameSystem("Nintendo 3DS", 0xFFC62828),
    GameSystem("PlayStation 3", 0xFF1A237E),
    GameSystem("Xbox 360", 0xFF6FA83A),
    GameSystem("Wii", 0xFF8E9AA8),
    GameSystem("PlayStation 2", 0xFF0D47A1),
    GameSystem("GameCube", 0xFF6A1B9A),
    GameSystem("Nintendo DS", 0xFFB71C1C),
    GameSystem("PlayStation", 0xFF1565C0),
    GameSystem("PSP", 0xFF263238),
    GameSystem("Nintendo 64", 0xFF4E342E),
    GameSystem("Super Nintendo", 0xFFAD1457),
    GameSystem("NES", 0xFFD84315),
    GameSystem("Game Boy", 0xFF7CB342),
    GameSystem("Sega Genesis", 0xFF1A237E),
    GameSystem("Dreamcast", 0xFFF57C00),
    GameSystem("PC", 0xFF5E35B1),
)

fun isGameSystem(mediaType: String): Boolean = GameSystems.any { it.name == mediaType }
