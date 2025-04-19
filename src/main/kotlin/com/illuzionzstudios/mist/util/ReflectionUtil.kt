package com.illuzionzstudios.mist.util

import com.illuzionzstudios.mist.compatibility.ServerVersion
import com.illuzionzstudios.mist.exception.PluginException
import org.apache.commons.lang.ClassUtils
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.*

/**
 * A utils class for help with reflection. Useful for NMS and
 * other things we may need to do
 */

class ReflectionUtil {

    companion object {

        /**
         * The full package name for NMS
         */
        val NMS = "net.minecraft.server"

        /**
         * The package name for Craftbukkit
         */
        val CRAFTBUKKIT = "org.bukkit.craftbukkit"

        /**
         * Find a class in net.minecraft.server package, adding the version
         * automatically
         */
        fun getNMSClass(name: String): Class<*> {
            return lookupClass(NMS + "." + ServerVersion.getServerVersion() + "." + name)
        }

        /**
         * Find a class in org.bukkit.craftbukkit package, adding the version
         * automatically
         */
        fun getOBCClass(name: String): Class<*> {
            return lookupClass(CRAFTBUKKIT + "." + ServerVersion.getServerVersion() + "." + name)
        }

        /**
         * Set the static field to the given value
         */
        fun setStaticField(clazz: Class<*>, fieldName: String, fieldValue: Any) {
            try {
                val field = getDeclaredField(clazz, fieldName)
                field!![null] = fieldValue
            } catch (t: Throwable) {
                throw PluginException(t, "Could not set $fieldName in $clazz to $fieldValue")
            }
        }

        /**
         * Set the static field to the given value
         */
        fun setStaticField(`object`: Any, fieldName: String, fieldValue: Any) {
            try {
                val field = `object`.javaClass.getDeclaredField(fieldName)
                field.isAccessible = true
                field[`object`] = fieldValue
            } catch (t: Throwable) {
                throw PluginException(t, "Could not set $fieldName in $`object` to $fieldValue")
            }
        }

        /**
         * Convenience method for getting a static field content.
         */
        fun <T> getStaticFieldContent(clazz: Class<*>, field: String): T {
            return getFieldContent(clazz, field, null)
        }

        /**
         * Return a constructor for the given NMS class. We prepend the class name
         * with the [.NMS] so you only have to give in the name of the class.
         */
        fun getNMSConstructor(nmsClass: String, vararg params: Class<*>?): Constructor<*> {
            return getConstructor(getNMSClass(nmsClass), *params)
        }

        /**
         * Return a constructor for the given OBC class. We prepend the class name
         * with the OBC so you only have to give in the name of the class.
         */
        fun getOBCConstructor(obcClass: String, vararg params: Class<*>?): Constructor<*> {
            return getConstructor(getOBCClass(obcClass), *params)
        }

        /**
         * Return a constructor for the given fully qualified class path such as
         * org.mineacademy.boss.BossPlugin
         */
        fun getConstructor(classPath: String, vararg params: Class<*>?): Constructor<*> {
            val clazz = lookupClass(classPath)
            return getConstructor(clazz, *params)
        }

        /**
         * Return a constructor for the given class
         */
        fun getConstructor(clazz: Class<*>, vararg params: Class<*>?): Constructor<*> {
            return try {
                val constructor = clazz.getConstructor(*params)
                constructor.isAccessible = true
                constructor
            } catch (ex: ReflectiveOperationException) {
                throw PluginException(ex, "Could not get constructor of $clazz with parameters $params")
            }
        }

        /**
         * Get the field content
         */
        fun <T> getFieldContent(instance: Any, field: String): T {
            return getFieldContent(instance.javaClass, field, instance)
        }

        /**
         * Get the field content
         */
        fun <T> getFieldContent(clazz: Class<*>, field: String, instance: Any?): T {
            var clazz = clazz
            val originalClassName = clazz.simpleName
            do  // note: getDeclaredFields() fails if any of the fields are classes that cannot be loaded
                for (f in clazz.declaredFields) if (f.name == field) return getFieldContent(
                    f,
                    instance
                ) as T while (!clazz.superclass.also { clazz = it }
                    .isAssignableFrom(
                        Any::class.java
                    ))
            throw ReflectionException("No such field $field in $originalClassName or its superclasses")
        }

        /**
         * Get the field content
         */
        fun getFieldContent(field: Field, instance: Any?): Any {
            return try {
                field.isAccessible = true
                field[instance]
            } catch (e: ReflectiveOperationException) {
                throw ReflectionException(
                    "Could not get field " + field.name + " in instance " + (instance ?: field).javaClass.simpleName
                )
            }
        }

        /**
         * Get all fields from the class and its super classes
         */
        fun getAllFields(clazz: Class<*>): Array<Field> {
            var clazz = clazz
            val list: MutableList<Field> = ArrayList()
            do list.addAll(listOf(*clazz.declaredFields)) while (!clazz.superclass.also { clazz = it }
                    .isAssignableFrom(
                        Any::class.java
                    ))
            return list.toTypedArray()
        }

        /**
         * Gets the declared field in class by its name
         */
        fun getDeclaredField(clazz: Class<*>, fieldName: String?): Field? {
            try {
                val field = clazz.getDeclaredField(fieldName)
                field.isAccessible = true
                return field
            } catch (e: ReflectiveOperationException) {
                e.printStackTrace()
            }
            return null
        }

        /**
         * Gets a class method
         */
        fun getMethod(clazz: Class<*>, methodName: String, vararg args: Class<*>): Method? {
            for (method in clazz.methods) if (method.name == methodName && isClassListEqual(
                    args as Array<Class<*>>,
                    method.parameterTypes
                )
            ) {
                method.isAccessible = true
                return method
            }
            return null
        }

        // Compares class lists
        private fun isClassListEqual(first: Array<Class<*>>, second: Array<Class<*>>): Boolean {
            if (first.size != second.size) return false
            for (i in first.indices) if (first[i] != second[i]) return false
            return true
        }

        /**
         * Gets a class method
         */
        fun getMethod(clazz: Class<*>, methodName: String, args: Int): Method? {
            for (method in clazz.methods) if (method.name == methodName && args == method.parameterTypes.size) {
                method.isAccessible = true
                return method
            }
            return null
        }

        /**
         * Gets a class method
         */
        fun getMethod(clazz: Class<*>, methodName: String): Method? {
            for (method in clazz.methods) if (method.name == methodName) {
                method.isAccessible = true
                return method
            }
            return null
        }

        /**
         * Get all fields from the class and its super classes
         */
        fun getAllMethods(clazz: Class<*>): Array<Method> {
            var clazz = clazz
            val list: MutableList<Method> = ArrayList()
            do list.addAll(listOf(*clazz.declaredMethods)) while (!clazz.superclass.also { clazz = it }
                    .isAssignableFrom(
                        Any::class.java
                    ))
            return list.toTypedArray()
        }

        /**
         * Wrapper for Class.forName
         */
        fun <T> lookupClass(path: String, type: Class<T>?): Class<T> {
            return lookupClass(path) as Class<T>
        }

        /**
         * Wrapper for Class.forName
         */
        fun lookupClass(path: String): Class<*> {
            return try {
                Class.forName(path)
            } catch (ex: ClassNotFoundException) {
                throw ReflectionException("Could not find class: $path")
            }
        }

        /**
         * Makes a new instance of a class if constructor without parameters
         *
         * @param clazz The class instance to make
         * @return The newly created class
         */
        fun <T> instantiate(clazz: Class<T>): T {
            return try {
                val c = clazz.getDeclaredConstructor()
                c.isAccessible = true
                c.newInstance()
            } catch (e: ReflectiveOperationException) {
                throw ReflectionException("Could not make instance of: $clazz", e)
            }
        }

        /**
         * Makes a new instance of a class with arguments
         *
         * @param clazz  The class instance to make
         * @param params Parameters to create a new class
         * @return The newly created class
         */
        fun <T> instantiate(clazz: Class<T>, vararg params: Any): T {
            return try {
                val classes: MutableList<Class<*>> = ArrayList()
                for (param in params) {
                    Valid.checkNotNull(param, "Argument cannot be null when instatiating $clazz")
                    val paramClass: Class<*> = param.javaClass
                    classes.add(if (paramClass.isPrimitive) ClassUtils.wrapperToPrimitive(paramClass) else paramClass)
                }
                val c = clazz.getDeclaredConstructor(*classes.toTypedArray())
                c.isAccessible = true
                c.newInstance(*params)
            } catch (e: ReflectiveOperationException) {
                throw ReflectionException("Could not make instance of: $clazz", e)
            }
        }

        /**
         * Attempts to create a new instance from the given constructor and parameters
         *
         * @param <T>         The type of class to creator
         * @param constructor Constructor instance for class
         * @param params      Parameters to create a new class
         * @return The newly created class
        </T> */
        fun <T> instantiate(constructor: Constructor<T>, vararg params: Any?): T {
            return try {
                constructor.newInstance(*params)
            } catch (ex: ReflectiveOperationException) {
                throw PluginException(
                    ex,
                    "Could not make new instance of " + constructor + " with params: " + java.lang.String.join(
                        " ",
                        *params as Array<String?>
                    )
                )
            }
        }

        /**
         * Represents an exception during reflection operation
         */
        class ReflectionException : RuntimeException {
            constructor(msg: String?) : super(msg)
            constructor(msg: String?, ex: Exception?) : super(msg, ex)

            companion object {
                private const val serialVersionUID = 1L
            }
        }
    }
}