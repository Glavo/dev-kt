package org.ice1000.devkt

import org.ice1000.devkt.config.GlobalSettings
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.*
import org.jetbrains.kotlin.com.intellij.openapi.Disposable
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.lexer.KotlinLexer
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

data class ASTToken(
		val start: Int,
		val end: Int,
		val text: String,
		val type: IElementType
)

/**
 * @author ice1000
 * @since v0.0.1
 */
object Kotlin {
	val targetDir = File("./build-cache")
	private val environment: KotlinCoreEnvironment
	private val psiFileFactory: PsiFileFactory
	private val lexer: KotlinLexer

	init {
		val compilerConfiguration = CompilerConfiguration()
		compilerConfiguration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
		compilerConfiguration.put(JVMConfigurationKeys.INCLUDE_RUNTIME, true)
		environment = KotlinCoreEnvironment.createForProduction(Disposable {},
				compilerConfiguration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
		val project = environment.project
		psiFileFactory = PsiFileFactory.getInstance(project)
		val parserDef = KotlinParserDefinition.instance
		lexer = parserDef.createLexer(project) as KotlinLexer
	}

	fun parse(text: String) = psiFileFactory
			.createFileFromText(KotlinLanguage.INSTANCE, text) as KtFile

	fun compileJvm(ktFile: KtFile) {
		ensureTargetDirExists()
		compileFileTo(ktFile, environment, targetDir)
	}

	fun compileJar(ktFile: KtFile) {
		ensureTargetDirExists()
		CompileEnvironmentUtil.writeToJar(
				targetDir.resolve(GlobalSettings.jarName),
				true,
				FqName.fromSegments(listOf("devkt", GlobalSettings.javaClassName)),
				compileFile(ktFile, environment))
	}

	fun compileJs(ktFile: KtFile) {
		ensureTargetDirExists()
		TODO()
	}

	private fun ensureTargetDirExists() {
		if (!targetDir.isDirectory) targetDir.mkdirs()
	}

	// TODO incremental
	fun lex(text: String) = lexer.run {
		start(text)
		generateSequence {
			tokenType
					?.let { ASTToken(tokenStart, tokenEnd, tokenText, it) }
					?.also { advance() }
		}
	}
}

val stringTokens = TokenSet.create(
		KtTokens.OPEN_QUOTE,
		KtTokens.CLOSING_QUOTE,
		KtTokens.REGULAR_STRING_PART
)

val stringTemplateTokens = TokenSet.create(
		KtTokens.SHORT_TEMPLATE_ENTRY_START,
		KtTokens.LONG_TEMPLATE_ENTRY_START,
		KtTokens.LONG_TEMPLATE_ENTRY_END
)

