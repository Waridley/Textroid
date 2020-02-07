import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

dependencies {
	api(project(":api:frontend"))
	api("com.github.twitch4j", "twitch4j", "64.$version")
}

val compileKotlin: KotlinCompile by tasks

compileKotlin.kotlinOptions {
	freeCompilerArgs = listOf("-XXLanguage:+InlineClasses")
}