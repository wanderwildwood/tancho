package com.wanderwildwood.tancho

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.text.TextMMD

/**
 * What was just deleted, held until the reader moves on. Undo takes it back; anything else
 * (another delete, leaving the screen) makes the delete final. Only one thing is held: a second
 * delete settles the first.
 */
class UndoWindow<T> {
    var held: T? = null
        private set

    /** Holds [item] and returns what was held before it, which is now final. */
    fun hold(item: T): T? {
        val before = held
        held = item
        return before
    }

    /** Takes the held item back out, for Undo or for settling it; null when nothing is held. */
    fun release(): T? {
        val out = held
        held = null
        return out
    }
}

/** "... was deleted." and an Undo button along the foot, the shape used across these apps. */
@Composable
fun UndoStrip(text: String, onUndo: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        TextMMD(text = text, style = MaterialTheme.typography.bodyMedium)
        OutlinedButtonMMD(onClick = onUndo, modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(48.dp)) {
            TextMMD(text = stringResource(R.string.deleted_undo), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
