package com.chenjili.chessgame

import com.chenjili.chessgame.pages.chess.data.StockfishAbiResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StockfishAbiResolverTest {

    @Test
    fun resolve_prefersFirstSupportedAbiWithBundledEngine() {
        val resolver = StockfishAbiResolver(
            supportedAbis = arrayOf("x86_64", "arm64-v8a", "armeabi-v7a")
        )

        val resolved = resolver.resolve()

        requireNotNull(resolved)
        assertEquals("arm64-v8a", resolved.abi)
        assertEquals("stockfish/arm64-v8a/stockfish", resolved.assetPath)
    }

    @Test
    fun resolve_returnsNullWhenNoSupportedAssetExists() {
        val resolver = StockfishAbiResolver(
            supportedAbis = arrayOf("x86", "x86_64")
        )

        assertNull(resolver.resolve())
    }
}

