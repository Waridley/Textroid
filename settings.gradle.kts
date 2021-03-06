rootProject.name = "Textroid"

include(
		"api",
		"api:backend",
		"api:frontend",
		"api:game",
		"credentials",
		"engine",
		"mongo",
		"server",
		"ttv",
		"ttv:chat_client",
		"ttv:monitor",
		"textroid-prime"
)



includeBuild("credential-manager") {
	dependencySubstitution {
		substitute(module("com.github.philippheuer.credentialmanager:credentialmanager")).with(project(":"))
	}
}


includeBuild("twitch4j") {
	dependencySubstitution {
		substitute(module("com.github.twitch4j:twitch4j")).with(project(":twitch4j-twitch4j"))
		substitute(module("com.github.twitch4j:twitch4j-auth")).with(project(":twitch4j-auth"))
		substitute(module("com.github.twitch4j:twitch4j-chat")).with(project(":twitch4j-chat"))
		substitute(module("com.github.twitch4j:twitch4j-common")).with(project(":twitch4j-common"))
		substitute(module("com.github.twitch4j:twitch4j-graphql")).with(project(":twitch4j-graphql"))
		substitute(module("com.github.twitch4j:twitch4j-pubsub")).with(project(":twitch4j-pubsub"))
		substitute(module("com.github.twitch4j:twitch4j-rest-helix")).with(project(":twitch4j-rest-helix"))
		substitute(module("com.github.twitch4j:twitch4j-rest-kraken")).with(project(":twitch4j-rest-kraken"))
		substitute(module("com.github.twitch4j:twitch4j-rest-tmi")).with(project(":twitch4j-rest-tmi"))
	}
}

includeBuild("events4j") {
	dependencySubstitution {
		substitute(module("com.github.philippheuer.events4j:events4j-core")).with(project(":events4j-core"))
		substitute(module("com.github.philippheuer.events4j:events4j-handler-simple")).with(project(":events4j-handler-simple"))
		substitute(module("com.github.philippheuer.events4j:events4j-handler-reactor")).with(project(":events4j-handler-reactor"))
		
	}
}