package me.ankitgrai.kaizen.domain

enum class AppCategory(val label: String) {
    All("All"),
    Social("Social"),
    Video("Video"),
    Work("Work"),
    Music("Music"),
    Games("Games"),
    Other("Other"),
    ;

    companion object {
        val labels: List<String> = entries.map { it.label }
        val defaultCategories: List<String> = entries.filter { it != All }.map { it.label }
    }
}

fun categorizeApp(packageName: String, appName: String): String {
    val id = "$packageName $appName".lowercase()
    return when {
        SOCIAL.any { it in id } -> AppCategory.Social.label
        VIDEO.any { it in id } -> AppCategory.Video.label
        MUSIC.any { it in id } -> AppCategory.Music.label
        GAMES.any { it in id } -> AppCategory.Games.label
        WORK.any { it in id } -> AppCategory.Work.label
        else -> AppCategory.Other.label
    }
}

fun initials(name: String): String {
    return name
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "?" }
}

private val SOCIAL = listOf(
    "instagram", "discord", "whatsapp", "telegram",
    "facebook", "messenger", "snapchat", "reddit",
    "signal", "threads", "x.android",
)

private val VIDEO = listOf(
    "youtube", "netflix", "twitch", "primevideo",
    "hotstar", "video", "vlc", "mxplayer",
    "crunchyroll", "morphe",
)

private val MUSIC = listOf(
    "spotify", "music", "soundcloud", "deezer",
    "gaana", "jiosaavn", "podcast",
)

private val GAMES = listOf(
    "genshin", "minecraft", "roblox", "game",
    "pubg", "freefire", "clashofclans",
    "playstation", "xbox",
)

private val WORK = listOf(
    "vscode", "visual.studio", "notion",
    "office", "docs", "sheets", "slack",
    "github", "gitlab", "trello", "evernote",
)
