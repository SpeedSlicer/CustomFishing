package com.illuzionzstudios.mist.compatibility

import com.illuzionzstudios.mist.compatibility.ServerVersion.V
import com.illuzionzstudios.mist.util.TextUtil
import com.illuzionzstudios.mist.util.Valid
import org.bukkit.entity.Entity
import org.bukkit.inventory.meta.ItemMeta
import java.lang.reflect.Method
import java.util.*

/**
 * Some properties of things.
 * TODO: I think this needs to be tweaked does it actually have any use?
 */
enum class XProperty(
    private val requiredClass: Class<*>?,
    private val setterMethodType: Class<*>?
) {
    // ItemMeta
    /**
     * The unbreakable property of ItemMeta
     */
    UNBREAKABLE(ItemMeta::class.java, Boolean::class.javaPrimitiveType),  // Entity

    /**
     * The glowing entity property, currently only support white color
     */
    GLOWING(Entity::class.java, Boolean::class.javaPrimitiveType),

    /**
     * The AI navigator entity property
     */
    AI(Entity::class.java, Boolean::class.javaPrimitiveType),

    /**
     * The gravity entity property
     */
    GRAVITY(Entity::class.java, Boolean::class.javaPrimitiveType),

    /**
     * Silent entity property that controls if the entity emits sounds
     */
    SILENT(Entity::class.java, Boolean::class.javaPrimitiveType),

    /**
     * The god mode entity property
     */
    INVULNERABLE(Entity::class.java, Boolean::class.javaPrimitiveType);

    /**
     * Apply the property to the entity. Class must be compatible with [.requiredClass]
     *
     * Example: SILENT.apply(myZombieEntity, true)
     */
    fun apply(instance: Any, key: Any?) {
        Valid.checkBoolean(
            requiredClass!!.isAssignableFrom(instance.javaClass),
            this.toString() + " accepts " + requiredClass.simpleName + ", not " + instance.javaClass.simpleName
        )
        try {
            val m = getMethod(instance.javaClass)
            m.isAccessible = true
            m.invoke(instance, key)
        } catch (e: ReflectiveOperationException) {
            if (e is NoSuchMethodException && ServerVersion.olderThan(V.values()[0])) return
            e.printStackTrace()
        }
    }

    // Automatically returns the correct getter or setter method for class
    @Throws(ReflectiveOperationException::class)
    private fun getMethod(clazz: Class<*>): Method {
        // Check AI because method is setAI not setAi
        return clazz.getMethod(
            "set" + if (toString() == "AI") "AI" else TextUtil.formatText(
                toString().lowercase(Locale.getDefault()),
                true
            ), setterMethodType
        )
    }
}