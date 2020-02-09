dependencies {
	implementation(project(":api:backend"))
	implementation(project(":credentials"))
	implementation(project(":engine"))
	implementation(project(":ttv"))
	
	api(group = "org.litote.kmongo", name = "kmongo", version = "3.12.0")
}