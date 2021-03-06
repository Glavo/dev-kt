# DevKt

[![GitHub (pre-)release](https://img.shields.io/github/release/ice1000/dev-kt/all.svg)](https://github.com/ice1000/dev-kt)

CI|Status
:---:|:---:
Travis CI (test, no artifact)|[![Build Status](https://travis-ci.org/ice1000/dev-kt.svg?branch=master)](https://travis-ci.org/ice1000/dev-kt)
AppVeyor (artifact, no test)|[![Build status](https://ci.appveyor.com/api/projects/status/c0aq16ej7415m302?svg=true)](https://ci.appveyor.com/project/ice1000/dev-kt)
CircleCI (both)|[![CircleCI](https://circleci.com/gh/ice1000/dev-kt.svg?style=svg)](https://circleci.com/gh/ice1000/dev-kt)

This is a DevCpp-like cross-platform Kotlin IDE features in lightweight.

You can download a snapshot [here](https://ci.appveyor.com/project/ice1000/dev-kt/build/artifacts), the one ends with "-all.jar" is an executable jar.

# Features

+ Fast (at least faster than Emacs/Eclipse/IntelliJ/CLion/VSCode/Atom)
+ Lightweight (Just a tiny Java Swing application)
+ Kotlin compiler integration (**100% correct parsing**)
+ JetBrains IDE icons
+ Build as jar/class files, run after build, just one click
+ Cross platform (windows/macos/linux), just an executable jar
+ One property-based configuration file, hackable

Just a simple comparison:

DevKt<br/><br/>Correct|<img width=200 src="https://user-images.githubusercontent.com/16398479/38292932-3c4ce2be-3818-11e8-9a56-9d30f3109c43.png">
:---:|:---:
IntelliJ IDEA<br/><br/>Correct,<br/>with inspections|<img width=200 src="https://user-images.githubusercontent.com/16398479/38292918-2ec81974-3818-11e8-8eb7-3648cd747ee5.png">
Emacs<br/><br/>Incorrect|<img width=200 src="https://user-images.githubusercontent.com/16398479/38292966-6670c57e-3818-11e8-8a26-3eccf864b93e.png">
VSCode<br/><br/>Incorrect|<img width=200 src="https://user-images.githubusercontent.com/16398479/38293034-95d721be-3818-11e8-9141-19faabae161e.png">

# Screenshots

<img src="https://user-images.githubusercontent.com/16398479/38440232-5ab4d282-3a13-11e8-9b00-5d199d687f8f.png">
<img src="https://user-images.githubusercontent.com/16398479/38440305-983541b4-3a13-11e8-9651-25e9a61a9b9a.png">

# Progress

+ Properties-based settings (hackable!)
	+ [X] Deals with missing properties
	+ [X] Smart auto saving
	+ [X] Highlight color customization
	+ [X] Hot reload (Due to some limitations of Swing it's really hard to implement)
	+ [X] Font customization
		+ [X] Font size customization
	+ [ ] Text style customization
	+ [ ] Keymap customization
	+ [ ] Internationalization
	+ [ ] Settings UI
+ File operations
	+ [X] Create new file when no files are opened
	+ [X] Save and sync
	+ [X] Show in files
	+ [X] Record recently opened files
	+ [X] Create new files from templates (kts, kt, kt2js, android activity, multiplatform codes, etc.)
	+ [X] Drag file to open
+ Editor
	+ [X] Lexer based highlights
	+ [X] Undo and redo (by `javax.swing.undo.UndoManager.UndoManager`)
	+ [X] Copy/paste/cut/select all
	+ [X] Line number
	+ [X] Background image
	+ [X] Insert/delete paired characters
	+ [X] Semantic-based highlights
		+ [ ] Highlight in daemon
		+ [ ] Prioritized
		+ [ ] Incremental
	+ [ ] Highlight selected token
	+ [ ] Auto indent
		+ [ ] Smart indent
		+ [ ] Indent with spaces
+ Build and run
	+ Build
		+ [X] Build as class files
		+ [X] Build as jar
		+ [ ] Build as javascript module
	+ Run
		+ [X] Run as class files
		+ [X] Run as jar
		+ [ ] Run as kotlin script
+ Others
	+ [X] Open alternative editors' download page in browser
	+ [X] MacOS toolbar support
	+ [X] PsiViewer
	+ [X] Memory indicator
	+ [ ] Built-in documentation

# Build

+ (Optional) Download and decompress [Sarasa Gothic](https://github.com/be5invis/Sarasa-Gothic/releases) font to `res/font`
  + As reference you can see [this shell script](./download-font.sh)
+ Use `gradlew fatJar` to build this application
+ Run this application with `java -jar build/libs/devkt-[some unimportant text]-all.jar`

