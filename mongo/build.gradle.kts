dependencies {
	implementation(project(":api:backend"))
	implementation(project(":credentials"))
	implementation(project(":game"))
	
	api(group = "org.litote.kmongo", name = "kmongo", version = "3.12.0")
}