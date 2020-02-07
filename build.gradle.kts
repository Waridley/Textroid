import org.apache.tools.ant.taskdefs.condition.Os

plugins {
	kotlin("jvm") version "1.3.61"
}

allprojects {
	group = "com.waridley"
	version = "0.2"
	
	apply(plugin = "java")
	apply(plugin = "kotlin")
	
	repositories {
		mavenLocal()
		mavenCentral()
		jcenter()
	}
	
	dependencies {
		implementation(kotlin("stdlib-jdk8"))
		implementation("org.litote.kmongo", "kmongo-id-jackson", "3.12.0")
		implementation("com.natpryce", "result4k", "2.0.0")
		implementation("com.fasterxml.jackson.core", "jackson-annotations", "2.9.7")
		implementation("com.github.ajalt", "clikt", "2.3.0")
		implementation("ch.qos.logback", "logback-classic", "1.2.3")
		implementation("com.github.philippheuer.events4j", "events4j-handler-reactor", "0.7.1")
		
		
		//Twitch4j dependencies because Spring dependency-management plugin does not work with composite builds
		implementation("com.apollographql.apollo:apollo-runtime:1.2.2")
		implementation("com.apollographql.apollo:apollo-gradle-plugin:1.2.2")
		implementation("com.github.philippheuer.events4j:events4j-core")
		implementation("com.github.philippheuer.events4j:events4j-handler-simple")
		implementation("org.slf4j:slf4j-api:1.7.30")
		implementation("ch.qos.logback:logback-classic:1.2.3")
		implementation("org.apache.commons:commons-lang3:3.9")
		implementation("commons-io:commons-io:2.6")
		implementation("org.apache.commons:commons-collections4:4.4")
		implementation("commons-configuration:commons-configuration:1.10")
		implementation("com.github.philippheuer.events4j:events4j-core:0.7.1")
		implementation("com.github.philippheuer.events4j:events4j-handler-simple:0.7.1")
		implementation("com.github.philippheuer.credentialmanager:credentialmanager:0.1.0")
		implementation("com.squareup.okhttp3:okhttp:4.3.1")
		implementation("com.github.vladimir-bukhtoyarov:bucket4j-core:4.7.0")
		implementation("io.github.openfeign:feign-okhttp:10.7.4")
		implementation("io.github.openfeign:feign-jackson:10.7.4")
		implementation("io.github.openfeign:feign-slf4j:10.7.4")
		implementation("io.github.openfeign:feign-hystrix:10.7.4")
		implementation("com.netflix.hystrix:hystrix-core:1.5.18")
		implementation("com.fasterxml.jackson.core:jackson-databind:2.10.2")
		implementation("com.fasterxml.jackson.module:jackson-module-parameter-names:2.10.2")
		implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.10.2")
		implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.10.2")
		implementation("com.neovisionaries:nv-websocket-client:2.9")
		implementation("org.jetbrains:annotations:18.0.0")
		implementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
		implementation("org.junit.jupiter:junit-jupiter-engine:5.6.0")
		implementation("org.projectlombok:lombok:1.18.10")
	}
}

dependencies {
	implementation(project("credentials"))
	implementation(project("engine"))
	implementation(project("server"))
	implementation(project("mongo"))
	implementation(project("ttv:chat_client"))
	implementation(project("ttv:monitor"))
	
	
	
	implementation("com.fasterxml.jackson.core", "jackson-databind", "2.9.7")
}

tasks {
	init {
		dependsOn("setEnv")
	}
	compileKotlin {
		kotlinOptions.jvmTarget = "1.8"
	}
	compileTestKotlin {
		kotlinOptions.jvmTarget = "1.8"
	}
}

task("publishDeps", Exec::class) {
	environment("CI_COMMIT_REF_NAME", "64.$version")
	if (Os.isFamily(Os.FAMILY_WINDOWS)) {
		executable("cmd.exe")
		args(
				"/C",
				"cd credential-manager && gradlew.bat publishToMavenLocal && cd ../twitch4j && gradlew.bat publishToMavenLocal")
	} else if (Os.isFamily(Os.FAMILY_UNIX)) {
		executable("bash")
		args(
				"-c",
				"cd credential-manager && ./gradlew publishToMavenLocal && cd ../twitch4j && ./gradlew publishToMavenLocal"
		)
	}
}

task("setEnv", Exec::class) {
	if (Os.isFamily(Os.FAMILY_WINDOWS)) {
		executable("cmd.exe")
		args("/C", "set CI_COMMIT_REF_NAME=64.$version")
	} else if (Os.isFamily(Os.FAMILY_UNIX)) {
		executable("bash")
		args("-c", "export CI_COMMIT_REF_NAME=64.$version")
	}
}