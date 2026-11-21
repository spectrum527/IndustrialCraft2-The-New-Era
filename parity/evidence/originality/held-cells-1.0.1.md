# 1.0.1 手持流体单元专项复核

四版合计 871 个生产 Java 编译输入，按路径和 SHA-256 去重后共 358 份。其中有 327 份沿用 1.0.0 审查、8 份沿用本轮 PR 初审、12 份沿用整容器预充审查，最后 11 份在此轮对三个对照版本完成 33,176 对比较。最后 11 份的原始结果为 1 份通过、6 份警告、4 份超过 25%。这 11 份均无连续八行相同。

下表保留原始归一化 5-gram 比例。最后一列从双方都剔除包声明和导入，然后与同一份最高分对照重新计算。维护者于 2026-11-21 接受这四份文件的人工复核，并要求公开原始分数。路径或内容变化时须重新检查。

| 生产文件（相对 src/） | SHA-256 | 全文件 | 去除包声明和导入 |
| --- | --- | ---: | ---: |
| `forge/1.19.2/src/main/java/com/industrialage/ia2/logistics/CellItem.java` | `294b482286d44d29f43a8c3fc0bf176361014203f401fb6dcf7cfb929f38585f` | 30.857% | 29.435% |
| `forge/1.19.2/src/main/java/com/industrialage/ia2/logistics/CellPickup.java` | `99ad9f342e1bc1beefd3c421454fbf9a9867a47027e47fab18bf5a87fcbfa9e7` | 25.974% | 25.336% |
| `neoforge/1.21.1/src/main/java/com/industrialage/ia2/logistics/CellItem.java` | `5a51bef48dc2d502042eae9dfa4a45c3ec4d2e6e0f3664fdf89d10974be567e9` | 32.967% | 31.377% |
| `neoforge/26.x/src/main/java/com/industrialage/ia2/logistics/CellItem.java` | `cb8f5d78e49178e6d97639a2e2292cf02cb830133258d8f7e0077807ac98d404` | 26.819% | 25.160% |

隔离复核检查了右键分发、侧面权限、自然源取液、容器模拟与原子提交，以及创造模式物品判重。最长的保留标识符匹配出现在原生上下文初始化或权限调用中。两个 NeoForge 取液方法与 Forge 创造模式判重方法均未出现 16 个保留标识符词元连续相同。复核确认这些执行路径分别独立实现。

本次生产源码冻结于 NeoForge `cd62c987cd6ab480bde969762051e5671b09bfc4` 与 Forge `7587bad7204b72a1b3917f404730d00cf4fac8cb`。初审、整容器预充与最终复核的结果随公开发行审查记录提供。[1.0.1 发行审查](https://github.com/spectrum527/IndustrialCraft2-The-New-Era/releases/download/v1.0.1/RELEASE-AUDIT-1.0.1.json)
