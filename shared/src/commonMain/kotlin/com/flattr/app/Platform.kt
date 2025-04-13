package com.flattr.app

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform