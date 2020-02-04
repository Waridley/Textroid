import org.apache.tools.ant.taskdefs.condition.Os

plugins {
	kotlin("jvm") version "1.3.61"
}

allprojects {
	group = "com.waridley"
	version = "0.1"

	apply(plugin = "java")
	apply(plugin = "kotlin")
	
	repositories {
		mavenLocal()
		mavenCentral()
		jcenter()
	}
	
	dependencies {
		implementation(kotlin("stdlib-jdk8"))
		implementation("com.natpryce", "result4k", "2.0.0")
		implementation("com.fasterxml.jackson.core", "jackson-annotations", "2.9.7")
		implementation("com.github.ajalt", "clikt", "2.3.0")
		implementation("ch.qos.logback","logback-classic","1.2.3")
		implementation("com.github.philippheuer.events4j", "events4j-handler-reactor", "0.7.1")
	}
}

dependencies {
	implementation(project("credentials"))
	implementation(project("game"))
	implementation(project("server"))
	implementation(project("mongo"))
	implementation(project("ttv:chat_client"))
	implementation(project("ttv:monitor"))
	
	implementation("com.fasterxml.jackson.core", "jackson-databind", "2.9.7")
}

tasks {

	compileKotlin {
		kotlinOptions.jvmTarget = "1.8"
	}
	compileTestKotlin {
		kotlinOptions.jvmTarget = "1.8"
	}
}

task("publishDeps", Exec::class) {
	environment("CI_COMMIT_REF_NAME", "64.$version")
	if(Os.isFamily(Os.FAMILY_WINDOWS)) {
		executable("cmd.exe")
		args("publishDeps.bat")
	} else if(Os.isFamily(Os.FAMILY_UNIX)) {
		executable("bash")
		args("publishDeps.sh")
	}
}