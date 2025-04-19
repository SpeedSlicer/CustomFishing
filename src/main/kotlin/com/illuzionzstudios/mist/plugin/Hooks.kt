package com.illuzionzstudios.mist.plugin

import com.illuzionzstudios.mist.controller.PluginController
import com.illuzionzstudios.mist.exception.PluginException
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import com.sk89q.worldedit.regions.Region
import com.sk89q.worldguard.protection.managers.RegionManager
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion
import com.sk89q.worldguard.protection.regions.ProtectedRegion
import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.util.function.Consumer


/**
 * External plugin hooks. Contains methods to interact with hooks
 */
object Hooks: PluginController {

    /**
     * PlaceholderAPI Enabled
     */
    var papiEnabled: Boolean = false
        private set

    /**
     * Worldguard hook
     */
    var worldguard: WorldGuardHook? = null
        private set

    override fun initialize(plugin: SpigotPlugin) {
        // Check if plugins loaded
        this.papiEnabled = Bukkit.getServer().pluginManager.getPlugin("PlaceholderAPI") != null
        if (Bukkit.getServer().pluginManager.getPlugin("WorldGuard") != null) {
            this.worldguard = WorldGuardHook()
        }
    }

    override fun stop(plugin: SpigotPlugin) {
    }

    /**
     * Replace placeholders if enabled
     */
    fun papiPlaceholders(string: String, player: Player): String = if (papiEnabled) PlaceholderAPI.setPlaceholders(player, string) else string

    class WorldGuardHook {
        private var legacy: Boolean = false

        fun getRegionsAt(loc: Location): List<String> {
            val list: MutableList<String> = ArrayList()
            getApplicableRegions(loc).forEach(Consumer { reg: ProtectedRegion ->
                val name: String = ChatColor.stripColor(reg.id) ?: ""
                if (!name.startsWith("__")) list.add(name)
            })
            return list
        }

        fun getRegion(name: String?): Region? {
            for (w in Bukkit.getWorlds()) {
                val rm = getRegionManager(w)
                if (legacy) try {
                    val regionMap = rm.javaClass.getMethod("getRegions").invoke(rm) as Map<*, *>
                    for (regObj in regionMap.values) {
                        if (regObj == null) continue
                        if (ChatColor.stripColor((regObj as ProtectedRegion).id).equals(name)) {
                            val clazz: Class<*> = regObj.javaClass
                            val getMax: Method = clazz.getMethod("getMaximumPoint")
                            val getMin: Method = clazz.getMethod("getMinimumPoint")
                            val regMax: Any = getMax.invoke(regObj)
                            val regMin: Any = getMin.invoke(regObj)
                            val vectorClass = Class.forName("com.sk89q.worldedit.BlockVector")
                            val getX: Method = vectorClass.getMethod("getX")
                            val getY: Method = vectorClass.getMethod("getY")
                            val getZ: Method = vectorClass.getMethod("getZ")
                            val locMax = Location(
                                w,
                                getX.invoke(regMax) as Double,
                                getY.invoke(regMax) as Double,
                                getZ.invoke(regMax) as Double
                            )
                            val locMin = Location(
                                w,
                                getX.invoke(regMin) as Double,
                                getY.invoke(regMin) as Double,
                                getZ.invoke(regMin) as Double
                            )
                            return CuboidRegion(BlockVector3.at(locMin.x, locMin.y, locMin.z), BlockVector3.at(locMax.x, locMax.y, locMax.z))
                        }
                    }
                } catch (t: Throwable) {
                    t.printStackTrace()
                    throw PluginException("Failed WorldEdit 6 legacy hook, see above & report")
                } else
                    for (reg in (rm as RegionManager).regions.values) {
                    if (reg?.id != null && ChatColor.stripColor(reg.id).equals(name)) {
                        val locMax: Location
                        val locMin: Location
                        val regMax: BlockVector3 = reg.maximumPoint
                        val regMin: BlockVector3 = reg.minimumPoint
                        locMax = Location(w, regMax.x.toDouble(), regMax.y.toDouble(),
                            regMax.z.toDouble()
                        )
                        locMin = Location(w, regMin.x.toDouble(), regMin.y.toDouble(),
                            regMin.z.toDouble()
                        )
                        return CuboidRegion(BlockVector3.at(locMin.x, locMin.y, locMin.z), BlockVector3.at(locMax.x, locMax.y, locMax.z))
                    }
                }
            }
            return null
        }

        val allRegions: List<String>
            get() {
                val list: MutableList<String> = ArrayList()
                for (w in Bukkit.getWorlds()) {
                    val rm = getRegionManager(w)
                    if (legacy) try {
                        val regionMap = rm.javaClass.getMethod("getRegions").invoke(rm) as Map<*, *>
                        var getId: Method? = null
                        for (regObj in regionMap.values) {
                            if (regObj == null) continue
                            if (getId == null) getId = regObj.javaClass.getMethod("getId")
                            val name: String = ChatColor.stripColor(getId?.invoke(regObj).toString()) ?: ""
                            if (!name.startsWith("__")) list.add(name)
                        }
                    } catch (t: Throwable) {
                        t.printStackTrace()
                        throw PluginException("Failed WorldEdit 6 legacy hook, see above & report")
                    } else {
                        (rm as RegionManager).regions.values.forEach { reg ->
                            if (reg?.id == null) return@forEach
                            val name: String = ChatColor.stripColor(reg.id) ?: ""
                            if (!name.startsWith("__")) list.add(name)
                        }
                    }
                }
                return list
            }

        private fun getApplicableRegions(loc: Location): Iterable<ProtectedRegion> {
            val rm = getRegionManager(loc.world ?: Bukkit.getWorlds()[0])
            return if (legacy) try {
                rm.javaClass.getMethod(
                    "getApplicableRegions",
                    Location::class.java
                ).invoke(rm, loc) as Iterable<ProtectedRegion>
            } catch (t: Throwable) {
                t.printStackTrace()
                throw PluginException("Failed WorldEdit 6 legacy hook, see above & report")
            } else (rm as RegionManager).getApplicableRegions(BlockVector3.at(loc.x, loc.y, loc.z))
        }

        private fun getRegionManager(w: World): Any {
            return if (legacy) try {
                Class.forName("com.sk89q.worldguard.bukkit.WGBukkit").getMethod("getRegionManager", World::class.java)
                    .invoke(null, w)
            } catch (t: Throwable) {
                t.printStackTrace()
                throw PluginException("Failed WorldGuard 6 legacy hook, see above & report")
            } else try {
                val bwClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitWorld")
                val bwClassNew: Constructor<*> = bwClass.getConstructor(World::class.java)
                var t = Class.forName("com.sk89q.worldguard.WorldGuard").getMethod("getInstance").invoke(null)
                t = t.javaClass.getMethod("getPlatform").invoke(t)
                t = t.javaClass.getMethod("getRegionContainer").invoke(t)
                t.javaClass.getMethod("get", Class.forName("com.sk89q.worldedit.world.World"))
                    .invoke(t, bwClassNew.newInstance(w))
            } catch (t: Throwable) {
                t.printStackTrace()
                throw PluginException("Failed WorldGuard hook, see above & report")
            }
        }

        init {
            val wg: Plugin? = Bukkit.getPluginManager().getPlugin("WorldGuard")
            if (wg != null) {
                legacy = !wg.description.version.startsWith("7")
            }
        }
    }
}