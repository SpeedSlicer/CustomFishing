package com.illuzionzstudios.mist.scheduler

import com.illuzionzstudios.mist.Logger.Companion.displayError
import com.illuzionzstudios.mist.Logger.Companion.severe
import com.illuzionzstudios.mist.Mist
import com.illuzionzstudios.mist.plugin.SpigotPlugin
import com.illuzionzstudios.mist.scheduler.rate.Async
import com.illuzionzstudios.mist.scheduler.rate.Rate
import com.illuzionzstudios.mist.scheduler.rate.Sync
import com.illuzionzstudios.mist.scheduler.timer.PresetCooldown
import com.illuzionzstudios.mist.util.ReflectionUtil
import java.lang.reflect.*
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicLong
import java.util.function.Consumer

/**
 * An instance of a scheduler that ticks objects. This will handle
 * safely ticking objects in order to avoid errors and leaks. To use this
 * we can directly call methods to delay tasks or run tasks on certain threads,
 * or we can make use of the [Sync] and [Async] annotations.
 *
 * These [Annotation] can annotate either [Class] [Field] or [Method]
 * Below is explained what each does
 *
 * [Class] If is an instance of [Tickable], invokes with a set rate
 * [Field] If this is an instance of [Tickable], will be invoked with set rate
 * [Method] The method will be invoked with a set rate
 *
 * When invoking a [Tickable] object, if the field is an instance of [Iterable]
 * or [Map], will check the elements for [Tickable] and invoke those
 */
abstract class MinecraftScheduler {

    /**
     * Start up our scheduler
     * Called in the [SpigotPlugin.onEnable]
     */
    fun initialize() {
        INSTANCE = this
        SYNC_SERVICE_REGISTRATION = Collections.newSetFromMap(ConcurrentHashMap())
        start()
    }

    /**
     * Stop all tickers
     * Called in the [SpigotPlugin.onDisable]
     */
    fun stopInvocation() {
        stop()
        SYNC_SERVICE_REGISTRATION!!.clear()
    }

    /**
     * Implemented to start the scheduler
     */
    protected abstract fun start()

    /**
     * Implemented to stop the scheduler
     */
    protected abstract fun stop()

