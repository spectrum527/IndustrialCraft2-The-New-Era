# spec 模板：机器/设备规格页

> 一台机器一个文件：`spec/machines/<id>.md`。四要素（输入/输出/边界/失败模式）缺一不可。
> 只写行为与数值，**不写**源码行号、类名、SRG 名。

```markdown
# <机器显示名>（ia2:<registry_id>）

## 概述
一句话用途 + 在工业链中的位置。

## 参数卡
| 项 | 值 | 单位/说明 |
|---|---|---|
| tier | 1 | 电压等级（0=8 EU/t，每级 ×4） |
| energy_capacity | 600 | EU（内部缓冲） |
| energy_consumption | 2 | EU/t（工作时） |
| operation_length | 300 | ticks/次（默认） |
| recipe_type | ia2:macerator | 数据包配方类型 |
| upgrade_slots | 4 | 支持的升级类型见 upgrade.md |
| sounds | machine.macerator.loop | 键名见 spec/audio/ |

## 槽位
| 槽 | 类型 | 数量 | 面限制 | 自动化规则 |
|---|---|---|---|---|

## 行为规格（状态机）
idle → working → done 的转移条件；配方缓存失效条件；升级重算时机（保持进度百分比）。

## 配方
输入/输出/权重；引用 spec/recipes/macerator.json。

## 输入（玩家可做的事）
交互列表（右键开 GUI、扳手旋转/拆卸行为与掉落…）。

## 输出（对世界/玩家可见的状态）
active 状态、比较器信号公式、GUI 同步字段、音效触发。

## 边界条件
断电/断料/塞满/方块朝向限制/多方块邻居要求。

## 失败模式
错误输入的处理、爆炸/损坏条件（如有）、修复项（引用 decisions.md B 表编号）。

## parity 场景
引用 parity/machines/<id>.json（期望值来自 1.19.2 参考实例）。
```

示例（打粉机骨架，P1 补全数值）：`spec/machines/macerator.md`（待产出）。
