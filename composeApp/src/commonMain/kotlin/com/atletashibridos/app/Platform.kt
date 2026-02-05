package com.atletashibridos.app

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform