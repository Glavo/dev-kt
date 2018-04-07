package org.ice1000.devkt.ui

import com.bulenkov.iconloader.util.SystemInfo
import net.iharder.dnd.FileDrop
import org.ice1000.devkt.*
import org.ice1000.devkt.`{-# LANGUAGE SarasaGothicFont #-}`.loadFont
import org.ice1000.devkt.config.ColorScheme
import org.ice1000.devkt.config.GlobalSettings
import org.ice1000.devkt.psi.KotlinAnnotator
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.SyntaxTraverser
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.utils.addToStdlib.indexOfOrNull
import org.jetbrains.kotlin.utils.addToStdlib.lastIndexOfOrNull
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.io.File
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.UndoableEditEvent
import javax.swing.text.*
import javax.swing.undo.UndoManager

/**
 * @author ice1000
 * @since v0.0.1
 */
class UIImpl(frame: DevKtFrame) : AbstractUI(frame) {
	private val undoManager = UndoManager()
	private var edited = false
		set(value) {
			val change = field != value
			field = value
			if (change) refreshTitle()
		}

	internal lateinit var saveMenuItem: JMenuItem
	internal lateinit var showInFilesMenuItem: JMenuItem
	private var lineNumber = 1
	private val document: KtDocument

	private inner class KtDocument : DefaultStyledDocument(), AnnotationHolder {
		private val highlightCache = ArrayList<AttributeSet?>(5000)
		private val colorScheme = ColorScheme(GlobalSettings, attributeContext)
		private val annotator = KotlinAnnotator()
		override val len: Int get() = length

		init {
			addUndoableEditListener {
				if (it.source !== this) return@addUndoableEditListener
				undoManager.addEdit(it.edit)
			}
			adjustFormat()
		}

		override fun adjustFormat(offs: Int, length: Int) {
			if (length <= 0) return
			setParagraphAttributesDoneBySettings(offs, length, colorScheme.tabSize, false)
			val currentLineNumber = text.count { it == '\n' } + 1
			val change = currentLineNumber != lineNumber
			lineNumber = currentLineNumber
			if (change) lineNumberLabel.text = (1..currentLineNumber).joinToString(
					separator = "<br/>", prefix = "<html>", postfix = "&nbsp;</html>")
		}

		override val text: String get() = editor.text

		//TODO 按下 `(` 后输入 `)` 会变成 `())`
		override fun insertString(offs: Int, str: String, a: AttributeSet?) {
			val (offset, string, attr, move) = when {
				str.length > 1 -> Quad(offs, str, a, 0)
				str in paired -> Quad(offs, str + paired[str], a, -1)
				else -> Quad(offs, str, a, 0)
			}

			super.insertString(offset, string, attr)
			editor.caretPosition += move
			reparse()
			adjustFormat(offset, string.length)
		}

		override fun remove(offs: Int, len: Int) {
			val delString = this.text.substring(offs, offs + len)        //即将被删除的字符串
			val (offset, length) = when {
				delString in paired            //是否存在于字符对里
						&& text.getOrNull(offs + 1)?.toString() == paired[delString] -> {
					offs to 2        //
				}

				else -> offs to len
			}

			super.remove(offset, length)
			reparse()
			adjustFormat(offset, length)
		}

		private fun lex() {
			val tokens = Kotlin.lex(text)
			for ((start, end, _, type) in tokens)
				attributesOf(type)?.let { highlight(start, end, it) }
		}

		private fun parse() {
			SyntaxTraverser
					.psiTraverser(Kotlin.parse(text).also { ktFileCache = it })
					.forEach { psi ->
						if (psi !is PsiWhiteSpace) annotator.annotate(psi, this, colorScheme)
					}
		}

		fun reparse() {
			while (highlightCache.size <= length) highlightCache.add(null)
			// val time = System.currentTimeMillis()
			if (GlobalSettings.highlightTokenBased) lex()
			// val time2 = System.currentTimeMillis()
			if (GlobalSettings.highlightSemanticBased) parse()
			// val time3 = System.currentTimeMillis()
			rehighlight()
			// benchmark
			// println("${time2 - time}, ${time3 - time2}, ${System.currentTimeMillis() - time3}")
		}

		private fun rehighlight() {
			if (length > 1) try {
				writeLock()
				var tokenStart = 0
				var attributeSet = highlightCache[0]
				highlightCache[0] = null
				for (i in 1 until highlightCache.size) {
					if (attributeSet != highlightCache[i]) {
						if (attributeSet != null)
							setCharacterAttributesDoneByCache(tokenStart, i - tokenStart, attributeSet, true)
						tokenStart = i
						attributeSet = highlightCache[i]
					}
					highlightCache[i] = null
				}
			} finally {
				writeUnlock()
			}
		}

		/**
		 * Re-implement of [setCharacterAttributes], invoke [fireUndoableEditUpdate] with
		 * [highlightCache] as event source, which is used by [undoManager] to prevent color
		 * modifications to be recorded.
		 */
		private fun setCharacterAttributesDoneByCache(offset: Int, length: Int, s: AttributeSet, replace: Boolean) {
			val changes = DefaultDocumentEvent(offset, length, DocumentEvent.EventType.CHANGE)
			buffer.change(offset, length, changes)
			val sCopy = s.copyAttributes()
			var lastEnd: Int
			var pos = offset
			while (pos < offset + length) {
				val run = getCharacterElement(pos)
				lastEnd = run.endOffset
				if (pos == lastEnd) break
				val attr = run.attributes as MutableAttributeSet
				changes.addEdit(AttributeUndoableEdit(run, sCopy, replace))
				if (replace) attr.removeAttributes(attr)
				attr.addAttributes(s)
				pos = lastEnd
			}
			changes.end()
			fireChangedUpdate(changes)
			fireUndoableEditUpdate(UndoableEditEvent(highlightCache, changes))
		}

		/**
		 * Re-implement of [setParagraphAttributes], invoke [fireUndoableEditUpdate] with
		 * [GlobalSettings] as event source, which is used by [undoManager] to prevent color
		 * modifications to be recorded.
		 */
		private fun setParagraphAttributesDoneBySettings(
				offset: Int, length: Int, s: AttributeSet, replace: Boolean) = try {
			writeLock()
			val changes = DefaultDocumentEvent(offset, length, DocumentEvent.EventType.CHANGE)
			val sCopy = s.copyAttributes()
			val section = defaultRootElement
			for (i in section.getElementIndex(offset)..section.getElementIndex(offset + if (length > 0) length - 1 else 0)) {
				val paragraph = section.getElement(i)
				val attr = paragraph.attributes as MutableAttributeSet
				changes.addEdit(AttributeUndoableEdit(paragraph, sCopy, replace))
				if (replace) attr.removeAttributes(attr)
				attr.addAttributes(s)
			}
			changes.end()
			fireChangedUpdate(changes)
			fireUndoableEditUpdate(UndoableEditEvent(GlobalSettings, changes))
		} finally {
			writeUnlock()
		}

		/**
		 * @see com.intellij.openapi.fileTypes.SyntaxHighlighter.getTokenHighlights
		 */
		private fun attributesOf(type: IElementType) = when (type) {
			IDENTIFIER -> colorScheme.identifiers
			CHARACTER_LITERAL -> colorScheme.charLiteral
			EOL_COMMENT -> colorScheme.lineComments
			DOC_COMMENT -> colorScheme.docComments
			SEMICOLON -> colorScheme.semicolon
			COLON -> colorScheme.colon
			COMMA -> colorScheme.comma
			INTEGER_LITERAL, FLOAT_LITERAL -> colorScheme.numbers
			LPAR, RPAR -> colorScheme.parentheses
			LBRACE, RBRACE -> colorScheme.braces
			LBRACKET, RBRACKET -> colorScheme.brackets
			BLOCK_COMMENT, SHEBANG_COMMENT -> colorScheme.blockComments
			in stringTokens -> colorScheme.string
			in stringTemplateTokens -> colorScheme.templateEntries
			in KEYWORDS -> colorScheme.keywords
			in OPERATIONS -> colorScheme.operators
			else -> null
		}

		override fun highlight(tokenStart: Int, tokenEnd: Int, attributeSet: AttributeSet) =
				doHighlight(tokenStart, tokenEnd, attributeSet)

		/**
		 * @see com.intellij.lang.annotation.AnnotationHolder.createAnnotation
		 */
		fun doHighlight(tokenStart: Int, tokenEnd: Int, attributeSet: AttributeSet) {
			if (tokenStart >= tokenEnd) return
			for (i in tokenStart until tokenEnd) {
				highlightCache[i] = attributeSet
			}
		}
	}

