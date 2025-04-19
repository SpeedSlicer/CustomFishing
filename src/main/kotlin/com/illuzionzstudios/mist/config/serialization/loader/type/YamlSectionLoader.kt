package com.illuzionzstudios.mist.config.serialization.loader.type

import com.illuzionzstudios.mist.config.ConfigSection
import com.illuzionzstudios.mist.config.serialization.loader.ObjectLoader

abstract class YamlSectionLoader<T>(load: ConfigSection?) : ObjectLoader<T, ConfigSection?>(load)