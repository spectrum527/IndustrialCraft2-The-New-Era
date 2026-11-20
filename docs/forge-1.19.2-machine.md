# Forge 1.19.2 首台机器与能量图验收

Forge 43.5.0 / Java 17 已对齐现代分支的首台机器节点。四个通用生产文件、三个契约测试文件和组件规格在本分支、1.21.1 机器提交 `741f773`、26.1.2 的 `b496f51` 和 26.2 的 `5e7687c` 具有相同的 Git blob。Forge 使用自己的原生方块实体、熔炉燃料查询和 NBT 读写。[现代机器交接](https://github.com/spectrum527/ic2/blob/741f773/docs/machine-foundation.md) · [Forge 宿主契约](../spec/machines/forge-1.19.2.md)

| 已交付任务 | 验收范围 |
|---|---|
| P3-T1、P3-T1-QA | 同世界六面有向连接、设备端点、导体中继、环路和拆接、等级及包预算。5 项契约通过。B#6 的调度与实收结算由 P3-T2/T3 接续。 |
| P4-T1、P4-T1-QA | 组件生命周期 4 项、燃烧储能 5 项契约通过。覆盖加载回滚、同步拆机、满仓、快照恢复和 32 EU 包上限。 |
| P4-T1-HOST、P4-T1-HOST-QA | 通过原生 GameTest 验证新机、分数 EU 的二进制往返及 14 类损坏输入。两次开服由原生 ticker 驱动生产方块实体，开发模组提供可放置方块及燃料槽。 |

## 同一块煤跨两次开服

进程 A 通过方块物品在主世界 `(29,90,25)` 放置机器，先放入岩浆桶并观察其保留，再投入两煤。进程 B 在首次加工前读取同一位置。关服后另从 Anvil 区块读取持久化数据，核对槽中物品、整型余燃料和双精度储能。

| 时点 | 本次工作刻 | 余燃料刻 | 储能 | 剩煤 |
|---|---:|---:|---:|---:|
| A 正常保存 | 37 | 363 | 370 EU | 1 |
| B 冷启动读回 | 0 | 363 | 370 EU | 1 |
| B 首块煤烧完并满仓空闲 | 363 | 0 | 4,000 EU | 1 |
| B 从端口请求 64 EU，实际得到 32 EU | 363 | 0 | 3,968 EU | 1 |

拆机后原端口返回 0 EU，关服后区块内该位置的方块实体记录已移除。干净构建通过 42 项 JUnit 和 4 项 Forge GameTest。开发环境与安装态各自完成 42 项写入和 16 项读回检查。P2 的 36 项客户端检查和两个方向的错版握手均通过。产物指纹、各次 JVM 观测及三基线结果见[运行记录](gates/forge-1.19.2-machine-runtime.json)和[相似度记录](gates/forge-1.19.2-machine-similarity.json)。

从仓库根执行 `./gradlew clean build :test:runPlatformHostTest :test:runPlatformWorldTest` 可复跑常规验收。安装态运行使用 `./gradlew :forge:1.19.2:reobfQaFixtureJar` 生成独立夹具，然后按 `python3 src/forge/1.19.2/qa/packaged_machine.py --help` 指定生产 JAR、夹具 JAR、已安装的 Forge 库和 EULA 文件。下一批为 P3-T2/T3 的拓扑调度与传输结算、P4-T2 的分面库存。玩家可制作的发电机与配方随 P3-T5/P5-T3 验收。
