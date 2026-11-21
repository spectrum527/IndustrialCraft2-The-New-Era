# QA 数据与相似度检查

0.1.0 的四平台安装包、注册表和实机对照记录见[首版验收](evidence/first-playable-0.1.0/README.md)。

## 裸线放电回归

Gradle 用 Java 21 运行 1.21.1，用 Java 25 运行 26.1.2 和 26.2。三个任务各自安装相应版本生成的 JAR，并从临时世界启动独立专服。

```bash
./gradlew :neoforge:1.21.1:runNativeGameplayCableShock \
  :neoforge:26.1.2:runNativeGameplayCableShock \
  :neoforge:26.2:runNativeGameplayCableShock \
  -Pic2.native.runRoot=build/qa/bare-cable
```

断言结果写入 `build/qa/bare-cable/<版本>/run/native-cable-shock/cableShock.properties`。试验放置并接通真实电缆，检查储能箱的实际收发、固定位置铁傀儡的生命值及原生魔法伤害源。场景覆盖无负载、满接收端、无用支路、绝缘和泡沫覆盖、部分接收、多段、多源、各电流档位的碰撞距离、不同线路交叠及防化套装。

## 手持流体单元回归

```bash
./gradlew :neoforge:1.21.1:runNativeGameplayHeldFluidCell \
  :neoforge:26.1.2:runNativeGameplayHeldFluidCell \
  :neoforge:26.2:runNativeGameplayHeldFluidCell \
  -Pic2.native.runRoot=build/qa/held-cell
```

`build/qa/held-cell/<版本>/run/native-held-cell/heldFluidCell.properties` 写入专服断言。两个单元家族通过原生手持和方块交互入口检验水和熔岩源、流动液、混液、余量不足、生存堆叠与满背包掉落、创造物品栏去重，地面拒放，以及对储罐和兼容机器灌注时的液量与容器守恒。

## 场景数据与相似度

依据项目规则 §1、QA 容差条款、已批准计划 §8.2（本任务给定表）及 WP-F 约定。工具需求先于正式领域 spec；这里定义交换格式，不定义游戏行为，也不包含采集器或实测期望。

`schema/scenario.schema.json` 使用 JSON Schema 2020-12。`schema_version` 标识格式；`data_version` 标识采集批次/数据修订。场景记录整数坐标 `[x,y,z]`、方块标识、初始状态、随机种子、运行 tick 数；采样记录目标、指标、单位与采样时刻；结果以 `sample_id` 对应采样计划，保留双方原始值。`results:[]` 表示未运行，不构成验收通过。

| 指标 | 容差 |
| --- | --- |
| 能量传输 EU/t、loss、击穿 | ±1% |
| 机器耗时、EU/t、产出 | ±1 tick、±1%、精确 |
| 配方产出、权重、副产物 | 精确（结果逐项记录 item/count/weight/byproduct） |
| 反应堆热量、发电、爆炸阈值 | ±2% |
| 作物阶段分布、遗传概率 | 统计检验，显式记录 method/sample_size/alpha |
| GUI 槽位坐标、控件类型 | 结构一致，不要求像素一致 |

相对容差使用 `abs(candidate-reference) <= abs(reference)*value`，参考值为零时要求精确零；绝对容差包含边界。`heat` 是待领域 spec 细化的热量计数单位。作物用分类计数记录观测；尚无已批准的检验方法、样本量与显著性参数，所以统计容差强制 `gate:false`。记录参数与 statistic/p_value 只提供分析材料，不能宣称门禁通过；批准后需更新数据协议。

Schema 检查结构与已批准容差，后续比较器还必须检查：场景/采样 ID 唯一、目标存在、采样 tick 不超出运行 tick、结果完整且与计划逐项对应、结果类型符合指标（数值/产出清单/分类计数/控件清单），统计结果包含 statistic/p_value，双方环境及领域 spec 版本可追溯。初始状态容纳标量；可选 `inventory` 以 slot/item/count 列表记录初始物品，槽位编号从零开始。此版本不执行这些语义检查。

以下是**自造格式示例，不代表实测期望**；所有方块名与数值均为虚构：

```json
{
  "schema_version": "1.0", "data_version": "synthetic-format-example",
  "scenario": {
    "id": "format-only", "ticks": 21, "seed": 7,
    "setup": [{"position": [0, 0, 0], "block": "ic2:example_machine",
      "initial_state": {"stored_eu": 0}, "inventory": [{"slot": 0, "item": "ic2:example_input", "count": 1}]}],
    "samples": [{"id": "duration", "target": [0, 0, 0], "at_ticks": [21],
      "metric": "machine_duration", "unit": "tick", "tolerance": {"type": "absolute", "value": 1}}]
  },
  "results": [{"sample_id": "duration", "at_tick": 21, "reference": 20, "candidate": 21}]
}
```

相似度工具仅依赖 Python 3 标准库：

```bash
python3 tools/similarity/check.py --candidate /private/candidate --reference /private/reference
PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s tools/similarity -p 'test_*.py' -v
python3 -m json.tool parity/schema/scenario.schema.json > /dev/null
```

递归扫描 `.java`：去注释与空白、保留关键字/字面量、标识符统一为 `ID`，比较 token 集合的 5-gram Jaccard。字符串、字符和文本块中的注释标记保留。每个 candidate 报最高匹配、0–1 分数、原因和扫描数量；原始行保留空格，忽略换行编码，任意参考文件连续八行相同即失败。此工具是词法启发式，不替代 Java 编译器或人工审查。

单文件分数 **>25%** 失败、**>15%** 警告；恰好 25% 仅警告，恰好 15% 不警告。stdout 始终输出 JSON（`--help` 除外）。退出码：`0` 完整扫描且通过/警告；`1` 相似度或八行规则失败；`2` 参数、读取、无 Java 文件或不足五 token 等无法评估；`3` CI 缺少参考目录而跳过（同时向 stderr 发警告）。空文件和短文件在任一侧均不形成通过结论。

完整检查只能在本地或持有私有参考目录的 runner 运行。`CI` 为非空且不为 `0/false/no` 时，候选目录存在（即使没有 Java 文件）且省略 `--reference` 得到 `skipped`/退出码 3；显式给错目录仍为错误/退出码 2。公共 CI 应把 3 标为跳过并展示警告，不能转换成门禁通过。自测只在工具目录创建并清理自造样本，不读取真实参考目录。玩家版游戏内逐项验收见本页开头的 0.1.0 记录。

## 风力动能与转子

三个 NeoForge 目标分别运行以下命令。每个任务先在专服用玩家交互放置风机、动能发电机和储电箱，再启动第二个专服进程加载同一存档。

```sh
./gradlew :neoforge:1.21.1:runNativeGameplayWindRead
./gradlew :neoforge:26.1.2:runNativeGameplayWindRead
./gradlew :neoforge:26.2:runNativeGameplayWindRead
```

各目标的 `build/run/native-wind/windWrite.properties` 与 `windRead.properties` 记录正反接、可用 KU、实际换电、扫掠遮挡和冷启动恢复的结果。26.x 的 `runModernServerSmoke` 还会把服务器已加载的工作台配方写入原生配方同步包并解码，检查喷气背包挂载配方能到达接收端。[风机规格](../spec/energy/wind-kinetic.md) · [联机规格](../spec/platform/network.md)
