import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

dependencies {
	api(project(":api"))
	
	implementation("com.fasterxml.jackson.core", "jackson-annotations")
	implementation(kotlin("scripting-jsr223"))
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
	freeCompilerArgs = listOf("-XXLanguage:+InlineClasses")
}