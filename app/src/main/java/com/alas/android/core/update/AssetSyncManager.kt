package com.alas.android.core.update

import android.content.Context
import com.alas.android.core.base.AlasLog
import org.json.JSONObject
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 数据同步管理器：从 ALAS 上游仓库拉取"自动化功能数据"到本地扩展目录。
 *
 * 同步的内容(对齐 ALAS 上游目录)：
 *  - `assets/<server>/<category>/*.png`  —— 各服玩法截图模板
 *  - `campaign/*`                          —— 地图数据(章节/活动海域)
 *  - 玩法相关 JSON/配置
 *
 * 下载后放入 `files/` 下的对应扩展目录。`core.base.ResourceManager` 在加载模板时
 * **优先读取扩展目录**，因此同步后的数据会覆盖内置 assets，实现"数据跟随上游"。
 *
 * 增量策略：记录上次同步的 upstream HEAD sha，变化时才重新拉取。
 */
class AssetSyncManager(
    private val context: Context,
    private val client: AlasUpstreamClient = AlasUpstreamClient(),
) {

    private val syncing = AtomicBoolean(false)

    /** 上次同步记录文件。 */
    private val metaFile: File = File(context.filesDir, "sync_meta.json")

    /** 上次同步的 HEAD sha。 */
    fun lastSyncedSha(): String? = readMeta()?.optString("head_sha")?.takeIf { it.isNotEmpty() }

    /** 扩展目录根(与 ResourceManager 约定一致)。 */
    private fun extRoot(): File = File(context.filesDir, "templates")

    val isSyncing: Boolean get() = syncing.get()

    /**
     * 执行同步。
     * @param servers 需要同步的服务器，如 ["cn","en"]。
     * @param progress 进度回调(已处理文件数, 总文件数, 当前文件名)。
     * @return 是否成功同步(HEAD 有变化)。
     */
    fun sync(
        servers: List<String>,
        progress: (done: Int, total: Int, name: String) -> Unit = { _, _, _ -> },
    ): Boolean {
        if (!syncing.compareAndSet(false, true)) return false
        return try {
            doSync(servers, progress)
        } finally {
            syncing.set(false)
        }
    }

    private fun doSync(
        servers: List<String>,
        progress: (done: Int, total: Int, name: String) -> Unit,
    ): Boolean {
        val head = client.headSha()
        if (head == null) {
            AlasLog.e("无法获取上游 HEAD，同步失败")
            throw IllegalStateException("无法获取 ALAS 上游 HEAD")
        }
        if (head == lastSyncedSha()) {
            AlasLog.i("数据已是最新(HEAD 未变化): ${head.take(8)}")
            return false
        }

        // 收集需要下载的文件
        val targets = mutableListOf<String>()
        servers.forEach { server ->
            targets += client.listFiles("assets/$server")
            targets += client.listFiles("assets/map_detection")
        }
        targets += client.listFiles("campaign")

        val total = targets.size
        var done = 0
        targets.forEach { rel ->
            val data = client.downloadRaw(rel) ?: return@forEach
            writeExtFile(rel, data)
            done++
            progress(done, total, rel)
        }

        // 记录本次 HEAD
        writeMeta(head)
        AlasLog.i("数据同步完成: $done 个文件 (HEAD ${head.take(8)})")
        return true
    }

    private fun writeExtFile(relPath: String, data: ByteArray) {
        // 把 assets/xxx 映射到 files/templates/xxx；campaign/ 保持原样
        val norm = if (relPath.startsWith("assets/")) relPath.removePrefix("assets/") else relPath
        val target = File(extRoot().parentFile, if (relPath.startsWith("assets/")) norm else relPath)
        target.parentFile?.mkdirs()
        target.writeBytes(data)
    }

    private fun readMeta(): JSONObject? {
        return try {
            JSONObject(metaFile.readText())
        } catch (e: Exception) {
            null
        }
    }

    private fun writeMeta(sha: String) {
        try {
            metaFile.writeText(JSONObject().put("head_sha", sha).toString())
        } catch (e: Exception) {
            AlasLog.e("写入同步记录失败", e)
        }
    }
}
