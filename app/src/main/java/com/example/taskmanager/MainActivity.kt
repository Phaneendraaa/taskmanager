package com.example.taskmanager

import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class Task(val id: String, val title: String, val done: Boolean)

enum class Filter(val label: String) {
    ALL("All"),
    ACTIVE("Active"),
    DONE("Done")
}

/** Holds the task list and saves it on this device (SharedPreferences). No backend. */
class TaskViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("tasks", Context.MODE_PRIVATE)

    val tasks = mutableStateListOf<Task>().apply { addAll(load()) }

    fun add(title: String) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        tasks.add(0, Task(UUID.randomUUID().toString(), clean, false))
        save()
    }

    fun toggle(id: String) {
        val i = tasks.indexOfFirst { it.id == id }
        if (i >= 0) {
            tasks[i] = tasks[i].copy(done = !tasks[i].done)
            save()
        }
    }

    fun delete(id: String) {
        tasks.removeAll { it.id == id }
        save()
    }

    fun clearDone() {
        tasks.removeAll { it.done }
        save()
    }

    private fun save() {
        val arr = JSONArray()
        tasks.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("title", it.title)
                    .put("done", it.done)
            )
        }
        prefs.edit().putString("list", arr.toString()).apply()
    }

    private fun load(): List<Task> {
        val raw = prefs.getString("list", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                Task(o.getString("id"), o.getString("title"), o.getBoolean("done"))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

class MainActivity : ComponentActivity() {
    private val vm: TaskViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                TaskScreen(vm)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(vm: TaskViewModel) {
    var text by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(Filter.ALL) }

    val visible = vm.tasks.filter {
        when (filter) {
            Filter.ALL -> true
            Filter.ACTIVE -> !it.done
            Filter.DONE -> it.done
        }
    }
    val remaining = vm.tasks.count { !it.done }

    val addTask = {
        vm.add(text)
        text = ""
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("My Tasks") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Add a task") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { addTask() })
                )
                Spacer(Modifier.width(8.dp))
                FilledIconButton(onClick = { addTask() }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add task")
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Filter.values().forEach { f ->
                    FilterChip(
                        selected = filter == f,
                        onClick = { filter = f },
                        label = { Text(f.label) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$remaining remaining",
                    style = MaterialTheme.typography.labelLarge
                )
                if (vm.tasks.any { it.done }) {
                    TextButton(onClick = { vm.clearDone() }) {
                        Text("Clear done")
                    }
                }
            }

            if (visible.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (vm.tasks.isEmpty()) "No tasks yet. Add one above." else "Nothing here.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(visible, key = { it.id }) { task ->
                        TaskRow(
                            task = task,
                            onToggle = { vm.toggle(task.id) },
                            onDelete = { vm.delete(task.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TaskRow(task: Task, onToggle: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = task.done, onCheckedChange = { onToggle() })
        Text(
            text = task.title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            textDecoration = if (task.done) TextDecoration.LineThrough else null
        )
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete task")
        }
    }
}
