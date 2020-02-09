package com.waridley.textroid.mongo.ttv

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential
import com.github.twitch4j.helix.TwitchHelix
import com.waridley.textroid.ttv.TtvUserStorageInterface

class MongoTtvStorage(override val helix: TwitchHelix, override val credential: OAuth2Credential): TtvUserStorageInterface {


}