dependencies {
	implementation(project(":credentials"))
	api(project(":ttv"))
	implementation(project(":mongo"))
	
	api("com.github.philippheuer.events4j", "events4j-handler-reactor")
	implementation("ch.qos.logback", "logback-classic")
	
}