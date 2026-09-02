package com.alas.android.core.update

import com.alas.android.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * 应用自更新检查：从本项目 GitHub Releases 拉取最新版本信息。
 *
 * 通过 Releases API 获取最新 release，比对版本号与当前版本，
 * 提供 APK 下载地址与 release 说明。
 */
class AppUpdater(
    private val owner: String = "w2831120407",
    private val repo: String = "ALAS-Android",
) {

    data class ReleaseInfo(
        val tagName: String,
        val versionCode: Int,
        val apkUrl: String?,
        val apkSize: Long,
        val notes: String,
        val publishedAt: String,
    )

    /** 当前版本号(由 BuildConfig 提供)。 */
    fun currentVersion(): Int = BuildConfig.VERSION_CODE

    /**
     * 获取最新 release。若没有 release 或没有可用 APK 返回 null。
     */
    fun latestRelease(): ReleaseInfo? {
        val url = "https://api.github.com/repos/$owner/$repo/releases/latest"
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 20_000
        conn.readTimeout = 30_000
        conn.setRequestProperty("User-Agent", "ALAS-Android")
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        if (conn.responseCode == 404) return null // 尚无 release
        if (conn.responseCode !in 200..299) {
            throw java.io.IOException("HTTP ${conn.responseCode}")
        }
        val text = conn.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(text)

        val assets = json.optJSONArray("assets") ?: JSONArray()
        var apkUrl: String? = null
        var apkSize = 0L
        for (i in 0 until assets.length()) {
            val a = assets.getJSONObject(i)
            if (a.optString("name").endsWith(".apk")) {
                apkUrl = a.optString("browser_download_url")
                apkSize = a.optLong("size")
                break
            }
        }
        // 版本号从 tagName 提取，形如 "v1.2.3" -> code 10203
        val tag = json.optString("tag_name", "")
        return ReleaseInfo(
            tagName = tag,
            versionCode = parseVersionCode(tag),
            apkUrl = apkUrl,
            apkSize = apkSize,
            notes = json.optString("body", ""),
            publishedAt = json.optString("published_at", ""),
        )
    }

    /** 是否有新版本可更新。 */
    fun hasUpdate(): Boolean {
        val rel = latestRelease() ?: return false
        return rel.versionCode > currentVersion()
    }

    /** 从 "v1.2.3" 解析为整数版本号 10203。 */
    private fun parseVersionCode(tag: String): Int {
        val nums = tag.trimStart('v').split('.').mapNotNull { it.toIntOrNull() }
        if (nums.isEmpty()) return 0
        return when (nums.size) {
            1 -> nums[0]
            2 -> nums[0] * 100 + nums[1]
            else -> nums[0] * 10000 + nums[1] * 100 + nums[2]
        }
    }
}
