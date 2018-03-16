package org.gradle.kotlin.dsl.plugins.precompiled

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify

import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle
import org.gradle.api.tasks.TaskContainer

import org.gradle.kotlin.dsl.fixtures.AbstractPluginTest
import org.gradle.kotlin.dsl.fixtures.assertInstanceOf
import org.gradle.kotlin.dsl.fixtures.classLoaderFor

import org.gradle.kotlin.dsl.precompile.PrecompiledInitScript
import org.gradle.kotlin.dsl.precompile.PrecompiledProjectScript
import org.gradle.kotlin.dsl.precompile.PrecompiledSettingsScript

import org.gradle.testkit.runner.TaskOutcome

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat

import org.junit.Test


class PrecompiledScriptPluginTest : AbstractPluginTest() {

    @Test
    fun `Project scripts from regular source-sets are compiled via the PrecompiledProjectScript template`() {

        givenPrecompiledKotlinScript("my-project-script.gradle.kts", """

            task("my-task")

        """)

        val project = mock<Project>()

        assertInstanceOf<PrecompiledProjectScript>(
            instantiatePrecompiledScriptOf(
                project,
                "My_project_script_gradle"))

        verify(project).task("my-task")
    }

    @Test
    fun `Settings scripts from regular source-sets are compiled via the PrecompiledSettingsScript template`() {

        givenPrecompiledKotlinScript("my-settings-script.settings.gradle.kts", """

            include("my-project")

        """)

        val settings = mock<Settings>()

        assertInstanceOf<PrecompiledSettingsScript>(
            instantiatePrecompiledScriptOf(
                settings,
                "My_settings_script_settings_gradle"))

        verify(settings).include("my-project")
    }

    @Test
    fun `Gradle scripts from regular source-sets are compiled via the PrecompiledInitScript template`() {

        givenPrecompiledKotlinScript("my-gradle-script.init.gradle.kts", """

            useLogger("my-logger")

        """)

        val gradle = mock<Gradle>()

        assertInstanceOf<PrecompiledInitScript>(
            instantiatePrecompiledScriptOf(
                gradle,
                "My_gradle_script_init_gradle"))

        verify(gradle).useLogger("my-logger")
    }

    @Test
    fun `implicit imports are available to precompiled scripts`() {

        givenPrecompiledKotlinScript("my-project-script.gradle.kts", """

            task<Jar>("jar")

        """)

        val tasks = mock<TaskContainer>()
        val project = mock<Project>() {
            on { getTasks() } doReturn tasks
        }

        instantiatePrecompiledScriptOf(
            project,
            "My_project_script_gradle")

        verify(tasks).create("jar", org.gradle.api.tasks.bundling.Jar::class.java)
    }

    private
    fun givenPrecompiledKotlinScript(fileName: String, code: String) {
        withPrecompiledScriptPlugins()
        withFile("src/main/kotlin/$fileName", code)
        compileKotlin()
    }

    private
    inline fun <reified T> instantiatePrecompiledScriptOf(target: T, className: String): Any =
        loadCompiledKotlinClass(className)
            .getConstructor(T::class.java)
            .newInstance(target)

    private
    fun loadCompiledKotlinClass(className: String) =
        classLoaderFor(existing("build/classes/kotlin/main"))
            .loadClass(className)

    private
    fun withPrecompiledScriptPlugins() =
        withBuildScript("""

            plugins {
                `kotlin-dsl`
            }

            apply<${PrecompiledScriptPlugins::class.qualifiedName}>()

        """)

    private
    fun compileKotlin() {
        assertThat(
            buildWithPlugin("classes").outcomeOf(":compileKotlin"),
            equalTo(TaskOutcome.SUCCESS))
    }
}