package com.example.pythagoros.backend

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {
    val config = BackendConfig.fromEnvironment()
    embeddedServer(
        Netty,
        port = config.port,
        host = config.host,
    ) {
        backendModule(config)
    }.start(wait = true)
}
