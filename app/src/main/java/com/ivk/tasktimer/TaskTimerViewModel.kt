package com.ivk.tasktimer

import android.app.Application
import android.content.ContentValues
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

private const val TAG = "TaskTimerViewModel"

class TaskTimerViewModel(application: Application) : AndroidViewModel(application) {

    private val contentObserver = object : ContentObserver(Handler()) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            Log.d(TAG, "contentObserver.onChange: called. uri is $uri")
            loadTasks()

        }
    }

    private var currentTiming: Timing? = null

    private val databaseCursor = MutableLiveData<Cursor>()
    val cursor: LiveData<Cursor>
    get() = databaseCursor

    init {
        Log.d(TAG, "TaskTimerViewModel: created")
        getApplication<Application>().contentResolver.registerContentObserver(TasksContract.CONTENT_URI,
                true, contentObserver)
        loadTasks()
    }

    private fun loadTasks() {
        val projection = arrayOf(TasksContract.Columns.ID,
                TasksContract.Columns.TASK_NAME,
                TasksContract.Columns.TASK_DESCRIPTION,
                TasksContract.Columns.TASK_SORT_ORDER)
        // <order by> Tasks.SortOrder, Tasks.Name
        val sortOrder = "${TasksContract.Columns.TASK_SORT_ORDER}, ${TasksContract.Columns.TASK_NAME}"

        GlobalScope.launch {
            val cursor = getApplication<Application>().contentResolver.query(
                TasksContract.CONTENT_URI,
                projection, null, null,
                sortOrder
            )
            databaseCursor.postValue(cursor)
        }
    }

    fun saveTask(task: Task): Task {
        val values = ContentValues()

        if (task.name.isNotEmpty()) {
            // Don't save a task with no name
            values.put(TasksContract.Columns.TASK_NAME, task.name)
            values.put(TasksContract.Columns.TASK_DESCRIPTION, task.description)
            values.put(TasksContract.Columns.TASK_SORT_ORDER, task.sortOrder)   //defaults to zero if empty

            if (task.id == 0L) {
                GlobalScope.launch {
                    Log.d(TAG, "saveTask: adding new task")
                    val uri = getApplication<Application>().contentResolver?.insert(TasksContract.CONTENT_URI, values)
                    if (uri != null) {
                        task.id = TasksContract.getId(uri)
                        Log.d(TAG, "saveTAsk: new id is ${task.id}")
                    }
                }
            } else {
                // task has an id, so we're updating
                GlobalScope.launch {
                    Log.d(TAG,"saveTAsk: updating task")
                    getApplication<Application>().contentResolver?.update(TasksContract.buildUriFromId(task.id), values, null, null)
                }
            }
        }
        return task
    }

    fun deleteTask(taskId: Long) {
        Log.d(TAG, "Deleting task")
        GlobalScope.launch {
            getApplication<Application>().contentResolver?.delete(TasksContract.buildUriFromId(taskId), null, null)
        }
    }

    fun timeTask(task: Task) {
        Log.d(TAG, "timeTask: called")
        // Use local variable, to allow smart casts
        val timingRecord = currentTiming

        if (timingRecord == null) {
            //no task being timed, start timing the new task
            val newTiming = Timing(task.id)
            saveTiming(newTiming)
            currentTiming = newTiming
        } else {
            // We have a task being timed, so save it
            timingRecord.setDuration()
            saveTiming(timingRecord)

            if (task.id == timingRecord.taskId) {
                // the current task was tapped a second time, stip timing
                currentTiming = null
            } else {
                // a new task is being timed
                val newTiming = Timing(task.id)
                saveTiming(newTiming)
                currentTiming = newTiming
            }
        }
    }

    private fun saveTiming(currentTiming: Timing) {
        Log.d(TAG, "saveTiming: called")

        // Are we updating, or inserting a new row?
        val inserting = (currentTiming.duration == 0L)

        val values = ContentValues().apply {
            if (inserting) {
                put(TimingsContract.Columns.TIMING_TASK_ID, currentTiming.taskId)
                put(TimingsContract.Columns.TIMING_START_TIME, currentTiming.startTime)
            }
            put(TimingsContract.Columns.TIMING_DURATION, currentTiming.duration)
        }

        GlobalScope.launch {
            if (inserting) {
                val uri = getApplication<Application>().contentResolver.insert(TimingsContract.CONTENT_URI, values)
                if (uri != null) {
                    currentTiming.id = TimingsContract.getId(uri)
                }
            } else {
                getApplication<Application>().contentResolver.update(TimingsContract.buildUriFromId(currentTiming.taskId), values, null, null)
            }
        }
    }

    override fun onCleared() {
        Log.d(TAG, "onCleared: called")
        getApplication<Application>().contentResolver.unregisterContentObserver(contentObserver)
    }
}