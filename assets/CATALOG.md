# 注册对象与重绘纹理目录

`REGISTRY-ART-CATALOG.csv` 将用户回交素材清单与修复版 1.2.4 实际注册表对照；每个命名空间 ID 只列一行。注册表记录于 2026-11-20，快照 SHA-256 为 `ebe12ca90ccc89216c94332390ecac41a8d7731081aae1d0cbcaac4462460370`。292 个方块 ID 与 552 个物品 ID 合并为 637 个不同 ID，其中 207 个同时具有方块和物品；与素材清单的 ID 集合完全一致。流体、实体和声音属于各自注册表，不因纹理文件增加方块或物品计数。

`runtime_block_1_2_4` / `runtime_item_1_2_4` 记录参考发行包中的实际注册类型；`user_redraw_png_paths` 是用户回交 ZIP 中对应的资源，不是新增注册对象。多个面、不同朝向、工作态、作物阶段、电池图标和颜色共用同一行；多个 ID 也可共用一张纹理。`external_reference_only` 表示素材清单里记载的外部依赖，并非项目已采用或再分发的素材。回交 ZIP 不包含独立橡胶树叶纹理，该项另从空白画布新绘。

`first_playable_target` 标记本轮目标的 39 项与额外的煤粉、铁粉；`energy_target`、`logistics_target` 和 `material_target` 分别标记扩展储电线缆、运输储罐和被动材料批次。其余行只是参考版已注册对象，不意味着本项目已实现、可造或可玩。静态 PNG 交付来源、授权待核状态和逐文件哈希见 [SOURCES.md](SOURCES.md) 与 [PRODUCTION-TEXTURES.csv](PRODUCTION-TEXTURES.csv)。
