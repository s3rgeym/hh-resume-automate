// Можно использовать для хранения того же клиента
package com.github.s3rgeym.hh_resume_automate.registry

object Registry {
    private val registryMap = mutableMapOf<String, Any>()

    fun register(name: String, value: Any) {
        registryMap[name] = value
    }

    fun <T> get(name: String): T? {
        return registryMap[name] as? T
    }
}