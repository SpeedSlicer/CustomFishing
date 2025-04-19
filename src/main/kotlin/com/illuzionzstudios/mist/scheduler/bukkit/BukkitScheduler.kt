package com.illuzionzstudios.mist.scheduler.bukkit

import com.illuzionzstudios.mist.plugin.SpigotPlugin
import com.illuzionzstudios.mist.scheduler.MinecraftScheduler
import com.illuzionzstudios.mist.scheduler.rate.Async
import com.illuzionzstudios.mist.scheduler.rate.Sync
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitRunnable
import java.util.concurrent.Callable
import java.util.concurrent.Future
import java.util.concurrent.FutureTask
import java.util.function.Consumer

/**
 * An instance of a [MinecraftScheduler] that handles ticking objects
 * Simply implements our methods that provide functionality
 */
class BukkitScheduler(
    /**
     * Instance of plugin to tick
     */
    val plugin: SpigotPlugin?
) : MinecraftScheduler() {

    /**
     * Ids of the schedulers
     */
    private var SYNC_SCHEDULER = -1
    private var ASYNC_SCHEDULER = -1

    public override fun start() {
        // The Bukkit SYNC scheduler thread
        SYNC_SCHEDULER = plugin!!.server.scheduler.scheduleSyncRepeatingTask(plugin, {
            heartbeat(
                Sync::class.java
            )
        }, 0L, 0L)

        // The Bukkit ASYNC scheduler
        ASYNC_SCHEDULER = plugin.server.scheduler.scheduleAsyncRepeatingTask(plugin, {
            heartbeat(
                Async::class.java
            )
        }, 0L, 0L)
    }

    public override fun stop() {
        // Stop invocation
        stopTask(SYNC_SCHEDULER)
        stopTask(ASYNC_SCHEDULER)
    }

    override fun stopTask(id: Int) {
        plugin!!.server.scheduler.cancelTask(id)
    }

    override fun validateMainThread() {
        if (!Bukkit.isPrimaryThread()) {
            throw RuntimeException("This method must be called on main server thread")
        }
    }

    override fun validateNotMainThread() {
        if (Bukkit.isPrimaryThread()) {
            throw RuntimeException("This method must not be called on the main server thread")
        }
    }

    override fun synchronize(runnable: Runnable?, time: Long): Int {
        return plugin!!.server.scheduler.scheduleSyncDelayedTask(plugin, runnable!!, time)
    }

    override fun desynchronize(runnable: Runnable?, time: Long): Int {
        return object : BukkitRunnable() {
            override fun run() {
                runnable?.run()
            }
        }.runTaskLaterAsynchronously(plugin!!, time).taskId
    }

    override fun <T> desynchronize(callable: Callable<T>?, consumer: Consumer<Future<T>>?) {
        // FUTURE TASK //
        val task = FutureTask(callable)

        // BUKKIT'S ASYNC SCHEDULE WORKER
        object : BukkitRunnable() {
            override fun run() {
                // RUN FUTURE TASK ON THREAD //
                task.run()
                object : BukkitRunnable() {
                    override fun run() {
                        // ACCEPT CONSUMER //
                        consumer?.accept(task)
                    }
                }.runTask(plugin!!)
            }
        }.runTaskAsynchronously(plugin!!)
    }
}