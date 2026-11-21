# 1.0.5 工坊、工具与进度源码复核

维护者已接受表中三份确切的超阈值文件，原始分数完整保留。

四目标分别读取实际 Gradle 生产源集并逐一对照 Git blob，共 960 个输入。按路径和 SHA-256 去重共 395 组，344 组沿用 1.0.4 审查，51 组新增或变更。本轮与 2.9.40、2.8.222 及修复版 `1.2.4-port.20261120` 共有 153,816 对比较。原始结果 5 组通过、43 组警告、3 组失败。三份原始失败均由全文件 5-gram 超限触发，本轮连续八行命中为零。51 组均完成人工复核。

| 超阈值文件（相对 `src/`） | 现行 SHA-256 | 原始 5-gram | 去包声明和导入 | 仅执行体 |
| --- | --- | ---: | ---: | ---: |
| `forge/1.19.2/src/main/java/com/industrialage/ia2/forge/content/MachineBlock.java` | `1292ce623c3209e73a9ae7c7b941d8099a12d41c19daadb93abb529cf72a0f2d` | 26.5809% | 26.1438% | 22.6592% |
| `minecraft/shared/src/main/java/com/industrialage/ia2/industrial/BoxBlock.java` | `19cd9d3b27def24620400bac0fe96e9860e9a0cc310dede10da8372e13a69c20` | 30.7229% | 24.6753% | 20.8791% |
| `minecraft/shared/src/main/java/com/industrialage/ia2/progress/IndustrialProgress.java` | `33b98e5b194eeef2df2fed3fe2c1d5d86b9701807eace45e168376bc7a985371` | 34.6154% | 21.2121% | 24.1379% |

Forge 机器方块的最长归一化匹配为 30 个词元，位于项目已存在的原生移除回调。本轮把扳手处理移至统一入口，由机器保留已有的蓄电掉落逻辑。储物箱的新方法使用公共拆卸入口，复用本项目的所有者检查。共享进度入口由短守卫及一处平台委托构成，最长归一化匹配为 19 个词元。实际操作触发及其他进度条件由本项目原生平台适配和数据生成实现。三份均无连续 16 个保留标识符的可执行词元命中。

| 文件 | 1.0.4 SHA-256 | 1.0.4 原始 5-gram |
| --- | --- | ---: |
| forge/1.19.2 `MachineBlock.java` | `b40fee12bfc3395e29128bcb833ad6a68a2f099445b92a9fb8fb4aefa2b363b4` | 25.7246% |
| minecraft/shared `BoxBlock.java` | `9f907978ae4e59b08bf010f1ff1fcea1ffff1cf700cbe8994e12e172bdddf2b7` | 30.9598% |
| minecraft/shared `IndustrialProgress.java` | 本版新增 | — |

对应生产提交为 NeoForge `cb815cb1064d35a8166e19054ddf51b9e3181913`，Forge `88b855eb77fa7701bad344379118b58b4075d320`。1.0.4 的六份具体处置和其十个原始导入窗仍按原路径与 SHA 核对，1.0.3 的旧渲染签核亦按仍在发行中的原指纹延续。[1.0.4 源码审查](cell-families-1.0.4.md) · [1.0.3 源码审查](water-rotors-1.0.3.md)

两条发行线各 13 张手工 PNG、5 个新制 OGG 及 1 个成形焦炉模型均已核对登记与字节指纹。四份生产 JAR 内各 19 项资源与冻结档案相同。进度树完整包含 28 项与 1,020 点经验奖励。[素材来源登记](../../../assets/SOURCES.md)