    /**
     * This is the master method to step a tick (or heartbeat) for all our [SynchronizationService]
     * Ticks all of a certain Sync type
     *
     * @param type Either [Sync] or [Async] to tick
     * @param <A>  An [Annotation] to tick
    */
    protected fun <A : Annotation?> heartbeat(type: Class<A>) {
        for (service in SYNC_SERVICE_REGISTRATION!!) {
            for (element in service.elements) {
                // Make sure matches rate type
                if (element.synchronizationClass != type) {
                    continue
                }

                // Lets get all the refresh services

                // Use synchronous ticks to check if rate has elapsed so that if the
                // server thread blocks will still account for the time that we missed

                // Check timer and it's ready
                if (type == Sync::class.java && element.timer.isReady
                    || type == Async::class.java && element.timer.isReadyRealTime
                ) {
                    element.timer.go()

                    // Invoke method
                    if (element.`object` is Method) {
                        val method = element.`object`

                        // Determine if method should be fired based on the rate of refresh
                        val start = System.currentTimeMillis()

                        // Call method
                        try {
                            method.invoke(service.source)
                        } catch (e: Exception) {
                            displayError(e, "Error in internal plugin scheduler")
                        }

                        // Took too long
                        if (System.currentTimeMillis() - start > Mist.TIME_WARNING_THRESHOLD
                            && type == Sync::class
                        ) {
                            severe("WARNING: Synchronization block took way too long to invoke! (" + (System.currentTimeMillis() - start) + "ms)")
                            severe("Block " + method.name + "() in " + service.source.javaClass)
                        }
                    } else {
                        // Invoke field
                        try {
                            val `object` =
                                if (element.`object` is Field) element.`object`[service.source] else service.source
                            if (`object` != null) {
                                if (`object` is Tickable) {
                                    safelyTick(`object`)
                                } else if (`object` is Iterable<*> || `object` is Map<*, *>) {
                                    var iterable: Iterable<*>? = null
                                    if (`object` is Collection<*>) {
                                        iterable = `object`
                                    } else if (`object` is Map<*, *>) {
                                        iterable = `object`.values
                                    }

                                    // If objects are maps that contain tickable objects
                                    // invoke those
                                    iterable?.forEach { fieldElement: Any? ->
                                        if (fieldElement is Tickable) {
                                            safelyTick(fieldElement as Tickable?)
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            displayError(e, "Interrupted synchronization invocation: in " + service.source.javaClass)
                        }
                    }
                }
            }
        }

        // Increment current ticks passed
        if (type == Sync::class.java) {
            synchronousTicks.getAndIncrement()
        }
    }

    /**
     * Safely runs tick without throwing an exception
     *
     * @param tickable Tickable object
     * @return Returns if tick operation executed successfully
     */
    fun safelyTick(tickable: Tickable?): Boolean {
        try {
            if (tickable == null) {
                return false
            }
            tickable.tick()
        } catch (e: Exception) {
            displayError(e, "Interrupted tick call!:")
        }
        return true
    }

    /**
     * @param occurrence The time in ticks we want to check
     * @return If time has elapsed
     */
    fun hasElapsed(occurrence: Double): Boolean {
        return synchronousTicks.get() % occurrence == 0.0
    }

    /**
     * Checks if method is being ran on server thread
     */
    abstract fun validateMainThread()

    /**
     * Checks if method is not being ran on server thread
     */
    abstract fun validateNotMainThread()

    /**
     * Submits task into sync scheduler
     *
     * @param runnable The task
     */
    fun synchronize(runnable: Runnable?): Int {
        return synchronize(runnable, 0)
    }

    /**
     * Submits task into async scheduler
     *
     * @param runnable The task
     */
    fun desynchronize(runnable: Runnable?): Int {
        return desynchronize(runnable, 0)
    }

    /**
     * Submits task into sync scheduler
     *
     * @param runnable The task
     * @param time     The delay time
     */
    abstract fun synchronize(runnable: Runnable?, time: Long): Int

    /**
     * Submits task into async scheduler
     *
     * @param runnable The task
     * @param time     The delay time
     */
    abstract fun desynchronize(runnable: Runnable?, time: Long): Int

    /**
     * Asynchronous callback tool
     *
     * @param callable The future task
     * @param consumer The task callback
     * @param <T>      The type returned
    */
    abstract fun <T> desynchronize(callable: Callable<T>?, consumer: Consumer<Future<T>>?)

    /**
     * Cancel a running task with certain id
     *
     * @param id The id of the task to cancel
     */
    abstract fun stopTask(id: Int)

    /**
     * Registers a class as refresh service listener
     *
     * @param source This can be any object you want
     */
    fun registerSynchronizationService(source: Any) {
        SYNC_SERVICE_REGISTRATION!!.add(SynchronizationService(source))
    }

    /**
     * Removes a synchronized service
     *
     * @param source This can be any object you want
     */
    fun dismissSynchronizationService(source: Any?) {
        SYNC_SERVICE_REGISTRATION!!.removeIf { service: SynchronizationService -> service.source == source }
    }

    /**
     * Cached Synchronization Element
     * This contains the method/field data that will be used such
     * as the ticking rate, invoked object
     */
    protected class SynchronizedElement<out A : Annotation?>(
        /**
         * The rate to tick by
         */
        protected val rate: Rate?,
        /**
         * The object to tick
         */
        val `object`: Any,
        /**
         * The class of the tick rate type
         */
        val synchronizationClass: Class<out A>
    ) {
        /**
         * Timer to tick object
         */
        val timer: PresetCooldown = PresetCooldown((rate?.time?.div(50))!!.toInt())
    }

    /**
     * A Cached Synchronization Service that should be ticked
     */
    protected class SynchronizationService(
        /**
         * The object that is being ticked
         */
        var source: Any
    ) {
        /**
         * Elements to be ticked
         */
        var elements: MutableSet<SynchronizedElement<*>> = HashSet()

        private fun getAnnotations(): Array<Class<out Annotation>> {
            return arrayOf(Sync::class.java, Async::class.java)
        }

        @Throws(NoSuchMethodException::class, InvocationTargetException::class, IllegalAccessException::class)
        private fun <A : Annotation> getElements(
            objects: Array<out AccessibleObject>,
            synchronizationClass: Class<A>,
            rate: Rate
        ): Set<SynchronizedElement<A>> {
            val elements: MutableSet<SynchronizedElement<A>> = HashSet()
            for (`object` in objects) {
                // Set them to public if they are private for obvious reasons
//                if (!`object`.canAccess(`object`)) {
//                    `object`.isAccessible = true;
//                }
                val declaredRate = getRate(synchronizationClass, `object`)
                if (declaredRate != null && declaredRate == rate) {
                    elements.add(SynchronizedElement(rate, `object`, synchronizationClass))
                }
            }

            return elements
        }

        @Throws(NoSuchMethodException::class, InvocationTargetException::class, IllegalAccessException::class)
        private fun <A : Annotation> getRate(synchronizationClass: Class<A>, element: AnnotatedElement): Rate? {
            if (!element.isAnnotationPresent(synchronizationClass)) {
                return null
            }

            // Get the annotation itself //
            val annotation = element.getAnnotation(synchronizationClass)

            // Get declared rate of refresh value is instant by default //
            val getRate: Method = annotation?.javaClass?.getDeclaredMethod("rate")!!

//            if (!getRate.canAccess(getRate)) {
//                getRate.isAccessible = true;
//            }
            return getRate.invoke(annotation) as Rate
        }

        init {
            try {
                // LOAD ELEMENTS //
                for (clazz in getAnnotations()) {
                    if (source.javaClass.isAnnotationPresent(clazz)) {
                        val rate = getRate(clazz, source.javaClass.superclass)
                        elements.add(SynchronizedElement(rate, source, clazz))
                    } else if (source.javaClass.superclass.isAnnotationPresent(clazz)) {
                        val rate = getRate(clazz, source.javaClass.superclass)
                        elements.add(SynchronizedElement(rate, source, clazz))
                    }
                    for (rate in Rate.values()) {
                        // LOAD METHODS //
                        elements.addAll(getElements(ReflectionUtil.getAllMethods(source.javaClass), clazz, rate))

                        // LOAD FIELDS //
                        elements.addAll(getElements(ReflectionUtil.getAllFields(source.javaClass), clazz, rate))
                    }
                }
            } catch (e: NoSuchMethodException) {
                e.printStackTrace()
            } catch (e: InvocationTargetException) {
                e.printStackTrace()
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        /**
         * These are the amount of synchronized ticks the application
         * has undergone. Async ticks are not counted as this will keep track
         * of total application time
         */
        private val synchronousTicks = AtomicLong()

        /**
         * This holds the set of registered synchronization services that are
         * currently ticking. Objects must be dismissed after being used
         */
        @Volatile
        private var SYNC_SERVICE_REGISTRATION: MutableSet<SynchronizationService>? = null

        /**
         * Instance of the [MinecraftScheduler]
         */
        private var INSTANCE: MinecraftScheduler? = null

        /**
         * @return Our instance of the [MinecraftScheduler]
         */
        fun get(): MinecraftScheduler? {
            return INSTANCE
        }

        /**
         * @return The current amount of ticks the application has undergone
         */
        val currentTick: Long
            get() = synchronousTicks.get()
    }
}