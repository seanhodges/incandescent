package uk.co.seanhodges.incandescent.client

object IconResolver {
    fun getRoomImage(name: String): Int = when {
        name.containsOneOf("bath") -> R.drawable.room_bathroom
        name.containsOneOf("bed") -> R.drawable.room_bedroom
        name.containsOneOf("game", "gaming") -> R.drawable.room_gaming
        name.containsOneOf("garage") -> R.drawable.room_garage
        name.containsOneOf("hall", "landing") -> R.drawable.room_hall
        name.containsOneOf("kids", "play") -> R.drawable.room_kidsroom
        name.containsOneOf("kitchen") -> R.drawable.room_kitchen
        name.containsOneOf("living", "lounge", "snug") -> R.drawable.room_lounge
        name.containsOneOf("nursery", "baby") -> R.drawable.room_nursery
        name.containsOneOf("office") -> R.drawable.room_office
        name.containsOneOf("shower", "cloak") -> R.drawable.room_shower
        name.containsOneOf("toilet", "loo", "rest") -> R.drawable.room_toilet
        name.containsOneOf("utility", "cupboard") -> R.drawable.room_utility

        else -> R.drawable.room_unknown
    }

    fun getDeviceImage(name: String, type: String): Int = when {
        name.containsOneOf("light") -> R.drawable.device_lightbulb
        name.containsOneOf("lamp") -> R.drawable.device_table_lamp
        name.containsOneOf("car", "tesla") -> R.drawable.device_car
        name.containsOneOf("christmas", "xmas") -> R.drawable.device_christmas_tree
        name.containsOneOf("coffee") -> R.drawable.device_coffee_maker
        name.containsOneOf("fan", "aircon") -> R.drawable.device_fan
        name.containsOneOf("game", "entertainment") -> R.drawable.device_games_console
        name.containsOneOf("guitar") -> R.drawable.device_guitar
        name.containsOneOf("hair") -> R.drawable.device_hair_dryer
        name.containsOneOf("kettle") -> R.drawable.device_kettle
        name.containsOneOf("microwave") -> R.drawable.device_microwave
        name.containsOneOf("bike", "moped") -> R.drawable.device_motorbike
        name.containsOneOf("socket", "plug") -> R.drawable.device_socket
        name.containsOneOf("thermostat", "heating") -> R.drawable.device_thermostat
        name.containsOneOf("tv", "television") -> R.drawable.device_tv
        name.containsOneOf("music", "stereo") -> R.drawable.device_music_player
        name.containsOneOf("office") -> R.drawable.device_office_lamp

        type == "light" -> R.drawable.device_lightbulb
        type == "socket" -> R.drawable.device_socket
        else -> R.drawable.device_unknown
    }

    private fun String.containsOneOf(vararg text: String): Boolean {
        text.forEach {
            if (this.contains(it, ignoreCase = true)) {
                return true
            }
        }
        return false
    }
}