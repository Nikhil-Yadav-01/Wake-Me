package com.nikhil.wakeme.util

import android.content.Context
import com.nikhil.wakeme.data.AlarmDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

suspend fun exportAlarmsToJson(context: Context, outFile: File) {
    withContext(Dispatchers.IO) {
        val db = AlarmDatabase.getInstance(context)
        val list = db.alarmDao().getEnabledAlarmsList()
        val arr = JSONArray()
        for (a in list) {
            val o = JSONObject()
            o.put("id", a.id)
            o.put("timeMillis", a.timeMillis)
            o.put("label", a.label)
            o.put("snoozeMinutes", a.snoozeMinutes)
            o.put("autoSnoozeEnabled", a.autoSnoozeEnabled)
            o.put("autoSnoozeMaxCycles", a.autoSnoozeMaxCycles)
            arr.put(o)
        }
        outFile.writeText(arr.toString())
    }
}

suspend fun importAlarmsFromJson(context: Context, inFile: File) {
    withContext(Dispatchers.IO) {
        val txt = inFile.readText()
        val arr = JSONArray(txt)
        val db = AlarmDatabase.getInstance(context)
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val a = com.nikhil.wakeme.data.AlarmEntity(
                timeMillis = o.getLong("timeMillis"),
                label = o.optString("label", null),
                snoozeMinutes = o.optInt("snoozeMinutes", 5),
                autoSnoozeEnabled = o.optBoolean("autoSnoozeEnabled", true),
                autoSnoozeMaxCycles = o.optInt("autoSnoozeMaxCycles", 0)
            )
            db.alarmDao().insert(a)
        }
    }
}
