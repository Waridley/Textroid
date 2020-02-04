#!/bin/bash
cd credential-manager && ./gradlew publishToMavenLocal
cd ../twitch4j && ./gradlew publishToMavenLocal