package com.wanderwildwood.tancho

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.io.IOException

class ModelFilesTest {
    @Test fun `sha256 of a known stream`() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            ModelFiles.sha256 { "abc".byteInputStream() },
        )
    }

    @Test fun `an unreadable stream hashes to nothing and is never accepted`() {
        val sha = ModelFiles.sha256 { throw IOException("gone") }
        assertEquals("", sha)
        assertNotEquals(ModelFiles.MODEL_SHA256, sha)
        assertNotEquals(ModelFiles.META_MODEL_SHA256, sha)
    }

    @Test fun `the pinned hashes are sha-256`() {
        listOf(ModelFiles.MODEL_SHA256, ModelFiles.META_MODEL_SHA256)
            .forEach { assertTrue(Regex("[0-9a-f]{64}").matches(it)) }
    }

    /** The build stages the models into the assets; the hashes the app checks must be the ones it staged. */
    @Test fun `the models the build packs are the ones the app accepts`() {
        val dir = listOf(File("build/generated/model-assets"), File("app/build/generated/model-assets")).firstOrNull { it.isDirectory }
            ?: return
        assertEquals(ModelFiles.MODEL_SHA256, ModelFiles.sha256 { FileInputStream(File(dir, ModelFiles.MODEL)) })
        assertEquals(ModelFiles.META_MODEL_SHA256, ModelFiles.sha256 { FileInputStream(File(dir, ModelFiles.META_MODEL)) })
    }
}
