package pe.aido.cuadre.ui.screens

/** What linking with the signed-in owner's account came back with. */
sealed class LinkOutcome {
    data object Done : LinkOutcome()
    /** The account has no store yet: ask its name and try again. */
    data object NeedStoreName : LinkOutcome()
    data class Error(val message: String) : LinkOutcome()
}
