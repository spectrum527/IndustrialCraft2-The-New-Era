# 单元系列与装备的源码复核

四目标实际 Gradle 生产清单有 929 个输入，核对 NeoForge `2e54c089e3a59c087935f7161caefacbe4444490` 和 Forge `41d621a7ddb4c118e24fb550b91b04a8da10d4fb` 的 Git 文件指纹。去重后共 381 份，312 份沿用 1.0.3 审查，69 份重新审查，与 2.9.40、2.8.222、修复版 `1.2.4-port.20261120` 比较 208,104 对。原始结果为 5 份通过、58 份警告、6 份失败。69 份均完成人工审查和执行体分类。

## 现行原始失败

| 文件（相对 `src/`） | 本版 SHA-256 | 原始 5-gram | 去包声明和导入 | 最大执行体 5-gram |
| --- | --- | ---: | ---: | ---: |
| `forge/1.19.2/src/main/java/com/industrialage/ia2/forge/content/BatteryItem.java` | `8c5b76ee45d4b2af848331568f4f87dc66a555c66544dfbe1f4e712fedfda923` | 26.0606% | 22.6667% | 23.8806% |
| `forge/1.19.2/src/main/java/com/industrialage/ia2/logistics/CellItem.java` | `436d41c299855ef71216e014195c8c517aaa53923f075dfe4d452b216dcd39ca` | 31.2709% | 30.0341% | 27.6888% |
| `forge/1.19.2/src/main/java/com/industrialage/ia2/logistics/CellPickup.java` | `a65acac013b3e3a47bee3947c0c913aad8fe35ff1f66c90979d29106b77d6882` | 26.4317% | 25.7991% | 28.8235% |
| `forge/1.19.2/src/main/java/com/industrialage/ia2/logistics/CellRecipes.java` | `45fab8c4fd1ab7a9dcd7c9421330de401fcd0ff057feede1764257beeaa4630f` | 20.2991% | 18.6404% | 19.5219% |
| `neoforge/1.21.1/src/main/java/com/industrialage/ia2/logistics/CellItem.java` | `b11b2f1c4202044307d2c11549c4bda044cf87c14fa4219ba1620977c1e01617` | 33.7278% | 32.3232% | 28.4182% |
| `neoforge/26.x/src/main/java/com/industrialage/ia2/logistics/CellItem.java` | `9bab8a5eaee3942041f86057fb72849eff8bcdc7cdaa8478f26a4c83392affc6` | 29.4479% | 27.8826% | 24.2820% |

Forge 的新 `CellRecipes.java` 触发 6 个连续八行窗，来自对照 2.9.40 和修复版的三个重叠导入段。NeoForge 1.21.1 的 `CellItem.java` 触发 4 个连续八行窗，仍是与修复版相同的通用导入区。十个窗的候选和参照均为导入声明，执行体相同连续八行命中为零。Forge 的配方适配器原始 5-gram 为 20.2991%，单独记录导入复核。

Forge 电池的生产差异仅在创造栏属性；Forge 世界取源类使用原生容器能力的返回物品，移除了重复命名分支。Forge 单元类的最长字面和归一化连续匹配位于项目旧有的原生方块交互，新同族转型方法单独比较最高为 22.7273%。NeoForge 两版单元的实际端口短调用来自公共流体接口，新容器类型、液体与余量的处理经执行体复核。Forge 配方新增的两类原生配方适配、内容检查与同族返壳委托经单独复核。

## 上一版指纹

| 文件 | 1.0.3 的 SHA-256 | 1.0.3 原始 5-gram |
| --- | --- | ---: |
| forge/1.19.2 `BatteryItem.java` | `e719ba2311b6a233b2fddab79c61913fd0c67f4c6f8ab494b4f0b09e37239687` | 25.4545% |
| forge/1.19.2 `CellItem.java` | `b5a03adab0a0789c6301795524fb332e1d9db6af9f82f7e765c05344182cccb5` | 31.4501% |
| forge/1.19.2 `CellPickup.java` | `99ad9f342e1bc1beefd3c421454fbf9a9867a47027e47fab18bf5a87fcbfa9e7` | 25.9740% |
| forge/1.19.2 `CellRecipes.java` | 本版新增 | — |
| neoforge/1.21.1 `CellItem.java` | `2ad449f1f3421219b28aad2b9fd0a26fcac0f644090c8ed73895da48548acd64` | 36.9099% |
| neoforge/26.x `CellItem.java` | `a70fabda15b57a4975c2b8d49132a0123e9d9372fb7ff1e3c5039c03107a2b5b` | 30.2041% |

维护者接受表中五份原始分数超过 25% 的现行确切文件，并要求保留这些原始分数。Forge 配方适配器按双方只命中导入声明的复核登记，保留原始失败及全部六窗。1.0.1 的四份单元签核仅对应各自上一版指纹，现行裁决依本页新指纹。1.0.3 的原有渲染器专项签核按未变的指纹继续生效。[1.0.1 单元审查](held-cells-1.0.1.md) · [1.0.3 渲染审查](water-rotors-1.0.3.md)

两条发行线各 11 张手绘物品栏 PNG 与像素稿回执一致，四个成品 JAR 内相应文件也逐项匹配。[素材来源登记](../../../assets/SOURCES.md)
