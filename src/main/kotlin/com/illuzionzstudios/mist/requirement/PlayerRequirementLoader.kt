package com.illuzionzstudios.mist.requirement

import com.illuzionzstudios.mist.Logger
import com.illuzionzstudios.mist.config.ConfigSection
import com.illuzionzstudios.mist.config.serialization.loader.type.YamlSectionLoader

/**
 * Load a requirement from a config section
 */
class PlayerRequirementLoader(section: ConfigSection): YamlSectionLoader<PlayerRequirement>(section) {

    override fun save(): Boolean {
        TODO("Not yet implemented")
    }

    override fun loadObject(file: ConfigSection?): PlayerRequirement {
        var type: String? = file?.getString("type")
        // If to flip
        val invert = type?.startsWith("not ") ?: false
        if (invert) {
            type = type?.drop(4)
        }
        return PlayerRequirement(RequirementType.getFilter(type ?: "permission"), invert, file?.get("value"), file?.get("input"))
    }
}