package org.ice1000.devkt.ui

import javafx.scene.image.Image

internal fun loadIcon(url: String) = Image(AllIcons::class.java.getResource(url).toExternalForm())

object AllIcons {
    val KOTLIN = loadIcon("/icon/kotlin.png")
    val KOTLIN_JS = loadIcon("/icon/kotlin_js.png")
    val KOTLIN_MP = loadIcon("/icon/kotlin_multiplatform_project_dark.png")
    val KOTLIN_FILE = loadIcon("/icon/kotlin_file.png")
    val KOTLIN_ANDROID = loadIcon("/icon/kotlin_activity.png")

    // Project related
    val OPEN = loadIcon("/icon/menu-open.png")
    val CUT = loadIcon("/icon/menu-cut_dark.png")
    val COPY = loadIcon("/icon/copy_dark.png")
    val PASTE = loadIcon("/icon/menu-paste.png")
    val COMPILE = loadIcon("/icon/compile_dark.png")
    val CLASS = loadIcon("/icon/javaClass.png")
    val EXECUTE = loadIcon("/icon/execute.png")
    val SETTINGS = loadIcon("/icon/Gear.png")
    val DUMP = loadIcon("/icon/dump_dark.png")
    val EXIT = loadIcon("/icon/exit_dark.png")
    val SAVE = loadIcon("/icon/menu-saveall.png")
    val UNDO = loadIcon("/icon/undo.png")
    val REDO = loadIcon("/icon/redo.png")
    val SYNCHRONIZE = loadIcon("/icon/synchronizeFS.png")
    val REFRESH = loadIcon("/icon/refresh.png")
    val JAR = loadIcon("/icon/archive.png")
    val GRADLE = loadIcon("/icon/gradle.png")

    // Providers
    val ECLIPSE = loadIcon("/icon/eclipse_dark.png")
    val IDEA = loadIcon("/icon/icon_small_dark.png")
    val EMACS = loadIcon("/icon/emacs25.png")
    val CLION = loadIcon("/icon/clion.png")
}