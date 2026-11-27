# 配方规格模板

> 以下字段均必填；没有余物/随机性也须显式写“无 / 100%”，不以 JSON 或实现代码代替行为。

## 1. 字段说明与必填项
| 字段 | 说明 |
|---|---|
| id / type / evidence | `ic2:` 配方 ID、配方类型、核验版本与静态/实测/设计/待确认状态 |
| inputs / matching | 物品或标签、数量 个/次、流体 mB/次或 mB/t；有序性、数据匹配与不足量行为 |
| conditions / config | 工具/温度/模式门槛、活键与默认值；多条同时匹配时的选择规则 |
| outputs / remainders | 每次精确产物、余物去向、概率/权重含义；非物品数值产出须标其单位 |
| duration / energy | tick/次、EU/t 或 EU/次；若归机器决定须明确引用已交付章节 |
| commit / boundaries | 原料扣除与输出提交时点、满槽、取消、换配方、数据包重载的处理 |
| failures / regressions | 非法数据、无效引用、零/负数量的拒绝方式；修复后行为与原版 bug（回归用例） |
| parity / dependencies | 初态、操作、精确断言、随机样本量/容差；未交付依赖及待确认冲突 |

## 2. 填写规则
输入、输出、边界、失败模式四要素不可省略；示例不能代表完整配方集，未核验配方不得标为真实。
数据包重载和冲突策略若尚未决定，应阻塞该分支，不能由实现者自行挑第一条。

## 3. 示例（真实配方经静态核验，尚未运行参考实例）
配方 `ic2:macerator/coal_to_coal_dust`，类型 `ic2:macerator`：输入 `minecraft:coal` 1 个，输出 `ic2:coal_dust` 1 个，概率 100%，无余物。
本项目 ID 已换为 `ic2:`；输入是外部物品。时长/能耗归 [`打粉机`](../machines/macerator.md) §2、§3：默认 300 tick/次、2 EU/t。
边界：原料在成功完成时扣除；输出不足 1 个空间时不开始；失败模式：无效输入或原料不足不扣物、不推进。
parity：空输出、仅 1 个煤、600 EU、无外部供能与升级；第 299 tick 无产出，第 300 tick 输出 1 个煤粉、输入清空、电量 0 EU；时间 ±1 tick，物品精确，能量 ±1%。
其余配方、冲突/重载策略等待 P1-T5 与 P4 配方/机器框架规格；不可把本例当成已交付的完整数据包。

## 4. 跨加载器通用标签、流体等价与动态兜底约定（v1.0.11）

- **通用标签双向与多路别名**：机器配方引擎与运行时标签查询同时识别 `forge:` 与 `c:` 命名空间，并展开跨版本路径差异，包括三路扳手（`tools/wrench` ↔ `tools/wrenches` ↔ `wrenches`）、三路剪刀（`shears` ↔ `tools/shear` ↔ `tools/shears`）、全部 8 种原版矿石（`minecraft:*_ores` ↔ `{forge,c}:ores/*`）、1.21+ 复数/重构路径（`gravels`、`slime_balls`、`netherracks`、`sandstone/blocks`、`leathers`、`strings`、`bones`、`feathers`、`gunpowders`、`plates/dense`）、砖块别名（`bricks/normal` ↔ `ingots/brick`、`bricks/nether` ↔ `ingots/nether_brick`）及沙类子标签（`sand/*` ↔ `sands/*`）。
- **多标签匹配优先级**：同一机器内存在多条标签配方时，按特异度稳定排序（带权重矿石配方 > 具体矿物/金属/作物标签 > 泛化岩石/圆石/沙砾标签），避免同时带有 `#c:stones`/`#c:cobblestones` 与 `#c:ores/*` 的外模组矿石被误打成沙子或圆石。
- **流体槽规范标签等价**：所有车间机器流体槽、核电冷却液槽、流体装罐与单元合成在匹配需求流体时，对规范流体 ID 及其对应的 `{c,forge}:*` 流体标签做统一等价判定（水、蒸馏水、蒸汽、过热蒸汽、沼气、杂酚油、熔岩、帕霍霍熔岩、生物质、冷却液、热冷却液、压缩空气、UU 物质、除草剂、建筑泡沫、氢气、氧气）。
- **回收机黑白名单配置**：运行时读取活键 `balance/recyclerBlacklist` 与 `balance/recyclerWhitelist`，支持精确物品 ID、`Tag:`/`#` 标签条目（与内置 `#ic2:recycler_blacklist`、`#c:glass_panes`、`#c:rods/wooden` 共同生效），且白名单严格高于黑名单与内置排除规则。
- **装罐机外模组食物兜底**：`canner_bottle` 显式配方表优先；未命中表项但具有正营养值的第三方食物按 `min(64, nutrition)` 消耗等量 `ic2:tin_can` 并产出等量 `ic2:filled_tin_can`。
- **UU 扫描/复制与组装标签等价**：精确物品 ID 优先；未直接列出的外模组矿物、粗矿、金属锭与常用建材按特异度顺序回退至通用标签等价成本或组装模板。
- **自动化标签过滤**：高级弹出/拉取升级、分拣机与高级采矿机支持以 `#ns:path` 或 `Tag:ns:path` 命名滤网样板物品按通用标签过滤（保留原精确物品与耐久/电量过滤语义）。
