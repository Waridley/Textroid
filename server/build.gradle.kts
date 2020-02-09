plugins {
	kotlin("jvm")
}

dependencies {
	implementation(project(":api:backend"))
	implementation(project(":api:frontend"))
	implementation(project(":engine"))
}