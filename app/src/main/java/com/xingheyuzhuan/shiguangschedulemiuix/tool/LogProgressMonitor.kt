package com.xingheyuzhuan.shiguangschedulemiuix.tool

import org.eclipse.jgit.lib.ProgressMonitor

class LogProgressMonitor(private val onLog: (String) -> Unit) : ProgressMonitor {

    private var currentTotalWork = 0
    private var currentCompleted = 0
    private var currentTitle = ""
    private var lastNotifiedPercentage = -1 // 记录上一次通知时的百分比

    override fun start(totalTasks: Int) {
        onLog("[Git] 开始同步流程...")
    }

    override fun beginTask(title: String, totalWork: Int) {
        this.currentTitle = title
        this.currentTotalWork = totalWork
        this.currentCompleted = 0
        this.lastNotifiedPercentage = -1

        onLog("正在执行: $title")
    }

    override fun update(completed: Int) {
        this.currentCompleted += completed

        if (currentTotalWork > 0) {
            val p = (this.currentCompleted * 100) / currentTotalWork
            // 只有当进度百分比的十位数发生变化，或者是达到100%时才打印
            if (p / 10 > lastNotifiedPercentage / 10 || p == 100) {
                lastNotifiedPercentage = p
                onLog("  - 进度: $p%")
            }
        } else {
            // 对于未知总量的任务，每处理 500 个单位提示一次
            if (this.currentCompleted % 500 == 0) {
                onLog("  - 已处理: $currentCompleted")
            }
        }
    }

    override fun endTask() {
        // 结束时不再额外打印，保持日志简洁
    }

    override fun isCancelled(): Boolean = false
}