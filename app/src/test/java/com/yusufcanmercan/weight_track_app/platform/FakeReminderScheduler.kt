package com.yusufcanmercan.weight_track_app.platform

/** Alarm kurmak yerine kaç kez istendiğini sayar. */
class FakeReminderScheduler : ReminderScheduler {
    var syncCount = 0
        private set

    override suspend fun sync() {
        syncCount++
    }
}
