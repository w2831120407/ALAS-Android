package com.alas.android.core.update

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * 与 ALAS 上游仓库交互的最小客户端(仅使用 GitHub 公开 API/raw)。
 *
 * 用途：
 *  - 获取上游仓库某个目录的文件清单(GitHub git/trees API)
 *  - 下载 raw 文件(模板 / 地图数据 / 玩法 JSON)
 *
 * 上游仓库默认：`LmeSzinc/AzurLaneAutoScript`(master 分支)。
 */
class AlasUpstreamClient(
    private val owner: String = "LmeSzinc",
    private val repo: String = "AzurLaneAutoScript",
    private val branch: String = "master",
) {

    /**
     * 递归获取仓库内 [path] 下的所有 blob 文件路径。
     * 使用 GitHub 的 git/trees API，`recursive=1` 一次拿全。
     * @return 形如 ["assets/cn/handler/START.png", ...] 的相对路径列表。
     */
    fun listFiles(path: String, recursive: Boolean = true): List<String> {
        val url = "https://api.github.com/repos/$owner/$repo/git/trees/$branch?recursive=1"
        val json = JSONObject(get(url))
        val tree = json.optJSONArray("tree") ?: JSONArray()
        val prefix = path.trimEnd('/')
        val result = mutableListOf<String>()
        for (i in 0 until tree.length()) {
            val item = tree.getJSONObject(i)
            if (item.optString("type") != "blob") continue
            val p = item.optString("path")
            if (prefix.isEmpty() || p.startsWith("$prefix/")) {
                result.add(p)
            }
        }
        return result
    }

    /**
     * 下载 raw 文件内容。
     * @param path 仓库内相对路径。
     * @return 文件字节；404 时返回 null。
     */
    fun downloadRaw(path: String): ByteArray? {
        val url = "https://raw.githubusercontent.com/$owner/$repo/$branch/$path"
        return getBytes(url)
    }

    /** 获取仓库默认分支最新的 commit sha(用于版本比对)。 */
    fun headSha(): String? {
        val url = "https://api.github.com/repos/$owner/$repo/branches/$branch"
        return try {
            JSONObject(get(url)).optJSONObject("commit")?.optString("sha")
        } catch (e: Exception) {
            null
        }
    }

    private fun get(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 20_000
        conn.readTimeout = 30_000
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        conn.setRequestProperty("User-Agent", "ALAS-Android")
        if (conn.responseCode !in 200..299) {
            throw java.io.IOException("HTTP ${conn.responseCode} for $url")
        }
        return conn.inputStream.bufferedReader().use { it.readText() }
    }

    private fun getBytes(url: String): ByteArray? {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 20_000
        conn.readTimeout = 60_000
        conn.setRequestProperty("User-Agent", "ALAS-Android")
        val code = conn.responseCode
        if (code == 404) return null
        if (code !in 200..299) {
            throw java.io.IOException("HTTP $code for $url")
        }
        return conn.inputStream.use { it.readBytes() }
    }
}
