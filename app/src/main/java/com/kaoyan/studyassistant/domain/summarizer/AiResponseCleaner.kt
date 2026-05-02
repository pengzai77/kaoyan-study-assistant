package com.kaoyan.studyassistant.domain.summarizer

/**
 * AI 响应内容清洗工具。
 *
 * 部分大模型（如 MiniMax M2.7、DeepSeek-R1、QwQ 等）会在回复中输出思考过程。
 * 思考过程有两种常见格式：
 *
 * 1. 标签包裹格式（content 字段内）：
 *    <think>这里是思考过程...</think>
 *    这里是最终回答
 *
 * 2. 独立字段格式（MiniMax 专有）：
 *    thinking_content: "这里是思考过程"
 *    content: "这里是最终回答"
 *    （DTO 层已通过 thinkingContent 字段映射，此处只处理 content 字段的清洗）
 *
 * 本工具统一过滤上述两种格式，确保界面只展示最终回答。
 */
object AiResponseCleaner {

    /**
     * 清洗 AI 响应内容，移除所有思考过程标签及其内容。
     *
     * 支持的标签格式：
     * - `<think>...</think>`（MiniMax、DeepSeek-R1、QwQ 通用）
     * - `<thinking>...</thinking>`（部分模型变体）
     * - `<reasoning>...</reasoning>`（部分模型变体）
     *
     * @param content 原始响应内容
     * @return 清洗后的内容，保证首尾无多余空白
     */
    fun clean(content: String): String {
        if (content.isBlank()) return content

        var result = content

        // 移除 <think>...</think>（支持多行、嵌套标签内有换行）
        result = result.replace(Regex("<think>[\\s\\S]*?</think>", RegexOption.IGNORE_CASE), "")

        // 移除 <thinking>...</thinking>
        result = result.replace(Regex("<thinking>[\\s\\S]*?</thinking>", RegexOption.IGNORE_CASE), "")

        // 移除 <reasoning>...</reasoning>
        result = result.replace(Regex("<reasoning>[\\s\\S]*?</reasoning>", RegexOption.IGNORE_CASE), "")

        // 如果标签未闭合（模型输出被截断），移除从 <think> 开始到末尾的内容
        result = result.replace(Regex("<think>[\\s\\S]*$", RegexOption.IGNORE_CASE), "")
        result = result.replace(Regex("<thinking>[\\s\\S]*$", RegexOption.IGNORE_CASE), "")
        result = result.replace(Regex("<reasoning>[\\s\\S]*$", RegexOption.IGNORE_CASE), "")

        return result.trim()
    }

    /**
     * 判断内容是否包含思考过程标签。
     * 用于调试或日志记录。
     */
    fun containsThinkingContent(content: String): Boolean {
        return content.contains(Regex("<think>|<thinking>|<reasoning>", RegexOption.IGNORE_CASE))
    }
}
