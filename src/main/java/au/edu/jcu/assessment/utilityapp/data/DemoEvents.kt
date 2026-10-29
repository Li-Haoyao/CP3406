package au.edu.jcu.assessment.utilityapp.data

import au.edu.jcu.assessment.utilityapp.model.CollabCategory
import au.edu.jcu.assessment.utilityapp.model.CollabEvent
import java.time.LocalDate

/** All games, brands, rewards and dates here are fictional design fixtures. */
object DemoEvents {
    fun create(today: LocalDate): List<CollabEvent> {
        fun event(id: String, game: String, brand: String, title: String, tagline: String,
            category: CollabCategory, regions: List<String>, start: Long, end: Long,
            rewards: List<String>, steps: List<String>, location: String,
            requirement: String, featured: Boolean = false) = CollabEvent(
            id, game, brand, title, tagline, category, regions, today.plusDays(start), today.plusDays(end),
            today.plusDays(minOf(start - 2, -1)), rewards, steps, location, requirement,
            "演示活动：品牌、奖励与活动安排均为虚构，仅用于展示界面与操作。", featured = featured)
        return listOf(
            event("demo-tea", "星轨漫游", "青柚茶研", "把星光，装进这一杯。", "一杯限定，开启次元漫游",
                CollabCategory.DRINK, listOf("CN", "MY", "SG"), -6, 8,
                listOf("限定杯套", "角色透卡", "主题纸袋"),
                listOf("查看你所在地区的参与门店与供应时间。", "到店选择联动饮品或指定套餐。", "按活动规则领取杯套与角色透卡，领取后核对数量。"),
                "中国 / 马来西亚 / 新加坡 · 指定门店（演示）", "购买指定联动饮品（演示规则）", true),
            event("demo-food", "雾境回声", "咕噜火锅", "今晚，组队开锅。", "和喜欢的角色一起赴约",
                CollabCategory.FOOD, listOf("CN"), -10, 4,
                listOf("角色徽章", "联动桌垫"),
                listOf("查看联动主题门店，并确认预约时段。", "到店出示预约信息，选择指定套餐。", "完成用餐后按规则领取联动赠品。"),
                "中国大陆 · 主题火锅门店（演示）", "指定套餐；部分门店需预约（演示规则）"),
            event("demo-fashion", "零域行动", "NOVA STUDIO", "把冒险，穿在身上。", "城市漫游者的限定衣橱",
                CollabCategory.FASHION, listOf("CN", "MY", "SG"), 3, 24,
                listOf("印花卫衣", "限定帆布包"),
                listOf("确认发售日期与所在地区的购买渠道。", "查看商品尺码、材质及发售说明。", "在开售后按品牌规则下单。"),
                "品牌线上店 / 指定线下门店（演示）", "按发售时间购买指定商品（演示规则）"),
            event("demo-retail", "星轨漫游", "小岛便利", "在转角，遇见小惊喜。", "收集一份日常里的快乐",
                CollabCategory.RETAIL, listOf("MY", "SG"), -2, 12,
                listOf("亚克力挂件", "限定贴纸"),
                listOf("确认附近参与门店及联动商品。", "选择符合活动要求的商品组合。", "结账时按规则兑换赠品。"),
                "马来西亚 / 新加坡 · 指定便利店（演示）", "指定商品组合（演示规则）"),
            event("demo-game", "零域行动", "像素派对", "两个世界，同一场冒险。", "跨越次元的特别委托",
                CollabCategory.GAME, listOf("CN", "MY", "SG"), -7, 2,
                listOf("联动头像", "限定装饰"),
                listOf("登录游戏并打开联动活动入口。", "查看活动任务与账号等级要求。", "完成任务后在活动页面领取奖励。"),
                "游戏内 · 演示服务器", "账号等级达到活动要求（演示规则）"),
            event("demo-coffee", "雾境回声", "MORI COFFEE", "让午后，慢一点。", "一杯咖啡的异世界来信",
                CollabCategory.DRINK, listOf("MY", "SG"), -4, 6,
                listOf("主题杯垫", "明信片"),
                listOf("查看联动咖啡店及营业时间。", "购买指定主题咖啡。", "领取主题杯垫和明信片。"),
                "马来西亚 / 新加坡 · 指定咖啡店（演示）", "指定主题饮品（演示规则）"),
            event("demo-ended", "星轨漫游", "周末面包社", "把美好，留在这一页。", "已经结束的联动记录",
                CollabCategory.FOOD, listOf("CN"), -24, -5,
                listOf("烘焙纸袋"), listOf("本演示活动已结束，用于验证历史活动筛选。"),
                "中国大陆 · 指定门店（演示）", "已结束，无法参与")
        ).map { it.copy(englishTranslation = DemoEventTranslations.byId[it.id], translations = RegionalDemoTranslations.forId(it.id)) }
    }
}