	init {
		mainMenu(menuBar, frame)
		document = KtDocument()
		editor.document = document
	}

	/**
	 * Should only be called once, extracted from the constructor
	 * to shorten the startup time
	 */
	@JvmName("   ")
	internal fun postInit() {
		refreshLineNumber()
		FileDrop(mainPanel) {
			it.firstOrNull { it.canRead() }?.let {
				loadFile(it)
			}
		}
		val lastOpenedFile = File(GlobalSettings.lastOpenedFile)
		if (lastOpenedFile.canRead()) {
			edited = false
			loadFile(lastOpenedFile)
		}
		editor.addKeyListener(object : KeyAdapter() {
			override fun keyPressed(e: KeyEvent) {
				if (e.isControlDown && !e.isAltDown && !e.isShiftDown && e.keyCode == KeyEvent.VK_Z) undo()
				if (e.isControlDown && !e.isAltDown && !e.isShiftDown && e.keyCode == KeyEvent.VK_S) save()
				if (e.isControlDown && e.isAltDown && !e.isShiftDown && e.keyCode == KeyEvent.VK_Y) sync()
				if (e.isControlDown && !e.isAltDown && e.isShiftDown && e.keyCode == KeyEvent.VK_Z) redo()
				if (!e.isControlDown && !e.isAltDown && e.isShiftDown && e.keyCode == KeyEvent.VK_ENTER) nextLine()
				if (e.isControlDown && !e.isAltDown && !e.isShiftDown && e.keyCode == KeyEvent.VK_ENTER) splitLine()
				if (e.isControlDown && e.isAltDown && !e.isShiftDown && e.keyCode == KeyEvent.VK_ENTER) newLineBeforeCurrent()
			}
		})
	}

