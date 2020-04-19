@file:Suppress("UNUSED")

import com.waridley.textroid.api.game.*

object Tallon_IV : Planet("A Wanderer-class Planet that was impacted by a Leviathan from Phaaze") {
	object Overworld: Region("Mostly lush jungle deluged by constant rain") {
		object North_Area: Area("The main area in the Tallon Overworld") {
			object Landing_Site: Room("The landing site of Samus's ship",
			                          BlueDoor(N, Alcove),
			                          BlueDoor(NW, Gully),
			                          BlueDoor(W, Canyon_Cavern),
			                          BlueDoor(S, Temple_Hall),
			                          BlueDoor(E, Waterfall_Cavern)) {
				object Samus_Ship
				object small_tunnel: MorphBallTunnel {
					val missile_expansion = MissileExpansion()
				}
			}
			object Alcove: Room("Contains the Space Jump Boots", BlueDoor(S, Landing_Site)) {
				object Space_Jump_Boots: Ability()
			}
			object Gully: Room("A narrow hallway", BlueDoor(E, Landing_Site), BlueDoor(W, Tallon_Canyon)) {
			
			}
			object Canyon_Cavern: Room("Connects Tallon Canyon and the Landing Site",
			                           BlueDoor(E, Landing_Site), BlueDoor(W, Tallon_Canyon)) {
				
			}
			object Temple_Hall: Room("A narrow hallway leading towards the impact crater",
			                         BlueDoor(N, Landing_Site), DeadEnd(S) /*Temple Security Station*/) {
				
			}
			object Waterfall_Cavern: Room("Waterfall Cavern",
			                              BlueDoor(W, Landing_Site), DeadEnd(E) /*Frigate Crash Site, gray door*/) {
				
			}
			
			object Tallon_Canyon: Room("Tallon Canyon",
			                           DeadEnd(N), //Transport Tunnel A
			                           DeadEnd(W), //Root Tunnel
			                           BlueDoor(NE, Gully),
			                           BlueDoor(SE, Canyon_Cavern)) {
				
			}
			object Root_Cave: Room("Root Cave") {
			
			}
		}
		
		object Artifact_Temple: Area("")
		object Crashed_Frigate_Orpheon: Area("")
		object South_Area: Area("")
	}
	
	object Chozo_Ruins: Region("Ancient ruins of the once-prosperous civilization of the Chozo")
	
	object Magmoor_Caverns:
			Region("Underground caverns of Tallon IV, filled with dangerous lava lakes and hostile creatures")
	
	object Phendrana_Drifts: Region("Frozen wastes of Tallon IV's mountaintops")
	
	object Phazon_Mines: Region("The Space Pirate headquarters of Tallon IV")
	
	object Impact_Crater: Region("The heart of the Phazon infestation, protected by the corrupted Metroid Prime")
}

Tallon_IV //Return to script invoker
