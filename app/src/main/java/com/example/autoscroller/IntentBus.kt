package com.example.autoscroller

object IntentBus {
    const val ACTION_SCROLL_CONTROL = "com.example.autoscroller.ACTION_SCROLL_CONTROL"
    const val EXTRA_COMMAND   = "command"
    const val EXTRA_SPEED     = "speed"      // px/sec
    const val EXTRA_DIRECTION = "direction"  // +1 down, -1 up
    const val CMD_START  = "start"
    const val CMD_STOP   = "stop"
    const val CMD_UPDATE = "update"
}
