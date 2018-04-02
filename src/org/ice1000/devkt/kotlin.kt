package org.ice1000.devkt

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.Disposable
import org.jetbrains.kotlin.com.intellij.openapi.extensions.ExtensionPoint
import org.jetbrains.kotlin.com.intellij.openapi.extensions.Extensions.getArea
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.TreeCopyHandler
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.KotlinLanguage

object Kotlin {
	private val psiFileFactory: PsiFileFactory

	init {
		val compilerConfiguration = CompilerConfiguration()
		compilerConfiguration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
		val project = KotlinCoreEnvironment.createForProduction(Disposable {},
				compilerConfiguration, EnvironmentConfigFiles.JVM_CONFIG_FILES).project
		val extensionPoint = "org.jetbrains.kotlin.com.intellij.treeCopyHandler"
		val extensionClassName = TreeCopyHandler::class.java.name.orEmpty()
		for (area in arrayOf(getArea(project), getArea(null))) {
			if (!area.hasExtensionPoint(extensionPoint)) {
				area.registerExtensionPoint(extensionPoint, extensionClassName, ExtensionPoint.Kind.INTERFACE)
			}
		}
		psiFileFactory = PsiFileFactory.getInstance(project)
	}

	fun parse(text: String) = psiFileFactory
			.createFileFromText(KotlinLanguage.INSTANCE, text)
}