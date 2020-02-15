package com.waridley.textroid.ttv.monitor

import com.github.twitch4j.chat.events.CommandEvent
import com.github.twitch4j.common.events.domain.EventUser
import com.github.twitch4j.helix.domain.User
import com.github.twitch4j.pubsub.domain.ChannelPointsUser
import com.github.twitch4j.pubsub.events.RewardRedeemedEvent
import com.waridley.textroid.ChatCommandEvent
import com.waridley.textroid.MintRequestEvent
import com.waridley.textroid.api.*
import com.waridley.textroid.ttv.TtvUser
import com.waridley.textroid.ttv.TtvUserStorage
import org.slf4j.Logger
import org.slf4j.LoggerFactory

typealias Twitch4jCommandEvent = CommandEvent

class TtvEventConverter(val playerStorage: PlayerStorageInterface, val ttvUserStorage: TtvUserStorage): TextroidEventHandler(
		{
			
			val log = logger<TtvEventConverter>()
			
			val playerCache = mutableMapOf<String, Player>()
			
			fun ChannelPointsUser.findPlayer() =
					playerCache[id] ?: playerStorage.findOrCreateOne(TtvUser::helixUser / User::getId stores id, listOf(Player::username stores login, Player::nickname stores (displayName ?: login))).also { playerCache[id] = it }
			fun EventUser.findPlayer() =
					playerCache[id] ?: playerStorage.findOrCreateOne(TtvUser::helixUser / User::getId stores id, name).also { playerCache[id] = it }
			
			on<RewardRedeemedEvent> {
				val title = redemption.reward.title
				if(title.startsWith(CURRENCY_SYMBOL)) {
					val amount =  title.substring(1).split(" ")[0].replace(",", "").toLong()
					publish(MintRequestEvent(redemption.user.findPlayer(), amount, this))
				}
			}
			
			on<Twitch4jCommandEvent> {
				val argsIndex = command.indexOf(" ")
				publish(ChatCommandEvent(
						user.findPlayer(),
						command.substring(0, if(argsIndex > 0) argsIndex else command.length),
						if(argsIndex > 0) command.substring(argsIndex + 1) else "",
						{ respondToUser("@${user.name}: $it") },
						this)
				)
			}
			
			on<TtvWatchtimeEvent.Online> {
				log.info("Watching live: ${users.map{it.displayName}}")
				users.forEach { ttvUserStorage.findOrCreateOne(it).onlineMinutes += time }
			}
			on<TtvWatchtimeEvent.Offline> {
				log.info("In chat while offline: ${users.map{it.displayName}}")
				users.forEach { ttvUserStorage.findOrCreateOne(it).offlineMinutes += time }
			}
			on<TtvWatchtimeEvent.Guest> {
				log.info("In ${hostRecord.targetLogin}'s chat: ${users.map{it.displayName}}")
				users.forEach {ttvUserStorage.findOrCreateOne(it).guestMinutes += time }
			}
			on<TtvWatchtimeEvent.Host> {
				log.info("In ${hostRecord.hostLogin}'s chat while hosting ${hostRecord.targetLogin}: ${users.map{it.displayName}}")
				users.forEach { ttvUserStorage.findOrCreateOne(it).hostMinutes += time }
			}
			
		}

)