/*
 * UniBoard — Unicode 输入法
 * Copyright (C) 2026 UniBoard contributors
 *
 * 本程序是自由软件：你可以依据 GNU 通用公共许可证第 3 版
 * 或（你选择的）任何更新版本所规定的条款，重新分发和/或修改它。
 *
 * 本程序基于"希望它有用"的目的分发，但不提供任何担保，
 * 也不承诺适用于特定用途。完整条款见项目根目录的 LICENSE 文件。
 */

package org.YavSoft.UniBoard.emoji

/**
 * Emoji 语义检索：输入「笑」「哭」「heart」「fire」这类描述词，候选栏直接给出对应表情。
 *
 * 词表按「关键词 → 表情」手写，中英文都收，覆盖日常最高频的一批；
 * 比遍历 Unicode 名称更准（CJK 用户不会去记 CLDR 的英文名）。
 */
object EmojiKeywords {

    private val MAP: List<Pair<String, String>> = listOf(
        // 情绪
        "笑" to "😄", "大笑" to "😆", "微笑" to "🙂", "开心" to "😊", "高兴" to "😃",
        "哭" to "😭", "难过" to "😢", "伤心" to "💔", "生气" to "😠", "怒" to "😡",
        "尴尬" to "😅", "汗" to "😓", "无语" to "😑", "困" to "😴", "睡" to "😴",
        "爱" to "❤️", "喜欢" to "😍", "心动" to "😍", "亲亲" to "😘", "抱抱" to "🤗",
        "赞" to "👍", "踩" to "👎", "ok" to "👌", "加油" to "💪", "鼓掌" to "👏",
        "谢谢" to "🙏", "祈祷" to "🙏", "拜托" to "🙏", "握手" to "🤝", "挥手" to "👋",
        "疑问" to "❓", "问号" to "❓", "感叹" to "❗", "惊" to "😲", "害怕" to "😨",
        "酷" to "😎", "思考" to "🤔", "疑问脸" to "🤔", "无语脸" to "😐", "翻白眼" to "🙄",
        "笑死" to "🤣", "庆祝" to "🎉", "胜利" to "✌️", "比心" to "🫰", "鞠躬" to "🙇",
        // 生活
        "吃" to "🍚", "饭" to "🍚", "面" to "🍜", "咖啡" to "☕", "茶" to "🍵",
        "啤酒" to "🍺", "干杯" to "🍻", "蛋糕" to "🍰", "生日" to "🎂", "水果" to "🍎",
        "苹果" to "🍎", "西瓜" to "🍉", "火" to "🔥", "热" to "🔥", "冷" to "🥶",
        "雨" to "🌧️", "晴" to "☀️", "太阳" to "☀️", "月" to "🌙", "星" to "⭐",
        "云" to "☁️", "雪" to "❄️", "风" to "🌬️", "雷" to "⚡", "电" to "⚡",
        "花" to "🌸", "树" to "🌳", "草" to "🌿", "海" to "🌊", "山" to "⛰️",
        "猫" to "🐱", "狗" to "🐶", "鸟" to "🐦", "鱼" to "🐟", "马" to "🐴",
        "车" to "🚗", "飞机" to "✈️", "船" to "🚢", "自行车" to "🚲", "地铁" to "🚇",
        "家" to "🏠", "公司" to "🏢", "学校" to "🏫", "医院" to "🏥", "钱" to "💰",
        "手机" to "📱", "电脑" to "💻", "相机" to "📷", "书" to "📚", "邮件" to "📧",
        "时间" to "⏰", "钟" to "⏰", "日历" to "📅", "锁" to "🔒", "钥匙" to "🔑",
        "礼物" to "🎁", "音乐" to "🎵", "电影" to "🎬", "游戏" to "🎮", "球" to "⚽",
        "工作" to "💼", "笔" to "✏️", "手机信号" to "📶", "电池" to "🔋", "灯" to "💡",
        // 符号与标记
        "对" to "✅", "错" to "❌", "警告" to "⚠️", "禁止" to "🚫", "新" to "🆕",
        "心" to "❤️", "红心" to "❤️", "星星" to "⭐", "闪" to "✨", "皇冠" to "👑",
        "数字" to "🔢", "百分" to "💯", "一百" to "💯", "箭头" to "➡️", "上" to "⬆️",
        // 英文
        "smile" to "😄", "happy" to "😊", "sad" to "😢", "cry" to "😭", "angry" to "😠",
        "love" to "❤️", "heart" to "❤️", "kiss" to "😘", "hug" to "🤗", "ok" to "👌",
        "thumbsup" to "👍", "thumbsdown" to "👎", "clap" to "👏", "pray" to "🙏",
        "fire" to "🔥", "hot" to "🔥", "cold" to "🥶", "sun" to "☀️", "moon" to "🌙",
        "star" to "⭐", "rain" to "🌧️", "snow" to "❄️", "cloud" to "☁️", "wind" to "🌬️",
        "cat" to "🐱", "dog" to "🐶", "bird" to "🐦", "fish" to "🐟", "horse" to "🐴",
        "car" to "🚗", "plane" to "✈️", "ship" to "🚢", "bike" to "🚲", "train" to "🚇",
        "house" to "🏠", "office" to "🏢", "school" to "🏫", "money" to "💰", "phone" to "📱",
        "laptop" to "💻", "book" to "📚", "mail" to "📧", "clock" to "⏰", "lock" to "🔒",
        "gift" to "🎁", "music" to "🎵", "movie" to "🎬", "game" to "🎮", "ball" to "⚽",
        "work" to "💼", "idea" to "💡", "yes" to "✅", "no" to "❌", "warning" to "⚠️",
        "party" to "🎉", "cool" to "😎", "think" to "🤔", "question" to "❓", "exclaim" to "❗",
        "coffee" to "☕", "tea" to "🍵", "beer" to "🍺", "cake" to "🎂", "birthday" to "🎂"
    )

    /** 按输入前缀匹配关键词，返回对应表情（去重，最多 limit 个） */
    fun suggest(input: String, limit: Int = 6): List<String> {
        if (input.length < 1) return emptyList()
        val q = input.lowercase()
        val out = LinkedHashSet<String>(limit)
        // 先精确、再前缀
        for ((keyword, emoji) in MAP) {
            if (keyword == q) out.add(emoji)
        }
        if (out.size < limit) {
            for ((keyword, emoji) in MAP) {
                if (keyword.startsWith(q)) out.add(emoji)
                if (out.size >= limit) break
            }
        }
        return out.toList()
    }
}
