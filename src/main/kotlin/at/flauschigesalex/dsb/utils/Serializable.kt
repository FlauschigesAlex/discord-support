package at.flauschigesalex.dsb.utils

import at.flauschigesalex.lib.base.file.JsonManager

abstract class Serializable {
    abstract fun toJson(): JsonManager
    final override fun toString(): String = toJson().toString()
}