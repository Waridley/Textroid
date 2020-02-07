import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

dependencies {
	implementation("org.litote.kmongo", "kmongo-id-jackson")
	implementation("com.fasterxml.jackson.core", "jackson-annotations")
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
	freeCompilerArgs = listOf("-XXLanguage:+InlineClasses")
}