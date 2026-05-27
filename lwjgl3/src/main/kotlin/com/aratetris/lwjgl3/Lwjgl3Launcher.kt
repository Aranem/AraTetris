package com.aratetris.lwjgl3

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.aratetris.TetrisGame

/** Windows/desktop entry point. */
fun main() {
    val config = Lwjgl3ApplicationConfiguration().apply {
        setTitle("AraTetris")
        setWindowedMode(480, 800)
        setForegroundFPS(60)
        useVsync(true)
        setResizable(true)
    }
    Lwjgl3Application(TetrisGame(), config)
}

/** Allow launching via a class (the configured application mainClass) as well as the top-level main. */
object Lwjgl3Launcher {
    @JvmStatic
    fun main(args: Array<String>) = com.aratetris.lwjgl3.main()
}
