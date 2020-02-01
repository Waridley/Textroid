dependencies {
	implementation(project(":credentials"))
	implementation(project(":ttv"))
	implementation(project(":mongo"))
	
	implementation("com.github.philippheuer.events4j", "events4j-handler-reactor")
	implementation("ch.qos.logback","logback-classic")
	implementation("com.github.ajalt", "clikt")
	
}