package com.elg.swiftsplit.infrastructure.remote

object LiveSplitProtocol {
    
    
    const val TERMINATOR = "\n"
    
    
    const val DEFAULT_PORT = 16834

    
    const val CMD_START_OR_SPLIT = "startorsplit"
    const val CMD_SPLIT = "split"
    const val CMD_UNSPLIT = "unsplit"
    const val CMD_SKIP_SPLIT = "skipsplit"
    const val CMD_PAUSE = "pause"
    const val CMD_RESUME = "resume"
    const val CMD_RESET = "reset"
    
    const val CMD_GET_CURRENT_TIME = "getcurrenttime"
    const val CMD_GET_DELTA = "getdelta"
    const val CMD_GET_SPLIT_INDEX = "getsplitindex"
    const val CMD_GET_SPLIT_NAME = "getcurrentsplitname"
    const val CMD_GET_GAME_NAME = "getgamename"
    const val CMD_GET_CATEGORY_NAME = "getcategoryname"
    const val CMD_GET_TIMER_PHASE = "getcurrenttimerphase"
    const val CMD_PING = "ping"

    
    fun expectsResponse(command: String): Boolean {
        val cmdLower = command.trim().lowercase()
        return cmdLower.startsWith("get") || cmdLower == "ping"
    }
}
