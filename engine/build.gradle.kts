import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

dependencies {
	implementation("org.litote.kmongo", "kmongo-id-jackson", "3.12.0")
	implementation("com.natpryce", "result4k")
	implementation("com.fasterxml.jackson.core", "jackson-annotations")
	implementation(kotlin("scripting-jsr223"))
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
	freeCompilerArgs = listOf("-XXLanguage:+InlineClasses")
}