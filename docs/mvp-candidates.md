# MVP 候选清单（G0 待确认）

本表供章程评审，不是范围决定或实现规格。共 41 个候选，其中推荐 39 个；每行一个建议注册 ID，方块及其对应的可持有形式按一个内容条目计。名称为建议，当前尚无实现或已完成注册的事实。推荐仅表示可供负责人选择，最终范围、ID、配方及验收由 G0 确认。

依赖列列出本表内选定的直接内容依赖；原版世界材料、燃料、工作台和熔炉不占本表名额。机器运转另需能量网络、机器库存、配方、界面与世界生成等系统，它们不是表内注册 ID。部分配方可有其他路线；表中只选一条可启动路线，不代表其他路线已经决定。

| 建议 ID | 类别 | 闭环作用 | 依赖其他条目 | 推荐及理由 |
|---|---|---|---|---|
| `ia2:rubber_log` | 方块 | 树干提供树脂及可提取原料 | `ia2:treetap` | 是：启动橡胶链 |
| `ia2:rubber_leaves` | 方块 | 树冠及树苗来源 | `ia2:rubber_sapling` | 是：支持树再生 |
| `ia2:rubber_sapling` | 方块 | 种植橡胶树 | `ia2:rubber_log`, `ia2:rubber_leaves` | 是：可再生来源 |
| `ia2:tin_ore` | 方块 | 生成锡矿 | `ia2:raw_tin` | 是：基础锡来源 |
| `ia2:deepslate_tin_ore` | 方块 | 深层锡矿 | `ia2:raw_tin` | 是：覆盖深层采矿 |
| `ia2:raw_tin` | 物品 | 锡矿掉落，熔炼为锡锭或打粉 | `ia2:tin_ore`, `ia2:tin_ingot`, `ia2:macerator` | 是：采矿输入 |
| `ia2:tin_ingot` | 物品 | 锡制板材的原料 | `ia2:raw_tin` | 是：电池材料 |
| `ia2:resin` | 物品 | 熔炼或提取为橡胶 | `ia2:rubber_log`, `ia2:treetap`, `ia2:rubber` | 是：导线绝缘来源 |
| `ia2:rubber` | 物品 | 绝缘线材 | `ia2:resin` | 是：电路和电池所需 |
| `ia2:treetap` | 物品 | 从树干采集树脂 | `ia2:rubber_log` | 是：无电启动 |
| `ia2:forge_hammer` | 物品 | 将锭加工成板及外壳 | — | 是：无电制板 |
| `ia2:cutter` | 物品 | 将板材剪成裸线 | `ia2:iron_plate`, `ia2:forge_hammer` | 是：无电制线 |
| `ia2:iron_plate` | 物品 | 机器外壳、电路和铁炉材料 | `ia2:forge_hammer` | 是：机器基础 |
| `ia2:copper_plate` | 物品 | 铜线原料 | `ia2:forge_hammer` | 是：电路基础 |
| `ia2:tin_plate` | 物品 | 锡线和锡外壳原料 | `ia2:tin_ingot`, `ia2:forge_hammer` | 是：电池基础 |
| `ia2:tin_casing` | 物品 | 电池外壳材料 | `ia2:tin_plate`, `ia2:forge_hammer` | 是：电池配方 |
| `ia2:copper_cable` | 方块 | 裸铜线及绝缘铜线原料 | `ia2:copper_plate`, `ia2:cutter` | 是：制线中间品 |
| `ia2:insulated_copper_cable` | 方块 | 低压输电和电路材料 | `ia2:copper_cable`, `ia2:rubber` | 是：连接与电路 |
| `ia2:tin_cable` | 方块 | 裸锡线及绝缘锡线原料 | `ia2:tin_plate`, `ia2:cutter` | 是：制线中间品 |
| `ia2:insulated_tin_cable` | 方块 | 低压线及电池、储电箱材料 | `ia2:tin_cable`, `ia2:rubber` | 是：电池配方 |
| `ia2:circuit` | 物品 | 四种加工机器的基础电路 | `ia2:insulated_copper_cable`, `ia2:iron_plate` | 是：机器基础 |
| `ia2:machine` | 方块 | 基础机器外壳 | `ia2:iron_plate` | 是：发电机及机器配方 |
| `ia2:re_battery` | 物品 | 存储电能，制作发电机及储电箱 | `ia2:tin_casing`, `ia2:insulated_tin_cable` | 是：供电基础 |
| `ia2:iron_furnace` | 机器 | 熔炼及电炉前置 | `ia2:iron_plate` | 是：电炉配方 |
| `ia2:generator` | 机器 | 消耗燃料输出电能 | `ia2:re_battery`, `ia2:machine` | 是：启动供电 |
| `ia2:batbox` | 机器 | 储存并输出低压电能 | `ia2:re_battery`, `ia2:insulated_tin_cable`, `ia2:generator` | 是：供电缓冲 |
| `ia2:macerator` | 机器 | 把矿物加工为粉碎产物 | `ia2:machine`, `ia2:circuit`, `ia2:generator` | 是：矿物处理 |
| `ia2:electric_furnace` | 机器 | 用电熔炼 | `ia2:iron_furnace`, `ia2:circuit`, `ia2:generator` | 是：电力加工 |
| `ia2:compressor` | 机器 | 压缩可用材料 | `ia2:machine`, `ia2:circuit`, `ia2:generator` | 是：基础加工；首批配方待定 |
| `ia2:extractor` | 机器 | 将树脂提取为橡胶 | `ia2:machine`, `ia2:circuit`, `ia2:treetap`, `ia2:generator` | 是：提高橡胶产量 |
| `ia2:crushed_tin` | 物品 | 粉碎锡，用于青铜混合或熔炼 | `ia2:raw_tin`, `ia2:macerator` | 是：青铜材料 |
| `ia2:crushed_copper` | 物品 | 粉碎铜，用于青铜混合 | `ia2:macerator` | 是：青铜材料 |
| `ia2:bronze_dust` | 物品 | 混合粉碎铜和锡后熔炼 | `ia2:crushed_tin`, `ia2:crushed_copper` | 是：青铜原料 |
| `ia2:bronze_ingot` | 物品 | 青铜工具和装甲原料 | `ia2:bronze_dust` | 是：基础装备 |
| `ia2:bronze_pickaxe` | 物品 | 采矿工具 | `ia2:bronze_ingot` | 是：基础工具 |
| `ia2:bronze_helmet` | 物品 | 头部防护 | `ia2:bronze_ingot` | 是：基础装甲 |
| `ia2:bronze_chestplate` | 物品 | 躯干防护 | `ia2:bronze_ingot` | 是：基础装甲 |
| `ia2:bronze_leggings` | 物品 | 腿部防护 | `ia2:bronze_ingot` | 是：基础装甲 |
| `ia2:bronze_boots` | 物品 | 足部防护 | `ia2:bronze_ingot` | 是：基础装甲 |
| `ia2:lead_ore` | 方块 | 铅矿来源 | 待补铅矿掉落及加工条目 | 否：当前闭环不消费铅 |
| `ia2:uranium_ore` | 方块 | 铀矿来源 | 待补铀矿掉落及加工条目 | 否：当前闭环不消费铀 |