	fun save() {
		val file = currentFile ?: JFileChooser(GlobalSettings.recentFiles.firstOrNull()?.parentFile).apply {
			showSaveDialog(mainPanel)
			fileSelectionMode = JFileChooser.FILES_ONLY
		}.selectedFile ?: return
		currentFile = file
		if (!file.exists()) file.createNewFile()
		GlobalSettings.recentFiles.add(file)
		file.writeText(editor.text)
		message("Saved to ${file.absolutePath}")
		edited = false
	}

	fun createNewFile(templateName: String) {
		if (!makeSureLeaveCurrentFile()) {
			currentFile = null
			edited = true
			editor.text = javaClass
					.getResourceAsStream("/template/$templateName")
					.reader()
					.readText()
		}
	}

	override fun loadFile(it: File) {
		if (it.canRead() and !makeSureLeaveCurrentFile()) {
			currentFile = it
			message("Loaded ${it.absolutePath}")
			val path = it.absolutePath.orEmpty()
			document.remove(0, document.len)
			document.insertString(0, it.readText(), null)
			edited = false
			GlobalSettings.lastOpenedFile = path
		}
		updateShowInFilesMenuItem()
	}

	//这三个方法应该可以合并成一个方法吧
	fun nextLine() {
		val index = editor.caretPosition        //光标所在位置
		val text = editor.text                //编辑器内容
		val endOfLineIndex = text.indexOfOrNull('\n', index) ?: document.len
		document.insertString(endOfLineIndex, "\n", null)
		editor.caretPosition = endOfLineIndex + 1
	}

	fun splitLine() {
		val index = editor.caretPosition        //光标所在位置
		document.insertString(index, "\n", null)
		editor.caretPosition = index
	}

	fun newLineBeforeCurrent() {
		val index = editor.caretPosition
		val text = editor.text
		val startOfLineIndex = text.lastIndexOfOrNull('\n', index.takeIf { it > 0 }?.minus(1) ?: 0) ?: 0        //一行的开头
		document.insertString(startOfLineIndex, "\n", null)
		editor.caretPosition = startOfLineIndex + 1
	}

	override fun ktFile() = ktFileCache ?: Kotlin.parse(editor.text)

	override fun makeSureLeaveCurrentFile() =
			edited && super.makeSureLeaveCurrentFile()

	fun buildClassAndRun() {
		buildAsClasses { if (it) runCommand(Kotlin.targetDir) }
	}

	fun buildJarAndRun() {
		buildAsJar { if (it) runCommand(Kotlin.targetJar) }
	}

	private fun runCommand(file: File) {
		val java = "java -cp ${file.absolutePath}:$selfLocation devkt.${GlobalSettings.javaClassName}Kt"
		val processBuilder = when {
			SystemInfo.isLinux -> {
				ProcessBuilder("gnome-terminal", "-x", "sh", "-c", "$java; bash")
			}
			SystemInfo.isMac -> {
				val lajiJava = "/usr/bin/$java"
				ProcessBuilder("osascript", "-e", "tell app \"Terminal\" to do script \"$lajiJava\"")
			}
			SystemInfo.isWindows -> {
				ProcessBuilder("cmd.exe", "/c", "start", "$java && pause")
			}
			else -> {
				JOptionPane.showMessageDialog(mainPanel, "Unsupported OS!")
				return
			}
		}
		currentFile?.run { processBuilder.directory(parentFile.absoluteFile) }
		processBuilder.start()
	}

	override fun updateShowInFilesMenuItem() {
		showInFilesMenuItem.isEnabled = currentFile != null
	}

	fun undo() {
		if (undoManager.canUndo()) {
			message("Undo!")
			undoManager.undo()
			edited = true
		}
	}

	fun redo() {
		if (undoManager.canRedo()) {
			message("Redo!")
			undoManager.redo()
			edited = true
		}
	}

	fun selectAll() {
		message("Select All")
		editor.selectAll()
	}

	fun cut() {
		message("Cut selection")
		editor.cut()
	}

	fun copy() {
		message("Copied selection")
		editor.copy()
	}

	fun paste() {
		message("Pasted to current position")
		editor.paste()
	}

	override fun reloadSettings() {
		frame.bounds = GlobalSettings.windowBounds
		imageCache = null
		loadFont()
		refreshTitle()
		refreshLineNumber()
		with(document) {
			adjustFormat()
			reparse()
		}
	}

	override fun refreshTitle() {
		frame.title = buildString {
			if (edited) append("*")
			append(currentFile?.absolutePath ?: "Untitled")
			append(" - ")
			append(GlobalSettings.appName)
		}
	}

}
