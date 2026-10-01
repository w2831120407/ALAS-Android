package com.nowdex.android.data.model

import androidx.compose.ui.graphics.Color

/**
 * 支持的 AI 智能体服务。
 * 对应 Nowdex 官网展示的服务列表。
 */
enum class AiService(
    val id: String,
    val displayName: String,
    val shortName: String,
    val color: Color,
    val planTier: String? = null,
) {
    Codex(
        id = "codex",
        displayName = "Codex",
        shortName = "Codex",
        color = Color(0xFF10A37F),
        planTier = "PLUS",
    ),
    Claude(
        id = "claude",
        displayName = "Claude",
        shortName = "Claude",
        color = Color(0xFFD97757),
        planTier = "MAX",
    ),
    Cursor(
        id = "cursor",
        displayName = "Cursor",
        shortName = "Cursor",
        color = Color(0xFF8B5CF6),
    ),
    Grok(
        id = "grok",
        displayName = "Grok",
        shortName = "Grok",
        color = Color(0xFF1D1D1F),
    ),
    Kimi(
        id = "kimi",
        displayName = "Kimi",
        shortName = "Kimi",
        color = Color(0xFF2563EB),
    ),
    GLM(
        id = "glm",
        displayName = "GLM",
        shortName = "GLM",
        color = Color(0xFF3B82F6),
    ),
    MiniMax(
        id = "minimax",
        displayName = "MiniMax",
        shortName = "MiniMax",
        color = Color(0xFF6366F1),
    ),
    Qoder(
        id = "qoder",
        displayName = "Qoder",
        shortName = "Qoder",
        color = Color(0xFFF59E0B),
    ),
    DeepSeek(
        id = "deepseek",
        displayName = "DeepSeek",
        shortName = "DeepSeek",
        color = Color(0xFF4989F4),
    ),
    OpenCodeGo(
        id = "opencodego",
        displayName = "OpenCode Go",
        shortName = "OpenCode",
        color = Color(0xFF10B981),
    );

    companion object {
        fun fromId(id: String): AiService? = entries.firstOrNull { it.id == id }
    }
}