## 闭环检查与限制

- 启动：树与木制树液采集器 → 树脂 → 普通熔炉制橡胶；锡矿 → 粗锡 → 锡锭；铁锭制锻造锤及铁板 → 剪线钳；铜板、锡板分别剪线并加橡胶绝缘。此前不需要任何电动机器。
- 供电与加工：锡外壳与绝缘锡线制电池，铁板制机器外壳；发电机供电，经线缆及储电箱连接机器。绝缘铜线和铁板制电路，配合外壳或铁炉构成四台加工机器。机器需有可操作的基础界面和配方查询支持；这些系统不计入本表 ID。
- 加工与装备：打粉机处理锡及原版铜矿材料，粉碎产物混合为青铜粉并熔炼为青铜锭，再制作镐和四件装甲。提取机处理树脂；压缩机可选用原版材料压缩配方。每条选定路线的本表内依赖均在推荐集合中；依赖关系含运行用途而非严格的合成先后，树木与机器运行因此会出现有意的互指。
- 此为内容依赖的静态检查，不证明数值、掉落、配方、能源等级或世界生成已完成验证。锡矿掉落、铜粉碎输入、配方标签、工具消耗、机器输入输出及失败条件需在 P1 逐项核实；非选中配方的其他产物未承诺纳入。具体输入、输出、边界和失败模式由后续规格定义。
- 范围未覆盖铅、铀的可用链与深层变体，也未覆盖青铜其他工具；它们不因未列入表而被决定永久排除。图形界面及配方查询属于功能范围，不是本表注册计数。

## 决策约束与回归

能量网络按同步计算运行，并预留可替换计算器接口（B#6）；机器界面采用统一的数据驱动体系并保留特殊界面例外通道（B#8）。机器循环音须在专用服务器场景正常播放（B#36）。原版 bug 回归：在专用服务器连接客户端并让机器持续工作时，检查循环音不会因服务端环境而静音；具体触发和期望需由后续音效规格与实机测试确认。B#6 的异步空壳不得成为可调用但失败的运行路径。

## 负责人待确认

1. 是否接受 39 项推荐范围及两种锡矿形态；铅、铀是否进入 MVP，若进入须补齐掉落、加工及用途并重新计数。
2. 是否接受青铜镐加完整青铜装甲作为基础装备，以及压缩机首批使用原版材料的配方；是否增加其他工具、材料或发电机。
3. 核定最终注册 ID、每个机器的首批配方与界面 / 配方查询范围，以及 G1 规格和 G5 实机验收标准。
