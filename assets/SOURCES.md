# 素材来源登记

每项生产素材登记一行。`resource_path` 为成品中的完整路径，例如 `assets/ic2/textures/blocks/machine/processing/basic/macerator_front.png`。机器、物品、线缆的投产纹理一律登记为 `new`。共享材质按唯一包内路径登记，多个使用者列入批次交接。

- `status` 只能为 `legacy` 或 `new`；`date` 填写登记日期。
- `legacy` 素材只能存放于独立私有素材仓库，永不进入主仓库 Git 历史；新作素材协议不适用于它们。
- `new` 素材采用 CC BY-SA 4.0，须记录作者和来源；外包及社区投稿须同意该协议。
- 构建测试自造的临时资源不是生产素材，不得列入本表或混入成品。
- 转为公开项目之前，必须确认 `legacy` 为零，并核验完整生产资源清单。空表本身不能证明替换完成或通过发布门禁。

| resource_path | status | author | license | source | date |
|---|---|---|---|---|---|
| `assets/ic2/lang/en_us.json` | new | IC2 项目 | CC BY-SA 4.0 | P2-T7 客户端状态页英文文案 | 2026-11-19 |
| `assets/ic2/lang/zh_cn.json` | new | IC2 项目 | CC BY-SA 4.0 | P2-T7 客户端状态页中文文案 | 2026-11-19 |
| `assets/ic2/status/client.json` | new | IC2 项目 | CC BY-SA 4.0 | P2-T7 客户端状态资源默认文案 | 2026-11-19 |

## 基础切片重绘纹理（2026-11-20）

用户回交 `回交纹理.zip` 当前 SHA-256：`2ac848f7874700311f1e887a1717c5d5a83c9a0a3f8d72eb20d0daf36eb87cba`。具体包内源路径和每份成品的 SHA-256 见 [PRODUCTION-TEXTURES.csv](PRODUCTION-TEXTURES.csv)。回交说明确认重绘，但未单列作者及 CC 授权原文；以下许可为项目约定，公开前仍需核验授权。青铜穿戴层按游戏目标格式另存于项目自有命名空间。

| resource_path | status | author | license | source | date |
|---|---|---|---|---|---|
| `assets/ic2/textures/blocks/general/machine_bottom.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/general/machine_bottom.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/general/machine_side.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/general/machine_side.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/electric/generator_bottom.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/generator/electric/generator_bottom.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/electric/generator_front.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/generator/electric/generator_front.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/electric/generator_front_active.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/generator/electric/generator_front_active.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/electric/generator_leftrightback.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/generator/electric/generator_leftrightback.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/electric/generator_top.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/generator/electric/generator_top.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/basic/compressor_bottom.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/machine/processing/basic/compressor_bottom.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/basic/compressor_front.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/machine/processing/basic/compressor_front.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/basic/compressor_front_active.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/machine/processing/basic/compressor_front_active.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/basic/compressor_leftrightback.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/machine/processing/basic/compressor_leftrightback.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/basic/compressor_top.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/machine/processing/basic/compressor_top.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/basic/electric_furnace_bottom.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/machine/processing/basic/electric_furnace_bottom.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/basic/electric_furnace_front.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/machine/processing/basic/electric_furnace_front.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/basic/electric_furnace_front_active.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/machine/processing/basic/electric_furnace_front_active.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/basic/electric_furnace_leftrightback.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/machine/processing/basic/electric_furnace_leftrightback.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/basic/electric_furnace_top.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/machine/processing/basic/electric_furnace_top.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/basic/extractor_back.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/machine/processing/basic/extractor_back.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/basic/extractor_bottom.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/machine/processing/basic/extractor_bottom.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/basic/extractor_front.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/machine/processing/basic/extractor_front.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/basic/extractor_front_active.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/machine/processing/basic/extractor_front_active.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/basic/extractor_leftright.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/machine/processing/basic/extractor_leftright.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/basic/extractor_leftright_active.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/machine/processing/basic/extractor_leftright_active.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/basic/extractor_top.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/machine/processing/basic/extractor_top.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/basic/iron_furnace_bottom.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/machine/processing/basic/iron_furnace_bottom.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/basic/iron_furnace_front.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/machine/processing/basic/iron_furnace_front.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/basic/iron_furnace_front_active.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/machine/processing/basic/iron_furnace_front_active.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/basic/iron_furnace_leftrightback.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/machine/processing/basic/iron_furnace_leftrightback.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/basic/iron_furnace_top.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/machine/processing/basic/iron_furnace_top.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/basic/macerator_bottom.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/machine/processing/basic/macerator_bottom.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/basic/macerator_front.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/machine/processing/basic/macerator_front.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/basic/macerator_front_active.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/machine/processing/basic/macerator_front_active.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/basic/macerator_leftrightback.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/machine/processing/basic/macerator_leftrightback.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/basic/macerator_top.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/machine/processing/basic/macerator_top.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/basic/macerator_top_active.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/machine/processing/basic/macerator_top_active.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/resource/machine.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/resource/machine.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/resource/ore/deepslate_tin_ore.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/resource/ore/deepslate_tin_ore.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/resource/ore/tin_ore.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/resource/ore/tin_ore.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/resource/rubber_leaves.png` | new | IC2 项目（本轮像素绘制） | CC BY-SA 4.0（项目约定；授权待核） | 空白画布新绘 | 2026-11-20 |
| `assets/ic2/textures/blocks/resource/rubber_sapling.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/resource/rubber_sapling.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/resource/rubber_wood_bottomtop.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/resource/rubber_wood_bottomtop.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/resource/rubber_wood_dry_bottomtop.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/resource/rubber_wood_dry_bottomtop.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/resource/rubber_wood_dry_front.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/resource/rubber_wood_dry_front.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/resource/rubber_wood_dry_leftrightback.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/resource/rubber_wood_dry_leftrightback.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/resource/rubber_wood_sides.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/resource/rubber_wood_sides.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/resource/rubber_wood_wet_bottomtop.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/resource/rubber_wood_wet_bottomtop.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/resource/rubber_wood_wet_front.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/resource/rubber_wood_wet_front.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/resource/rubber_wood_wet_leftrightback.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/resource/rubber_wood_wet_leftrightback.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/copper_cable_0.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/cable/copper_cable_0.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/copper_cable_1_black.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/cable/copper_cable_1_black.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/copper_cable_1_blue.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/cable/copper_cable_1_blue.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/copper_cable_1_brown.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/cable/copper_cable_1_brown.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/copper_cable_1_cyan.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/cable/copper_cable_1_cyan.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/copper_cable_1_gray.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/cable/copper_cable_1_gray.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/copper_cable_1_green.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/cable/copper_cable_1_green.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/copper_cable_1_light_blue.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/cable/copper_cable_1_light_blue.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/copper_cable_1_light_gray.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/cable/copper_cable_1_light_gray.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/copper_cable_1_lime.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/cable/copper_cable_1_lime.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/copper_cable_1_magenta.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/cable/copper_cable_1_magenta.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/copper_cable_1_orange.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/cable/copper_cable_1_orange.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/copper_cable_1_pink.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/cable/copper_cable_1_pink.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/copper_cable_1_purple.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/cable/copper_cable_1_purple.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/copper_cable_1_red.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/cable/copper_cable_1_red.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/copper_cable_1_white.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/cable/copper_cable_1_white.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/copper_cable_1_yellow.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/cable/copper_cable_1_yellow.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/tin_cable_0.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/cable/tin_cable_0.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/tin_cable_1_black.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/cable/tin_cable_1_black.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/tin_cable_1_blue.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/cable/tin_cable_1_blue.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/tin_cable_1_brown.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/cable/tin_cable_1_brown.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/tin_cable_1_cyan.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/cable/tin_cable_1_cyan.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/tin_cable_1_gray.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/cable/tin_cable_1_gray.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/tin_cable_1_green.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/cable/tin_cable_1_green.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/tin_cable_1_light_blue.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/cable/tin_cable_1_light_blue.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/tin_cable_1_light_gray.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/cable/tin_cable_1_light_gray.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/tin_cable_1_lime.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/cable/tin_cable_1_lime.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/tin_cable_1_magenta.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/cable/tin_cable_1_magenta.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/tin_cable_1_orange.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/cable/tin_cable_1_orange.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/tin_cable_1_pink.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/cable/tin_cable_1_pink.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/tin_cable_1_purple.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/cable/tin_cable_1_purple.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/tin_cable_1_red.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/cable/tin_cable_1_red.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/tin_cable_1_white.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/cable/tin_cable_1_white.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/tin_cable_1_yellow.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/cable/tin_cable_1_yellow.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/storage/batbox_back.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/storage/batbox_back.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/storage/batbox_bottom.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/storage/batbox_bottom.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/storage/batbox_front.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/storage/batbox_front.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/storage/batbox_leftright.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/storage/batbox_leftright.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/storage/batbox_top.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/storage/batbox_top.png` | 2026-11-20 |
| `assets/ic2/textures/entity/equipment/humanoid/bronze.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/minecraft/textures/models/armor/ic2_bronze_layer_1.png` | 2026-11-20 |
| `assets/ic2/textures/entity/equipment/humanoid_leggings/bronze.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/minecraft/textures/models/armor/ic2_bronze_layer_2.png` | 2026-11-20 |
| `assets/ic2/textures/items/armor/bronze_boots.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/armor/bronze_boots.png` | 2026-11-20 |
| `assets/ic2/textures/items/armor/bronze_chestplate.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/armor/bronze_chestplate.png` | 2026-11-20 |
| `assets/ic2/textures/items/armor/bronze_helmet.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/armor/bronze_helmet.png` | 2026-11-20 |
| `assets/ic2/textures/items/armor/bronze_leggings.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/armor/bronze_leggings.png` | 2026-11-20 |
| `assets/ic2/textures/items/battery/re_battery_0.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/battery/re_battery_0.png` | 2026-11-20 |
| `assets/ic2/textures/items/battery/re_battery_1.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/battery/re_battery_1.png` | 2026-11-20 |
| `assets/ic2/textures/items/battery/re_battery_2.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/battery/re_battery_2.png` | 2026-11-20 |
| `assets/ic2/textures/items/battery/re_battery_3.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/battery/re_battery_3.png` | 2026-11-20 |
| `assets/ic2/textures/items/battery/re_battery_4.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/battery/re_battery_4.png` | 2026-11-20 |
| `assets/ic2/textures/items/cable/copper_cable_0.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/cable/copper_cable_0.png` | 2026-11-20 |
| `assets/ic2/textures/items/cable/copper_cable_1.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/cable/copper_cable_1.png` | 2026-11-20 |
| `assets/ic2/textures/items/cable/tin_cable_0.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/cable/tin_cable_0.png` | 2026-11-20 |
| `assets/ic2/textures/items/cable/tin_cable_1.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/cable/tin_cable_1.png` | 2026-11-20 |
| `assets/ic2/textures/items/crafting/circuit.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/crafting/circuit.png` | 2026-11-20 |
| `assets/ic2/textures/items/crafting/rubber.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/crafting/rubber.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/casing/tin.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/casing/tin.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/crushed/copper.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/crushed/copper.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/crushed/tin.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/crushed/tin.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/dust/bronze.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/dust/bronze.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/dust/coal.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/dust/coal.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/dust/iron.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/dust/iron.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/ingot/bronze.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/ingot/bronze.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/ingot/tin.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/ingot/tin.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/plate/copper.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/plate/copper.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/plate/iron.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/plate/iron.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/plate/tin.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/plate/tin.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/raw/tin.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/raw/tin.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/resin.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/resin.png` | 2026-11-20 |
| `assets/ic2/textures/items/tool/bronze_pickaxe.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/tool/bronze_pickaxe.png` | 2026-11-20 |
| `assets/ic2/textures/items/tool/cutter.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/tool/cutter.png` | 2026-11-20 |
| `assets/ic2/textures/items/tool/forge_hammer.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/tool/forge_hammer.png` | 2026-11-20 |
| `assets/ic2/textures/items/tool/treetap.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/tool/treetap.png` | 2026-11-20 |
| `assets/ic2/textures/models/armor/bronze_layer_1.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/minecraft/textures/models/armor/ic2_bronze_layer_1.png` | 2026-11-20 |
| `assets/ic2/textures/models/armor/bronze_layer_2.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/minecraft/textures/models/armor/ic2_bronze_layer_2.png` | 2026-11-20 |

## 运输与储罐批次（2026-11-20）

颜色派生图按回交素材的运行时着色台账逐像素相乘，保留透明度；原始重绘 PNG 仍以旧资源键保留。授权原文仍待核验。

| resource_path | status | author | license | source | date |
|---|---|---|---|---|---|
| `assets/ic2/textures/blocks/bronze_tank_cap.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | `assets/ic2/textures/blocks/bronze_tank_cap.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/bronze_tank_side.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | `assets/ic2/textures/blocks/bronze_tank_side.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/tank_bottom.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | `assets/ic2/textures/blocks/machine/fluid/tank_bottom.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/tank_side.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | `assets/ic2/textures/blocks/machine/fluid/tank_side.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/tank_top.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | `assets/ic2/textures/blocks/machine/fluid/tank_top.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/transport/bronze/pipe_large.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | `assets/ic2/textures/blocks/transport/pipe_large.png，逐像素乘色 #AE5111` | 2026-11-20 |
| `assets/ic2/textures/blocks/transport/bronze/pipe_medium.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | `assets/ic2/textures/blocks/transport/pipe_medium.png，逐像素乘色 #AE5111` | 2026-11-20 |
| `assets/ic2/textures/blocks/transport/bronze/pipe_side.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | `assets/ic2/textures/blocks/transport/pipe_side.png，逐像素乘色 #AE5111` | 2026-11-20 |
| `assets/ic2/textures/blocks/transport/bronze/pipe_small.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | `assets/ic2/textures/blocks/transport/pipe_small.png，逐像素乘色 #AE5111` | 2026-11-20 |
| `assets/ic2/textures/blocks/transport/bronze/pipe_tiny.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | `assets/ic2/textures/blocks/transport/pipe_tiny.png，逐像素乘色 #AE5111` | 2026-11-20 |
| `assets/ic2/textures/blocks/transport/pipe_large.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | `assets/ic2/textures/blocks/transport/pipe_large.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/transport/pipe_medium.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | `assets/ic2/textures/blocks/transport/pipe_medium.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/transport/pipe_side.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | `assets/ic2/textures/blocks/transport/pipe_side.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/transport/pipe_small.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | `assets/ic2/textures/blocks/transport/pipe_small.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/transport/pipe_tiny.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | `assets/ic2/textures/blocks/transport/pipe_tiny.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/transport/steel/pipe_large.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | `assets/ic2/textures/blocks/transport/pipe_large.png，逐像素乘色 #808080` | 2026-11-20 |
| `assets/ic2/textures/blocks/transport/steel/pipe_medium.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | `assets/ic2/textures/blocks/transport/pipe_medium.png，逐像素乘色 #808080` | 2026-11-20 |
| `assets/ic2/textures/blocks/transport/steel/pipe_side.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | `assets/ic2/textures/blocks/transport/pipe_side.png，逐像素乘色 #808080` | 2026-11-20 |
| `assets/ic2/textures/blocks/transport/steel/pipe_small.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | `assets/ic2/textures/blocks/transport/pipe_small.png，逐像素乘色 #808080` | 2026-11-20 |
| `assets/ic2/textures/blocks/transport/steel/pipe_tiny.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | `assets/ic2/textures/blocks/transport/pipe_tiny.png，逐像素乘色 #808080` | 2026-11-20 |
| `assets/ic2/textures/items/cover/pump_background.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | `assets/ic2/textures/items/cover/pump_background.png` | 2026-11-20 |
| `assets/ic2/textures/items/cover/pump_foreground.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | `assets/ic2/textures/items/cover/pump_foreground.png` | 2026-11-20 |
| `assets/ic2/textures/items/cover/pump_foreground_lv.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | `assets/ic2/textures/items/cover/pump_foreground.png，逐像素乘色 #00FF00` | 2026-11-20 |
| `assets/ic2/textures/items/cover/pump_foreground_mv.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | `assets/ic2/textures/items/cover/pump_foreground.png，逐像素乘色 #FFFF00` | 2026-11-20 |
| `assets/ic2/textures/items/resource/casing/bronze.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | `assets/ic2/textures/items/resource/casing/bronze.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/casing/gold.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | `assets/ic2/textures/items/resource/casing/gold.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/casing/steel.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | `assets/ic2/textures/items/resource/casing/steel.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/ingot/steel.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | `assets/ic2/textures/items/resource/ingot/steel.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/plate/bronze.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | `assets/ic2/textures/items/resource/plate/bronze.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/plate/gold.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | `assets/ic2/textures/items/resource/plate/gold.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/plate/steel.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | `assets/ic2/textures/items/resource/plate/steel.png` | 2026-11-20 |
| `assets/ic2/textures/items/tool/crowbar.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | `assets/ic2/textures/items/tool/crowbar.png` | 2026-11-20 |
| `assets/ic2/textures/items/tool/wrench.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | `assets/ic2/textures/items/tool/wrench.png` | 2026-11-20 |

## 扩展储电和电缆批次（2026-11-20）

| resource_path | status | author | license | source | date |
|---|---|---|---|---|---|
| `assets/ic2/textures/blocks/wiring/cable/detector_cable_0.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/cable/detector_cable_0.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/detector_cable_0_active.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/cable/detector_cable_0_active.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/glass_cable_0_white.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/cable/glass_cable_0_white.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/gold_cable_0.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/cable/gold_cable_0.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/gold_cable_1_black.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/cable/gold_cable_1_black.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/gold_cable_2_black.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/cable/gold_cable_2_black.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/iron_cable_0.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/cable/iron_cable_0.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/iron_cable_1_black.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/cable/iron_cable_1_black.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/iron_cable_2_black.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/cable/iron_cable_2_black.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/iron_cable_3_black.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/cable/iron_cable_3_black.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/splitter_cable_0.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/cable/splitter_cable_0.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/splitter_cable_0_active.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/cable/splitter_cable_0_active.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/storage/cesu_back.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/storage/cesu_back.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/storage/cesu_bottomtop.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/storage/cesu_bottomtop.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/storage/cesu_front.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/storage/cesu_front.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/storage/cesu_leftright.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/storage/cesu_leftright.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/storage/mfe_back.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/storage/mfe_back.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/storage/mfe_bottomtop.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/storage/mfe_bottomtop.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/storage/mfe_front.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/storage/mfe_front.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/storage/mfe_leftright.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/storage/mfe_leftright.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/storage/mfsu_bottomtop.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/storage/mfsu_bottomtop.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/storage/mfsu_front.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/storage/mfsu_front.png` | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/storage/mfsu_leftrightback.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/blocks/wiring/storage/mfsu_leftrightback.png` | 2026-11-20 |
| `assets/ic2/textures/items/cable/detector_cable.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/cable/detector_cable.png` | 2026-11-20 |
| `assets/ic2/textures/items/cable/glass_cable.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/cable/glass_cable.png` | 2026-11-20 |
| `assets/ic2/textures/items/cable/gold_cable_0.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/cable/gold_cable_0.png` | 2026-11-20 |
| `assets/ic2/textures/items/cable/gold_cable_1.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/cable/gold_cable_1.png` | 2026-11-20 |
| `assets/ic2/textures/items/cable/gold_cable_2.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/cable/gold_cable_2.png` | 2026-11-20 |
| `assets/ic2/textures/items/cable/iron_cable_0.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/cable/iron_cable_0.png` | 2026-11-20 |
| `assets/ic2/textures/items/cable/iron_cable_1.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/cable/iron_cable_1.png` | 2026-11-20 |
| `assets/ic2/textures/items/cable/iron_cable_2.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/cable/iron_cable_2.png` | 2026-11-20 |
| `assets/ic2/textures/items/cable/iron_cable_3.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/cable/iron_cable_3.png` | 2026-11-20 |
| `assets/ic2/textures/items/cable/splitter_cable.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/cable/splitter_cable.png` | 2026-11-20 |
| `assets/ic2/textures/items/cell/empty_cell.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/cell/empty_cell.png` | 2026-11-20 |

## 被动加工材料批次（2026-11-20）

| resource_path | status | author | license | source | date |
|---|---|---|---|---|---|
| `assets/ic2/textures/items/crafting/advanced_circuit.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/crafting/advanced_circuit.png` | 2026-11-20 |
| `assets/ic2/textures/items/crafting/alloy.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/crafting/alloy.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/ashes.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/ashes.png` | 2026-11-20 |
| `assets/ic2/textures/items/crafting/bio_chaff.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/crafting/bio_chaff.png` | 2026-11-20 |
| `assets/ic2/textures/items/crafting/carbon_fibre.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/crafting/carbon_fibre.png` | 2026-11-20 |
| `assets/ic2/textures/items/crafting/carbon_mesh.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/crafting/carbon_mesh.png` | 2026-11-20 |
| `assets/ic2/textures/items/crafting/carbon_plate.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/crafting/carbon_plate.png` | 2026-11-20 |
| `assets/ic2/textures/items/crafting/cf_powder.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/crafting/cf_powder.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/dust/clay.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/dust/clay.png` | 2026-11-20 |
| `assets/ic2/textures/items/crafting/coal_ball.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/crafting/coal_ball.png` | 2026-11-20 |
| `assets/ic2/textures/items/crafting/coal_block.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/crafting/coal_block.png` | 2026-11-20 |
| `assets/ic2/textures/items/crafting/coal_chunk.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/crafting/coal_chunk.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/dust/coal_fuel.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/dust/coal_fuel.png` | 2026-11-20 |
| `assets/ic2/textures/items/crafting/coil.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/crafting/coil.png` | 2026-11-20 |
| `assets/ic2/textures/items/coin.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/coin.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/coke.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/coke.png` | 2026-11-20 |
| `assets/ic2/textures/items/crafting/compressed_hydrated_coal.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/crafting/compressed_hydrated_coal.png` | 2026-11-20 |
| `assets/ic2/textures/items/crafting/compressed_plants.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/crafting/compressed_plants.png` | 2026-11-20 |
| `assets/ic2/textures/items/crafting/copper_boiler.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/crafting/copper_boiler.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/casing/copper.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/casing/copper.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/dust/copper.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/dust/copper.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/ingot/copper.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/ingot/copper.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/crushed/gold.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/crushed/gold.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/crushed/iron.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/crushed/iron.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/crushed/lead.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/crushed/lead.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/crushed/silver.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/crushed/silver.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/crushed/uranium.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/crushed/uranium.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/plate/dense_bronze.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/plate/dense_bronze.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/plate/dense_copper.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/plate/dense_copper.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/plate/dense_gold.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/plate/dense_gold.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/plate/dense_iron.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/plate/dense_iron.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/plate/dense_lapis.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/plate/dense_lapis.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/plate/dense_lead.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/plate/dense_lead.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/plate/dense_obsidian.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/plate/dense_obsidian.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/plate/dense_steel.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/plate/dense_steel.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/plate/dense_tin.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/plate/dense_tin.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/dust/diamond.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/dust/diamond.png` | 2026-11-20 |
| `assets/ic2/textures/items/crafting/electric_motor.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/crafting/electric_motor.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/dust/emerald.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/dust/emerald.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/dust/ender_pearl.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/dust/ender_pearl.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/dust/energium.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/dust/energium.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/dust/gold.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/dust/gold.png` | 2026-11-20 |
| `assets/ic2/textures/items/crafting/heat_conductor.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/crafting/heat_conductor.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/dust/tin_hydrated.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/dust/tin_hydrated.png` | 2026-11-20 |
| `assets/ic2/textures/items/crafting/industrial_diamond.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/crafting/industrial_diamond.png` | 2026-11-20 |
| `assets/ic2/textures/items/crafting/iridium.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/crafting/iridium.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/iridium_ore.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/iridium_ore.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/iridium_shard.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/iridium_shard.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/casing/iron.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/casing/iron.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/dust/lapis.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/dust/lapis.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/plate/lapis.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/plate/lapis.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/casing/lead.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/casing/lead.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/dust/lead.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/dust/lead.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/ingot/lead.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/ingot/lead.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/plate/lead.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/plate/lead.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/dust/lithium.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/dust/lithium.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/ingot/mixed_metal.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/ingot/mixed_metal.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/nuclear/mox.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/nuclear/mox.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/nuclear/mox_pellet.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/nuclear/mox_pellet.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/dust/netherrack.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/dust/netherrack.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/dust/obsidian.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/dust/obsidian.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/plate/obsidian.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/plate/obsidian.png` | 2026-11-20 |
| `assets/ic2/textures/items/crafting/pellet.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/crafting/pellet.png` | 2026-11-20 |
| `assets/ic2/textures/items/crafting/plant_ball.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/crafting/plant_ball.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/nuclear/plutonium.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/nuclear/plutonium.png` | 2026-11-20 |
| `assets/ic2/textures/items/crafting/power_unit.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/crafting/power_unit.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/purified/copper.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/purified/copper.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/purified/gold.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/purified/gold.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/purified/iron.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/purified/iron.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/purified/lead.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/purified/lead.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/purified/silver.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/purified/silver.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/purified/tin.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/purified/tin.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/purified/uranium.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/purified/uranium.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/raw/lead.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/raw/lead.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/raw/uranium.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/raw/uranium.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/ingot/refined_iron.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/ingot/refined_iron.png` | 2026-11-20 |
| `assets/ic2/textures/items/crafting/scrap.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/crafting/scrap.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/dust/silicon_dioxide.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/dust/silicon_dioxide.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/dust/silver.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/dust/silver.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/ingot/silver.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/ingot/silver.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/slag.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/slag.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/dust/small_bronze.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/dust/small_bronze.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/dust/small_copper.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/dust/small_copper.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/dust/small_diamond.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/dust/small_diamond.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/dust/small_emerald.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/dust/small_emerald.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/dust/small_gold.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/dust/small_gold.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/dust/small_iron.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/dust/small_iron.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/dust/small_lapis.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/dust/small_lapis.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/dust/small_lead.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/dust/small_lead.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/dust/small_lithium.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/dust/small_lithium.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/dust/small_obsidian.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/dust/small_obsidian.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/nuclear/small_plutonium.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/nuclear/small_plutonium.png` | 2026-11-20 |
| `assets/ic2/textures/items/crafting/small_power_unit.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/crafting/small_power_unit.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/dust/small_silver.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/dust/small_silver.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/dust/small_sulfur.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/dust/small_sulfur.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/dust/small_tin.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/dust/small_tin.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/nuclear/small_uranium_235.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/nuclear/small_uranium_235.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/nuclear/small_uranium_238.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/nuclear/small_uranium_238.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/dust/stone.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/dust/stone.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/dust/sulfur.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/dust/sulfur.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/dust/tin.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/dust/tin.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/nuclear/uranium.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/nuclear/uranium.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/nuclear/uranium_235.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/nuclear/uranium_235.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/nuclear/uranium_238.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/nuclear/uranium_238.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/ingot/uranium.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/ingot/uranium.png` | 2026-11-20 |
| `assets/ic2/textures/items/resource/nuclear/uranium_pellet.png` | new | IC2 重绘项目（用户回交，作者未单列） | CC BY-SA 4.0（项目约定；授权待核） | 用户回交 ZIP：`assets/ic2/textures/items/resource/nuclear/uranium_pellet.png` | 2026-11-20 |
| `assets/ic2/textures/armor/jetpack_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/bcTrigger/capacitor_empty.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/bcTrigger/capacitor_full.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/bcTrigger/capacitor_has_energy.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/bcTrigger/capacitor_has_room.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/bcTrigger/charge_empty.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/bcTrigger/charge_full.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/bcTrigger/charge_partial.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/bcTrigger/discharge_empty.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/bcTrigger/discharge_full.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/bcTrigger/discharge_partial.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/bcTrigger/energy_flowing.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/bcTrigger/energy_not_flowing.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/bcTrigger/full_heat.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/bcTrigger/has_fuel.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/bcTrigger/has_scrap.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/bcTrigger/no_fuel.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/bcTrigger/no_full_heat.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/bcTrigger/no_scrap.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/bcTrigger/not_working.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/bcTrigger/working.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/barrel_bottomtop.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/barrel_sides.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/barrel_tap.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/cf/foam.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/cf/reinforced_foam.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/cf/wall_black.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/cf/wall_black_smooth.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/cf/wall_blue.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/cf/wall_blue_smooth.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/cf/wall_brown.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/cf/wall_brown_smooth.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/cf/wall_cyan.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/cf/wall_cyan_smooth.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/cf/wall_gray.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/cf/wall_gray_smooth.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/cf/wall_green.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/cf/wall_green_smooth.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/cf/wall_light_blue.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/cf/wall_light_blue_smooth.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/cf/wall_light_gray.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/cf/wall_light_gray_smooth.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/cf/wall_lime.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/cf/wall_lime_smooth.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/cf/wall_magenta.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/cf/wall_magenta_smooth.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/cf/wall_orange.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/cf/wall_orange_smooth.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/cf/wall_pink.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/cf/wall_pink_smooth.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/cf/wall_purple.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/cf/wall_purple_smooth.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/cf/wall_red.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/cf/wall_red_smooth.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/cf/wall_white.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/cf/wall_white_smooth.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/cf/wall_yellow.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/cf/wall_yellow_smooth.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/acacia_sapling_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/acacia_sapling_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/acacia_sapling_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/acacia_sapling_3.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/acacia_sapling_4.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/aurelia_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/aurelia_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/aurelia_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/aurelia_3.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/aurelia_4.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/beetroots_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/beetroots_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/beetroots_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/beetroots_3.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/birch_sapling_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/birch_sapling_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/birch_sapling_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/birch_sapling_3.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/birch_sapling_4.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/blackthorn_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/blackthorn_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/blackthorn_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/blackthorn_3.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/blazereed_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/blazereed_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/blazereed_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/blazereed_3.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/bobs_yer_uncle_ranks_berries_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/bobs_yer_uncle_ranks_berries_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/bobs_yer_uncle_ranks_berries_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/bobs_yer_uncle_ranks_berries_3.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/brown_mushroom_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/brown_mushroom_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/brown_mushroom_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/carrots_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/carrots_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/carrots_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/carrots_3.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/cocoa_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/cocoa_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/cocoa_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/cocoa_3.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/coffee_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/coffee_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/coffee_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/coffee_3.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/coffee_4.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/corium_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/corium_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/corium_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/corium_3.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/corpse_plant_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/corpse_plant_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/corpse_plant_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/corpse_plant_3.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/creeper_weed_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/creeper_weed_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/creeper_weed_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/creeper_weed_3.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/cyazint_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/cyazint_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/cyazint_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/cyazint_3.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/cyprium_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/cyprium_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/cyprium_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/cyprium_3.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/dandelion_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/dandelion_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/dandelion_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/dandelion_3.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/dark_oak_sapling_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/dark_oak_sapling_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/dark_oak_sapling_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/dark_oak_sapling_3.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/dark_oak_sapling_4.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/diareed_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/diareed_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/diareed_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/diareed_3.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/eating_plant_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/eating_plant_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/eating_plant_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/eating_plant_3.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/eating_plant_4.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/eating_plant_5.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/egg_plant_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/egg_plant_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/egg_plant_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/ender_blossom_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/ender_blossom_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/ender_blossom_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/ender_blossom_3.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/ferru_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/ferru_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/ferru_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/ferru_3.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/flax_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/flax_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/flax_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/flax_3.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/hops_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/hops_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/hops_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/hops_3.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/hops_4.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/hops_5.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/hops_6.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/infested_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/infested_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/infested_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/infested_3.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/infested_4.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/jungle_sapling_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/jungle_sapling_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/jungle_sapling_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/jungle_sapling_3.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/jungle_sapling_4.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/meat_rose_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/meat_rose_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/meat_rose_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/meat_rose_3.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/melon_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/melon_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/melon_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/melon_3.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/milk_wart_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/milk_wart_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/milk_wart_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/nether_wart_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/nether_wart_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/nether_wart_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/oak_sapling_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/oak_sapling_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/oak_sapling_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/oak_sapling_3.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/oak_sapling_4.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/oil_berries_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/oil_berries_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/oil_berries_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/plumbiscus_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/plumbiscus_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/plumbiscus_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/plumbiscus_3.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/poppy_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/poppy_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/poppy_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/poppy_3.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/potato_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/potato_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/potato_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/potato_3.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/pumpkin_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/pumpkin_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/pumpkin_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/pumpkin_3.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/red_mushroom_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/red_mushroom_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/red_mushroom_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/red_wheat_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/red_wheat_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/red_wheat_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/red_wheat_3.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/red_wheat_4.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/red_wheat_5.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/red_wheat_6.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/reed_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/reed_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/reed_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/reed_33.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/shining_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/shining_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/shining_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/shining_3.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/shining_4.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/slime_plant_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/slime_plant_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/slime_plant_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/slime_plant_3.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/spidernip_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/spidernip_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/spidernip_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/spidernip_3.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/spruce_sapling_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/spruce_sapling_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/spruce_sapling_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/spruce_sapling_3.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/spruce_sapling_4.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/stagnium_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/stagnium_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/stagnium_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/stagnium_3.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/stick.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/stick2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/stick_upgraded.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/sticky_reed_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/sticky_reed_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/sticky_reed_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/sticky_reed_3.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/tearstalks_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/tearstalks_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/tearstalks_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/tearstalks_3.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/terra_wart_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/terra_wart_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/terra_wart_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/tulip_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/tulip_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/tulip_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/tulip_3.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/venomilia_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/venomilia_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/venomilia_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/venomilia_3.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/venomilia_4.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/venomilia_5.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/weed_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/weed_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/weed_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/weed_3.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/weed_4.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/wheat_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/wheat_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/wheat_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/wheat_3.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/wheat_4.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/wheat_5.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/wheat_6.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/wheat_7.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/withereed_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/withereed_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/withereed_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/crop/withereed_3.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/explosive/dynamite.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/explosive/dynamite_remote.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/explosive/itnt_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/explosive/itnt_sides.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/explosive/itnt_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/explosive/nuke_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/explosive/nuke_sides.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/explosive/nuke_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/fence_iron.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/fluid/coolant_flow.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/fluid/coolant_flow.png.mcmeta` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 重绘帧序列的播放参数，按游戏资源元数据核对 | 2026-11-20 |
| `assets/ic2/textures/blocks/fluid/coolant_still.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/fluid/coolant_still.png.mcmeta` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 重绘帧序列的播放参数，按游戏资源元数据核对 | 2026-11-20 |
| `assets/ic2/textures/blocks/fluid/fluid_2_still.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/fluid/fluid_2_still.png.mcmeta` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 重绘帧序列的播放参数，按游戏资源元数据核对 | 2026-11-20 |
| `assets/ic2/textures/blocks/fluid/fluid_3_still.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/fluid/fluid_3_still.png.mcmeta` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 重绘帧序列的播放参数，按游戏资源元数据核对 | 2026-11-20 |
| `assets/ic2/textures/blocks/fluid/fluid_flow.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/fluid/fluid_flow.png.mcmeta` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 重绘帧序列的播放参数，按游戏资源元数据核对 | 2026-11-20 |
| `assets/ic2/textures/blocks/fluid/fluid_still.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/fluid/fluid_still.png.mcmeta` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 重绘帧序列的播放参数，按游戏资源元数据核对 | 2026-11-20 |
| `assets/ic2/textures/blocks/fluid/fluid_water_flow.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/fluid/fluid_water_flow.png.mcmeta` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 重绘帧序列的播放参数，按游戏资源元数据核对 | 2026-11-20 |
| `assets/ic2/textures/blocks/fluid/fluid_water_still.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/fluid/fluid_water_still.png.mcmeta` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 重绘帧序列的播放参数，按游戏资源元数据核对 | 2026-11-20 |
| `assets/ic2/textures/blocks/fluid/hot_coolant_flow.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/fluid/hot_coolant_flow.png.mcmeta` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 重绘帧序列的播放参数，按游戏资源元数据核对 | 2026-11-20 |
| `assets/ic2/textures/blocks/fluid/hot_coolant_still.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/fluid/hot_coolant_still.png.mcmeta` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 重绘帧序列的播放参数，按游戏资源元数据核对 | 2026-11-20 |
| `assets/ic2/textures/blocks/fluid/pahoehoe_lava_still.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/fluid/pahoehoe_lava_still.png.mcmeta` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 重绘帧序列的播放参数，按游戏资源元数据核对 | 2026-11-20 |
| `assets/ic2/textures/blocks/fluid/uu_matter_flow.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/fluid/uu_matter_flow.png.mcmeta` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 重绘帧序列的播放参数，按游戏资源元数据核对 | 2026-11-20 |
| `assets/ic2/textures/blocks/fluid/uu_matter_still.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/fluid/uu_matter_still.png.mcmeta` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 重绘帧序列的播放参数，按游戏资源元数据核对 | 2026-11-20 |
| `assets/ic2/textures/blocks/fluid/weed_ex_still.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/fluid/weed_ex_still.png.mcmeta` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 重绘帧序列的播放参数，按游戏资源元数据核对 | 2026-11-20 |
| `assets/ic2/textures/blocks/general/machine_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/electric/creative_generator_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/electric/creative_generator_side.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/electric/creative_generator_side_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/electric/creative_generator_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/electric/geo_generator_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/electric/geo_generator_front.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/electric/geo_generator_front_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/electric/geo_generator_leftrightback.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/electric/geo_generator_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/electric/kinetic_generator_back.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/electric/kinetic_generator_back_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/electric/kinetic_generator_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/electric/kinetic_generator_front.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/electric/kinetic_generator_leftright.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/electric/kinetic_generator_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/electric/rt_generator_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/electric/rt_generator_sides.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/electric/rt_generator_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/electric/rt_generator_top_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/electric/semifluid_generator_back.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/electric/semifluid_generator_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/electric/semifluid_generator_front.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/electric/semifluid_generator_front_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/electric/semifluid_generator_leftright.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/electric/semifluid_generator_leftright_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/electric/semifluid_generator_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/electric/solar_generator_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/electric/solar_generator_sides.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/electric/solar_generator_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/electric/stirling_generator_back.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/electric/stirling_generator_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/electric/stirling_generator_front.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/electric/stirling_generator_front_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/electric/stirling_generator_leftright.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/electric/stirling_generator_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/electric/water_generator_back.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/electric/water_generator_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/electric/water_generator_front.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/electric/water_generator_left.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/electric/water_generator_right.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/electric/water_generator_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/electric/wind_generator_back.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/electric/wind_generator_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/electric/wind_generator_front.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/electric/wind_generator_leftright.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/electric/wind_generator_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/heat/electric_heat_generator_back.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/heat/electric_heat_generator_back_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/heat/electric_heat_generator_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/heat/electric_heat_generator_front.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/heat/electric_heat_generator_front_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/heat/electric_heat_generator_leftright.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/heat/electric_heat_generator_leftright_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/heat/electric_heat_generator_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/heat/electric_heat_generator_top_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/heat/fluid_heat_generator_back.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/heat/fluid_heat_generator_back_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/heat/fluid_heat_generator_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/heat/fluid_heat_generator_front.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/heat/fluid_heat_generator_front_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/heat/fluid_heat_generator_leftright.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/heat/fluid_heat_generator_leftright_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/heat/fluid_heat_generator_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/heat/rt_heat_generator_back.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/heat/rt_heat_generator_back_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/heat/rt_heat_generator_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/heat/rt_heat_generator_front.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/heat/rt_heat_generator_front_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/heat/rt_heat_generator_leftright.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/heat/rt_heat_generator_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/heat/rt_heat_generator_top_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/heat/solid_heat_generator_back.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/heat/solid_heat_generator_back_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/heat/solid_heat_generator_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/heat/solid_heat_generator_front.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/heat/solid_heat_generator_front_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/heat/solid_heat_generator_left.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/heat/solid_heat_generator_left_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/heat/solid_heat_generator_right.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/heat/solid_heat_generator_right_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/heat/solid_heat_generator_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/kinetic/electric_kinetic_generator_back.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/kinetic/electric_kinetic_generator_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/kinetic/electric_kinetic_generator_front.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/kinetic/electric_kinetic_generator_leftright.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/kinetic/electric_kinetic_generator_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/kinetic/manual_kinetic_generator_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/kinetic/manual_kinetic_generator_sides.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/kinetic/manual_kinetic_generator_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/kinetic/steam_kinetic_generator_back.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/kinetic/steam_kinetic_generator_back_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/kinetic/steam_kinetic_generator_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/kinetic/steam_kinetic_generator_front.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/kinetic/steam_kinetic_generator_left_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/kinetic/steam_kinetic_generator_leftright.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/kinetic/steam_kinetic_generator_right_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/kinetic/steam_kinetic_generator_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/kinetic/stirling_kinetic_generator_bottomtop.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/kinetic/stirling_kinetic_generator_bottomtop_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/kinetic/stirling_kinetic_generator_front.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/kinetic/stirling_kinetic_generator_leftrightback.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/kinetic/stirling_kinetic_generator_leftrightback_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/kinetic/water_kinetic_generator_back.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/kinetic/water_kinetic_generator_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/kinetic/water_kinetic_generator_front.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/kinetic/water_kinetic_generator_leftright.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/kinetic/water_kinetic_generator_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/kinetic/wind_kinetic_generator_back.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/kinetic/wind_kinetic_generator_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/kinetic/wind_kinetic_generator_front.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/kinetic/wind_kinetic_generator_leftright.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/kinetic/wind_kinetic_generator_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/reactor/nuclear_reactor_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/reactor/nuclear_reactor_front_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/reactor/nuclear_reactor_leftrightback_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/reactor/nuclear_reactor_sides.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/reactor/nuclear_reactor_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/reactor/reactor_access_hatch.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/reactor/reactor_chamber_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/reactor/reactor_chamber_sides.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/reactor/reactor_chamber_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/reactor/reactor_fluid_port.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/reactor/reactor_redstone_port.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/reactor/reactor_vessel.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/generator/reactor/reactor_vessel_plain.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/iridium_storage_box.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/iron_scaffold_bottomtop.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/iron_scaffold_sides.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/chunk_loader_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/chunk_loader_side.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/chunk_loader_side_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/chunk_loader_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/condenser_back_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/condenser_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/condenser_front_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/condenser_frontback.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/condenser_left_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/condenser_leftright.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/condenser_right_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/condenser_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/condenser_top_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/fluid_bottler_back.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/fluid_bottler_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/fluid_bottler_front.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/fluid_bottler_front_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/fluid_bottler_leftright.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/fluid_bottler_leftright_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/fluid_bottler_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/fluid_distributor_bottomtop.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/fluid_distributor_bottomtop_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/fluid_distributor_front.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/fluid_distributor_front_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/fluid_distributor_leftrightback.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/fluid_distributor_leftrightback_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/fluid_regulator_back.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/fluid_regulator_back_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/fluid_regulator_bottomtop.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/fluid_regulator_front.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/fluid_regulator_front_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/fluid_regulator_left_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/fluid_regulator_leftright.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/fluid_regulator_right_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/liquid_heat_exchanger_back.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/liquid_heat_exchanger_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/liquid_heat_exchanger_front.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/liquid_heat_exchanger_front_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/liquid_heat_exchanger_left_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/liquid_heat_exchanger_leftright.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/liquid_heat_exchanger_right_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/liquid_heat_exchanger_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/pump_back.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/pump_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/pump_front.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/pump_front_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/pump_leftright.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/pump_leftright_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/pump_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/solar_distiller_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/solar_distiller_sides.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/solar_distiller_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/steam_generator_back_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/steam_generator_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/steam_generator_front_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/steam_generator_frontback.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/steam_generator_left_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/steam_generator_leftright.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/steam_generator_right_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/steam_generator_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/weighted_fluid_distributor_in.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/fluid/weighted_fluid_distributor_out.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/misc/industrial_workbench_back.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/misc/industrial_workbench_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/misc/industrial_workbench_front.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/misc/industrial_workbench_left.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/misc/industrial_workbench_right.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/misc/industrial_workbench_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/misc/item_buffer_bottomtop.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/misc/item_buffer_frontback.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/misc/item_buffer_left.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/misc/item_buffer_right.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/misc/luminator.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/misc/luminator_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/misc/magnetizer_bottomtop.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/misc/magnetizer_front.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/misc/magnetizer_leftrightback.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/misc/rci_lzh_back.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/misc/rci_lzh_sides.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/misc/rci_lzh_sides_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/misc/rci_rsh_back.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/misc/rci_rsh_sides.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/misc/rci_rsh_sides_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/misc/sorting_machine_back.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/misc/sorting_machine_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/misc/sorting_machine_front.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/misc/sorting_machine_left.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/misc/sorting_machine_right.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/misc/sorting_machine_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/misc/teleporter_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/misc/teleporter_sides.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/misc/teleporter_sides_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/misc/teleporter_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/misc/teleporter_top_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/misc/terraformer_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/misc/terraformer_sides.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/misc/terraformer_sides_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/misc/terraformer_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/misc/tesla_coil_bottomtop.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/misc/tesla_coil_sides.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/misc/weighted_item_distributor_in.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/misc/weighted_item_distributor_out.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/advanced/blast_furnace_back.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/advanced/blast_furnace_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/advanced/blast_furnace_front.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/advanced/blast_furnace_leftright.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/advanced/blast_furnace_leftright_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/advanced/blast_furnace_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/advanced/blast_furnace_top_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/advanced/block_cutter_back.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/advanced/block_cutter_bottomtop.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/advanced/block_cutter_front.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/advanced/block_cutter_front_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/advanced/block_cutter_leftright.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/advanced/block_cutter_leftright_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/advanced/centrifuge_back.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/advanced/centrifuge_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/advanced/centrifuge_front.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/advanced/centrifuge_front_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/advanced/centrifuge_leftright.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/advanced/centrifuge_leftright_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/advanced/centrifuge_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/advanced/centrifuge_top_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/advanced/fermenter_back.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/advanced/fermenter_back_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/advanced/fermenter_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/advanced/fermenter_front.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/advanced/fermenter_front_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/advanced/fermenter_leftright.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/advanced/fermenter_leftright_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/advanced/fermenter_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/advanced/induction_furnace_back.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/advanced/induction_furnace_bottomtop.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/advanced/induction_furnace_front.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/advanced/induction_furnace_front_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/advanced/induction_furnace_leftright.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/advanced/induction_furnace_leftright_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/advanced/metal_former_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/advanced/metal_former_front.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/advanced/metal_former_front_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/advanced/metal_former_leftrightback.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/advanced/metal_former_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/advanced/metal_former_top_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/advanced/ore_washing_plant_back.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/advanced/ore_washing_plant_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/advanced/ore_washing_plant_front.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/advanced/ore_washing_plant_front_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/advanced/ore_washing_plant_left.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/advanced/ore_washing_plant_left_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/advanced/ore_washing_plant_right.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/advanced/ore_washing_plant_right_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/advanced/ore_washing_plant_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/basic/canner_back.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/basic/canner_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/basic/canner_front.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/basic/canner_front_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/basic/canner_left_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/basic/canner_leftright.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/basic/canner_right_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/basic/canner_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/basic/recycler_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/basic/recycler_front.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/basic/recycler_front_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/basic/recycler_leftrightback.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/basic/recycler_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/basic/recycler_top_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/basic/solid_canner_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/basic/solid_canner_front.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/basic/solid_canner_front_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/basic/solid_canner_leftrightback.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/processing/basic/solid_canner_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/resource/advanced_miner_back_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/resource/advanced_miner_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/resource/advanced_miner_bottom_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/resource/advanced_miner_front_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/resource/advanced_miner_frontback.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/resource/advanced_miner_leftright.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/resource/advanced_miner_leftright_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/resource/advanced_miner_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/resource/crop_harvester_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/resource/crop_harvester_sides.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/resource/crop_harvester_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/resource/cropmatron_back_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/resource/cropmatron_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/resource/cropmatron_front_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/resource/cropmatron_leftright_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/resource/cropmatron_sides.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/resource/cropmatron_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/resource/miner_back.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/resource/miner_back_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/resource/miner_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/resource/miner_bottom_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/resource/miner_front.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/resource/miner_front_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/resource/miner_leftright.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/resource/miner_leftright_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/resource/miner_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/resource/mining_pipe.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/resource/mining_pipe_tip.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/uu/mass_fabricator_back.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/uu/mass_fabricator_back_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/uu/mass_fabricator_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/uu/mass_fabricator_front.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/uu/mass_fabricator_left.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/uu/mass_fabricator_left_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/uu/mass_fabricator_right.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/uu/mass_fabricator_right_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/uu/mass_fabricator_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/uu/matter_generator_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/uu/matter_generator_front.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/uu/matter_generator_front_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/uu/matter_generator_leftrightback.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/uu/matter_generator_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/uu/pattern_storage_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/uu/pattern_storage_frontback.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/uu/pattern_storage_leftright.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/uu/pattern_storage_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/uu/replicator_back.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/uu/replicator_back_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/uu/replicator_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/uu/replicator_front.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/uu/replicator_front_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/uu/replicator_left.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/uu/replicator_left_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/uu/replicator_right.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/uu/replicator_right_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/uu/replicator_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/uu/replicator_top_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/uu/scanner_back.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/uu/scanner_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/uu/scanner_front.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/uu/scanner_leftright.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/uu/scanner_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/uu/uu_assembly_bench_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/uu/uu_assembly_bench_side.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/machine/uu/uu_assembly_bench_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/personal/energy_o_mat_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/personal/energy_o_mat_front.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/personal/energy_o_mat_leftrightback.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/personal/energy_o_mat_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/personal/personal_chest_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/personal/personal_chest_case_bottomtop.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/personal/personal_chest_case_leftrightback.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/personal/personal_chest_front.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/personal/personal_chest_leftrightback.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/personal/personal_chest_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/personal/trade_o_mat_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/personal/trade_o_mat_front.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/personal/trade_o_mat_leftrightback.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/personal/trade_o_mat_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/personal/trading_terminal_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/personal/trading_terminal_front.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/personal/trading_terminal_leftrightback.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/personal/trading_terminal_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/personal/wireless_trade_o_mat_side.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/raw_lead_block.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/reinforced_door_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/reinforced_door_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/reinforced_glass.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/reinforced_iron_scaffold_sides.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/reinforced_wood_scaffold_sides.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/resin_sheet.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/resource/advanced_machine.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/resource/basalt.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/resource/bronze_block.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/resource/copper_block.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/resource/copper_ore.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/resource/lead_block.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/resource/ore/deepslate_lead_ore.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/resource/ore/deepslate_uranium_ore.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/resource/ore/lead_ore.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/resource/ore/raw_lead_block.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/resource/ore/raw_tin_block.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/resource/ore/raw_uranium_block.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/resource/ore/uranium_ore.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/resource/reinforced_stone.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/resource/rubber/door_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/resource/rubber/door_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/resource/rubber/planks.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/resource/rubber/trapdoor.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/resource/silver_block.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/resource/steel_block.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/resource/stripped_rubber_log.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/resource/stripped_rubber_log_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/resource/tin_block.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/resource/uranium_block_bottomtop.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/resource/uranium_block_sides.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/rubber_sheet.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/steam/coke_kiln.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/steam/coke_kiln_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/steam/coke_kiln_grate.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/steam/coke_kiln_hatch.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/steam/refractory_bricks.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/storage_box.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/storage_box_old.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/transport/pipe_large_old.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/transport/pipe_medium_old.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/transport/pipe_side_old.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/transport/pipe_small_old.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/transport/pipe_tiny_old.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/glass_cable_0_black.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/glass_cable_0_blue.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/glass_cable_0_brown.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/glass_cable_0_cyan.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/glass_cable_0_gray.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/glass_cable_0_green.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/glass_cable_0_light_blue.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/glass_cable_0_light_gray.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/glass_cable_0_lime.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/glass_cable_0_magenta.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/glass_cable_0_orange.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/glass_cable_0_pink.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/glass_cable_0_purple.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/glass_cable_0_red.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/glass_cable_0_yellow.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/gold_cable_1_blue.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/gold_cable_1_brown.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/gold_cable_1_cyan.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/gold_cable_1_gray.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/gold_cable_1_green.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/gold_cable_1_light_blue.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/gold_cable_1_light_gray.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/gold_cable_1_lime.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/gold_cable_1_magenta.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/gold_cable_1_orange.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/gold_cable_1_pink.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/gold_cable_1_purple.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/gold_cable_1_red.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/gold_cable_1_white.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/gold_cable_1_yellow.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/gold_cable_2_blue.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/gold_cable_2_brown.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/gold_cable_2_cyan.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/gold_cable_2_gray.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/gold_cable_2_green.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/gold_cable_2_light_blue.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/gold_cable_2_light_gray.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/gold_cable_2_lime.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/gold_cable_2_magenta.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/gold_cable_2_orange.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/gold_cable_2_pink.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/gold_cable_2_purple.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/gold_cable_2_red.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/gold_cable_2_white.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/gold_cable_2_yellow.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/iron_cable_1_blue.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/iron_cable_1_brown.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/iron_cable_1_cyan.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/iron_cable_1_gray.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/iron_cable_1_green.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/iron_cable_1_light_blue.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/iron_cable_1_light_gray.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/iron_cable_1_lime.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/iron_cable_1_magenta.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/iron_cable_1_orange.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/iron_cable_1_pink.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/iron_cable_1_purple.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/iron_cable_1_red.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/iron_cable_1_white.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/iron_cable_1_yellow.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/iron_cable_2_blue.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/iron_cable_2_brown.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/iron_cable_2_cyan.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/iron_cable_2_gray.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/iron_cable_2_green.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/iron_cable_2_light_blue.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/iron_cable_2_light_gray.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/iron_cable_2_lime.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/iron_cable_2_magenta.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/iron_cable_2_orange.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/iron_cable_2_pink.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/iron_cable_2_purple.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/iron_cable_2_red.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/iron_cable_2_white.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/iron_cable_2_yellow.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/iron_cable_3_blue.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/iron_cable_3_brown.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/iron_cable_3_cyan.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/iron_cable_3_gray.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/iron_cable_3_green.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/iron_cable_3_light_blue.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/iron_cable_3_light_gray.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/iron_cable_3_lime.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/iron_cable_3_magenta.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/iron_cable_3_orange.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/iron_cable_3_pink.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/iron_cable_3_purple.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/iron_cable_3_red.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/iron_cable_3_white.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/cable/iron_cable_3_yellow.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/chargepad/chargepad_batbox_back.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/chargepad/chargepad_batbox_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/chargepad/chargepad_batbox_front.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/chargepad/chargepad_batbox_leftright.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/chargepad/chargepad_batbox_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/chargepad/chargepad_batbox_top_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/chargepad/chargepad_cesu_back.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/chargepad/chargepad_cesu_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/chargepad/chargepad_cesu_front.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/chargepad/chargepad_cesu_leftright.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/chargepad/chargepad_cesu_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/chargepad/chargepad_cesu_top_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/chargepad/chargepad_mfe_back.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/chargepad/chargepad_mfe_back_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/chargepad/chargepad_mfe_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/chargepad/chargepad_mfe_front.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/chargepad/chargepad_mfe_leftright.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/chargepad/chargepad_mfe_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/chargepad/chargepad_mfe_top_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/chargepad/chargepad_mfsu_back_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/chargepad/chargepad_mfsu_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/chargepad/chargepad_mfsu_front.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/chargepad/chargepad_mfsu_front_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/chargepad/chargepad_mfsu_left_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/chargepad/chargepad_mfsu_leftrightback.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/chargepad/chargepad_mfsu_right_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/chargepad/chargepad_mfsu_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/chargepad/chargepad_mfsu_top_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/storage/electrolyzer_bottom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/storage/electrolyzer_leftrightback_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/storage/electrolyzer_sides.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/storage/electrolyzer_sides_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/storage/electrolyzer_top.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/transformer/ev_transformer_bottomtop.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/transformer/ev_transformer_bottomtop_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/transformer/ev_transformer_front.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/transformer/ev_transformer_front_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/transformer/ev_transformer_leftrightback.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/transformer/ev_transformer_leftrightback_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/transformer/hv_transformer_bottomtop.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/transformer/hv_transformer_bottomtop_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/transformer/hv_transformer_front.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/transformer/hv_transformer_front_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/transformer/hv_transformer_leftrightback.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/transformer/hv_transformer_leftrightback_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/transformer/lv_transformer_front.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/transformer/lv_transformer_front_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/transformer/lv_transformer_side.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/transformer/lv_transformer_side_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/transformer/mv_transformer_bottomtop.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/transformer/mv_transformer_bottomtop_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/transformer/mv_transformer_front.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/transformer/mv_transformer_front_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/transformer/mv_transformer_leftrightback.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wiring/transformer/mv_transformer_leftrightback_active.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wood_scaffold_bottomtop.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/blocks/wood_scaffold_sides.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/entity/boat/boat_carbon.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/entity/boat/boat_electric.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/entity/boat/boat_rubber.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/common.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/gui_canner_classic.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/gui_cropmatron_classic.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/gui_trading_terminal.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guiadvminer.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guibatchcrafter.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guiblastfurnace.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guiblastfurnacenew.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guiblockcutter.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guibottler.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guicanner.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guichargepadblock.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guichunkloader.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guicondenser.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guicontainmentbox.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guicropharvester.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guicropmatron.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guicropmatronold.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guicropnalyzer.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guielectricblock.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guielectricheatgenerator.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guielectrickineticgenerator.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guielectrolyzer.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guienergyomatclosed.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guienergyomatopen.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guifermenter.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guifluiddistributor.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guifluidgenerator.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guifluidheatgenerator.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guifluidregulator.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guiheatsourcefluid.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guiindustrialworkbench.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guiitembuffer.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guikineticgenerator.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guilathe.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guimagnetizer.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guimatter.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guimetalformer.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guiminer.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guinuclearreactor.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guinuclearreactorfluid.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guinuke.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guiorewashingplant.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guioverlay.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guipatternstorage.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guireactorfluidport.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guireplicator.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guirtheatgenerator.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guiscanner.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guisolardestiller.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guisolidcanner.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guisolidheatgenerator.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guisortingmachine.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guisteamgenerator.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guisteamkineticgenerator.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guisteamrepressurizer.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guistirlinggenerator.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guistirlingkineticgenerator.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guithermalcentrifuge.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guitoolbox.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guitooleumeter.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guitoolscanner.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guitradeomatclosed.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guitradeomatopen.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guitransfomer.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guiwaterkineticgenerator.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guiweightedfluiddistributor.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guiweighteditemdistributor.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/guiwindkineticgenerator.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/infobutton.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/overlay/blast_furnace.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/overlay/canner_arrow.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/overlay/induction_furnace_input.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/overlay/induction_furnace_output.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/overlay/pump_arrow.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/overlay/solar_sun.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/gui/scrapboxrecipes.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/36.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/armor/advanced_batpack.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/armor/alloy_chestplate.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/armor/batpack.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/armor/cf_pack.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/armor/energy_pack.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/armor/hazmat_chestplate.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/armor/hazmat_helmet.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/armor/hazmat_leggings.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/armor/jetpack.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/armor/jetpack_electric.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/armor/lappack.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/armor/nano_boots.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/armor/nano_chestplate.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/armor/nano_helmet.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/armor/nano_leggings.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/armor/nightvision_goggles.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/armor/quantum_boots.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/armor/quantum_boots_overlay.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/armor/quantum_chestplate.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/armor/quantum_chestplate_overlay.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/armor/quantum_helmet.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/armor/quantum_helmet_overlay.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/armor/quantum_leggings.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/armor/quantum_leggings_overlay.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/armor/rubber_boots.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/armor/solar_helmet.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/armor/static_boots.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/battery/advanced_charging_re_battery_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/battery/advanced_charging_re_battery_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/battery/advanced_charging_re_battery_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/battery/advanced_charging_re_battery_3.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/battery/advanced_charging_re_battery_4.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/battery/advanced_re_battery_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/battery/advanced_re_battery_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/battery/advanced_re_battery_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/battery/advanced_re_battery_3.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/battery/advanced_re_battery_4.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/battery/charging_energy_crystal_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/battery/charging_energy_crystal_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/battery/charging_energy_crystal_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/battery/charging_energy_crystal_3.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/battery/charging_energy_crystal_4.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/battery/charging_lapotron_crystal_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/battery/charging_lapotron_crystal_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/battery/charging_lapotron_crystal_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/battery/charging_lapotron_crystal_3.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/battery/charging_lapotron_crystal_4.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/battery/charging_re_battery_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/battery/charging_re_battery_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/battery/charging_re_battery_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/battery/charging_re_battery_3.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/battery/charging_re_battery_4.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/battery/energy_crystal_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/battery/energy_crystal_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/battery/energy_crystal_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/battery/energy_crystal_3.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/battery/energy_crystal_4.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/battery/lapotron_crystal_0.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/battery/lapotron_crystal_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/battery/lapotron_crystal_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/battery/lapotron_crystal_3.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/battery/lapotron_crystal_4.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/battery/single_use_battery.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/boat/broken_rubber_boat.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/boat/carbon_boat.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/boat/electric_boat.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/boat/rubber_boat.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/brewing/201.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/brewing/barrel.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/brewing/booze_mug_beer_ale.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/brewing/booze_mug_beer_beer.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/brewing/booze_mug_beer_black_stuff.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/brewing/booze_mug_beer_brew.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/brewing/booze_mug_beer_dragon_blood.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/brewing/booze_mug_beer_youngster.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/brewing/booze_mug_rum.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/brewing/mug_coffee.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/brewing/mug_cold_coffee.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/brewing/mug_dark_coffee.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/brewing/mug_empty.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/bucket/biomass.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/bucket/construction_foam.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/bucket/coolant.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/bucket/creosote.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/bucket/heavy_water.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/bucket/hot_coolant.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/bucket/hot_water.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/bucket/pahoehoe_lava.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/bucket/uu_matter.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/bucket/weed_ex.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/cell/air_cell.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/cell/bio_cell.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/cell/biogas_cell.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/cell/biomass_cell.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/cell/cf_cell.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/cell/coal_cell.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/cell/coolant_cell.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/cell/distilled_water_cell.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/cell/electrolyzed_water_cell.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/cell/fluid_cell.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/cell/fluid_cell_window.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/cell/hot_coolant_cell.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/cell/lava_cell.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/cell/pahoehoelava_cell.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/cell/ref_bio_cell.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/cell/ref_coal_cell.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/cell/steam_cell.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/cell/superheated_steam_cell.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/cell/uu_matter_cell.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/cell/water_cell.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/crafting/bronze_rotor_blade.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/crafting/bronze_shaft.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/crafting/carbon_rotor_blade.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/crafting/crystal_memory.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/crafting/diamond_block_cutting_blade.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/crafting/fuel_rod.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/crafting/iron_block_cutting_blade.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/crafting/iron_rotor_blade.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/crafting/iron_shaft.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/crafting/jetpack_attachment_plate.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/crafting/mfsu_upgrade_kit.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/crafting/raw_crystal_memory.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/crafting/scrap_box.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/crafting/steam_turbine.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/crafting/steam_turbine_blade.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/crafting/steel_block_cutting_blade.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/crafting/steel_rotor_blade.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/crafting/steel_shaft.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/crafting/tin_can.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/crafting/wood_rotor_blade.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/crop/bobs_yer_uncle_ranks_berry.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/crop/coffee_beans.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/crop/coffee_powder.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/crop/crop_invalid.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/crop/crop_seed_bag.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/crop/crop_stick.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/crop/crop_unknown.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/crop/cropnalyzer.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/crop/fertilizer.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/crop/grin_powder.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/crop/hops.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/crop/hydration_cell.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/crop/milk_wart.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/crop/oil_berry.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/crop/terra_wart.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/crop/weed.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/crop/weed_ex.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/crop/weeding_trowel.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/dcp.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/dynamite.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/dynamite_sticky.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/filled_tin_can.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/fuel_can.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/fuel_can_empty.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/iodine_tablet.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/reactor/advanced_heat_exchanger.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/reactor/advanced_heat_vent.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/reactor/component_heat_exchanger.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/reactor/component_heat_vent.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/reactor/containment_plating.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/reactor/fuel_rod/dual_mox.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/reactor/fuel_rod/dual_uranium.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/reactor/fuel_rod/isotope.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/reactor/fuel_rod/lithium.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/reactor/fuel_rod/mox.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/reactor/fuel_rod/quad_mox.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/reactor/fuel_rod/quad_uranium.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/reactor/fuel_rod/tritium.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/reactor/fuel_rod/uranium.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/reactor/heat_exchanger.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/reactor/heat_plating.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/reactor/heat_storage.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/reactor/heat_vent.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/reactor/heatpack.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/reactor/hex_heat_storage.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/reactor/iridium_reflector.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/reactor/lzh_condensator.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/reactor/neutron_reflector.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/reactor/overclocked_heat_vent.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/reactor/plating.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/reactor/reactor_heat_exchanger.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/reactor/reactor_heat_vent.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/reactor/rsh_condensator.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/reactor/thick_neutron_reflector.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/reactor/tri_heat_storage.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/reactor/uran_empty_cell.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/reactor/uran_enriched_cell.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/reinforced_door.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/resource/iodine.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/resource/nuclear/depleted_dual_mox.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/resource/nuclear/depleted_dual_uranium.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/resource/nuclear/depleted_mox.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/resource/nuclear/depleted_quad_mox.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/resource/nuclear/depleted_quad_uranium.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/resource/nuclear/depleted_uranium.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/resource/nuclear/rtg_pellet.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/resource/uu_matter.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/rotor/bronze_rotor.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/rotor/bronze_rotor_model.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/rotor/carbon_rotor.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/rotor/carbon_rotor_model.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/rotor/iron_rotor.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/rotor/iron_rotor_model.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/rotor/steel_rotor.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/rotor/steel_rotor_model.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/rotor/wood_rotor.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/rotor/wood_rotor_model.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/rubber_door.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/rubber_sign.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tfbp/blank.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tfbp/chilling.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tfbp/cultivation.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tfbp/desertification.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tfbp/flatification.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tfbp/irrigation.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tfbp/mushroom.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/attachment_remover.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/attachment_remover_bronze.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/bronze_axe.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/bronze_hoe.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/bronze_shovel.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/bronze_sword.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/containment_box.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/crowbar_body.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/crowbar_body_bronze.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/crowbar_bronze.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/crowbar_handle.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/debug_item.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/electric/advanced_scanner.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/electric/chainsaw.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/electric/diamond_drill.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/electric/drill.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/electric/electric_hoe.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/electric/electric_treetap.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/electric/electric_wrench.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/electric/iridium_drill.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/electric/mining_laser.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/electric/nano_saber.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/electric/nano_saber/active_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/electric/nano_saber/active_10.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/electric/nano_saber/active_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/electric/nano_saber/active_3.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/electric/nano_saber/active_4.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/electric/nano_saber/active_5.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/electric/nano_saber/active_6.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/electric/nano_saber/active_7.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/electric/nano_saber/active_8.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/electric/nano_saber/active_9.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/electric/nano_saber/inactive.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/electric/obscurator.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/electric/obscurator_mask.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/electric/plasma_launcher.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/electric/plasma_launcher.png.mcmeta` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 重绘帧序列的播放参数，按游戏资源元数据核对 | 2026-11-20 |
| `assets/ic2/textures/items/tool/electric/scanner.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/electric/wind_meter.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/foam_sprayer.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/frequency_transmitter.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/meter.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/painter/painter.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/painter/painter_black.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/painter/painter_blue.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/painter/painter_brown.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/painter/painter_cyan.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/painter/painter_gray.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/painter/painter_green.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/painter/painter_light_blue.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/painter/painter_light_gray.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/painter/painter_lime.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/painter/painter_magenta.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/painter/painter_orange.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/painter/painter_pink.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/painter/painter_purple.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/painter/painter_red.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/painter/painter_white.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/painter/painter_yellow.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/remote.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/tool_box.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/tool/tool_box_open.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/advanced_ejector.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/advanced_ejector_down.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/advanced_ejector_east.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/advanced_ejector_north.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/advanced_ejector_south.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/advanced_ejector_up.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/advanced_ejector_west.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/advanced_fluid_ejector.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/advanced_fluid_ejector_down.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/advanced_fluid_ejector_east.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/advanced_fluid_ejector_north.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/advanced_fluid_ejector_south.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/advanced_fluid_ejector_up.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/advanced_fluid_ejector_west.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/advanced_fluid_pulling.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/advanced_fluid_pulling_down.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/advanced_fluid_pulling_east.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/advanced_fluid_pulling_north.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/advanced_fluid_pulling_south.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/advanced_fluid_pulling_up.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/advanced_fluid_pulling_west.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/advanced_pulling.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/advanced_pulling_down.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/advanced_pulling_east.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/advanced_pulling_north.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/advanced_pulling_south.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/advanced_pulling_up.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/advanced_pulling_west.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/ejector.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/ejector_down.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/ejector_east.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/ejector_north.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/ejector_south.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/ejector_up.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/ejector_west.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/energy_storage.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/fluid_ejector.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/fluid_ejector_down.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/fluid_ejector_east.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/fluid_ejector_north.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/fluid_ejector_south.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/fluid_ejector_up.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/fluid_ejector_west.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/fluid_pulling.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/fluid_pulling_down.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/fluid_pulling_east.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/fluid_pulling_north.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/fluid_pulling_south.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/fluid_pulling_up.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/fluid_pulling_west.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/overclocker.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/pulling.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/pulling_down.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/pulling_east.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/pulling_north.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/pulling_south.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/pulling_up.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/pulling_west.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/redstone_inverter.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/remote_interface.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/upgrade/transformer.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/mob_effect/radiation.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/models/armor/advanced_batpack_layer_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/models/armor/batpack_layer_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/models/armor/energy_pack_layer_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/models/armor/solar_helmet_layer_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/models/armor/static_boots_layer_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/models/beam.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/models/laser.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/models/newsafe.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户回交纹理.zip，同名 PNG | 2026-11-20 |
| `assets/ic2/textures/items/bucket_contents/air.png` | new | IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户流体纹理桶口裁切与着色 | 2026-11-20 |
| `assets/ic2/textures/items/bucket_contents/biogas.png` | new | IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户流体纹理桶口裁切与着色 | 2026-11-20 |
| `assets/ic2/textures/items/bucket_contents/distilled_water.png` | new | IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户流体纹理桶口裁切与着色 | 2026-11-20 |
| `assets/ic2/textures/items/bucket_contents/hydrogen.png` | new | IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户流体纹理桶口裁切与着色 | 2026-11-20 |
| `assets/ic2/textures/items/bucket_contents/oxygen.png` | new | IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户流体纹理桶口裁切与着色 | 2026-11-20 |
| `assets/ic2/textures/items/bucket_contents/steam.png` | new | IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户流体纹理桶口裁切与着色 | 2026-11-20 |
| `assets/ic2/textures/items/bucket_contents/superheated_steam.png` | new | IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 用户流体纹理桶口裁切与着色 | 2026-11-20 |
| `assets/ic2/sounds.json` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/block/nuke/explode.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/generator/generator/loop.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/generator/geothermal/loop.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/generator/nuclear/loop.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/generator/nuclear/power/high.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/generator/nuclear/power/low.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/generator/nuclear/power/medium.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/generator/water/loop.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/generator/wind/loop.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/item/battery/use.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/item/chainsaw/idle.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/item/chainsaw/stop.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/item/chainsaw/use1.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/item/chainsaw/use2.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/item/crowbar/use.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/item/cutter/use.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/item/drill/hard.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/item/drill/idle.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/item/drill/soft.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/item/electric/shutdown.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/item/laser/explosive.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/item/laser/long_range.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/item/laser/low_focus.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/item/laser/scatter.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/item/laser/shoot.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/item/nanosaber/idle.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/item/nanosaber/power_up.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/item/nanosaber/swing1.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/item/nanosaber/swing2.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/item/nanosaber/swing3.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/item/painter/use.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/item/scanner/use.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/item/treetap/electric/use.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/item/treetap/use.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/item/wrench/use.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/machine/canner/operate.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/machine/canner/reverse.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/machine/compressor/operate.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/machine/electrolyzer/loop.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/machine/extractor/operate.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/machine/fabricator/loop.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/machine/fabricator/scrap.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/machine/furnace/electric/loop.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/machine/furnace/electric/start.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/machine/furnace/electric/stop.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/machine/furnace/induction/loop.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/machine/furnace/induction/start.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/machine/furnace/induction/stop.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/machine/furnace/iron/operate.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/machine/interrupt1.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/machine/macerator/operate.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/machine/miner/operate.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/machine/o_mat/operate.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/machine/overload.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/machine/pump/operate.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/machine/recycler/operate.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/machine/teleporter/charge.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/machine/teleporter/use.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/sounds/machine/terraformer/loop.ogg` | new | IC2 重建项目 | CC BY-SA 4.0 | 独立工业音效合成，音源脚本 synthesize_audio.py | 2026-11-20 |
| `assets/ic2/textures/entity/equipment/humanoid/alloy.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 回交纹理.zip 内 `assets/minecraft/textures/models/armor/ic2_alloy_layer_1.png`，原样适配本版装备路径 | 2026-11-20 |
| `assets/ic2/textures/entity/equipment/humanoid/hazmat.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 回交纹理.zip 内 `assets/minecraft/textures/models/armor/ic2_hazmat_layer_1.png`，原样适配本版装备路径 | 2026-11-20 |
| `assets/ic2/textures/entity/equipment/humanoid/nano.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 回交纹理.zip 内 `assets/minecraft/textures/models/armor/ic2_nano_layer_1.png`，原样适配本版装备路径 | 2026-11-20 |
| `assets/ic2/textures/entity/equipment/humanoid/night_vision.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 回交纹理.zip 内 `assets/minecraft/textures/models/armor/ic2_night_vision_layer_1.png`，原样适配本版装备路径 | 2026-11-20 |
| `assets/ic2/textures/entity/equipment/humanoid/quantum.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 回交纹理.zip 内 `assets/minecraft/textures/models/armor/ic2_quantum_layer_1.png`，原样适配本版装备路径 | 2026-11-20 |
| `assets/ic2/textures/entity/equipment/humanoid/quantum_overlay.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 回交纹理.zip 内 `assets/minecraft/textures/models/armor/quantum_1_overlay.png`，原样适配本版装备路径 | 2026-11-20 |
| `assets/ic2/textures/entity/equipment/humanoid/solar_helmet.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 回交纹理.zip 内 `assets/minecraft/textures/models/armor/solar_1.png`，原样适配本版装备路径 | 2026-11-20 |
| `assets/ic2/textures/entity/equipment/humanoid/static_boots.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 回交纹理.zip 内 `assets/ic2/textures/models/armor/static_boots_layer_1.png`，原样适配本版装备路径 | 2026-11-20 |
| `assets/ic2/textures/entity/equipment/humanoid_leggings/hazmat.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 回交纹理.zip 内 `assets/minecraft/textures/models/armor/ic2_hazmat_layer_2.png`，原样适配本版装备路径 | 2026-11-20 |
| `assets/ic2/textures/entity/equipment/humanoid_leggings/nano.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 回交纹理.zip 内 `assets/minecraft/textures/models/armor/ic2_nano_layer_2.png`，原样适配本版装备路径 | 2026-11-20 |
| `assets/ic2/textures/entity/equipment/humanoid_leggings/quantum.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 回交纹理.zip 内 `assets/minecraft/textures/models/armor/ic2_quantum_layer_2.png`，原样适配本版装备路径 | 2026-11-20 |
| `assets/ic2/textures/entity/equipment/humanoid_leggings/quantum_overlay.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 回交纹理.zip 内 `assets/minecraft/textures/models/armor/quantum_2_overlay.png`，原样适配本版装备路径 | 2026-11-20 |
| `assets/ic2/textures/models/armor/alloy_layer_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 回交纹理.zip 内 `assets/minecraft/textures/models/armor/ic2_alloy_layer_1.png`，原样适配本版装备路径 | 2026-11-20 |
| `assets/ic2/textures/models/armor/hazmat_layer_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 回交纹理.zip 内 `assets/minecraft/textures/models/armor/ic2_hazmat_layer_1.png`，原样适配本版装备路径 | 2026-11-20 |
| `assets/ic2/textures/models/armor/hazmat_layer_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 回交纹理.zip 内 `assets/minecraft/textures/models/armor/ic2_hazmat_layer_2.png`，原样适配本版装备路径 | 2026-11-20 |
| `assets/ic2/textures/models/armor/nano_layer_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 回交纹理.zip 内 `assets/minecraft/textures/models/armor/ic2_nano_layer_1.png`，原样适配本版装备路径 | 2026-11-20 |
| `assets/ic2/textures/models/armor/nano_layer_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 回交纹理.zip 内 `assets/minecraft/textures/models/armor/ic2_nano_layer_2.png`，原样适配本版装备路径 | 2026-11-20 |
| `assets/ic2/textures/models/armor/night_vision_layer_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 回交纹理.zip 内 `assets/minecraft/textures/models/armor/ic2_night_vision_layer_1.png`，原样适配本版装备路径 | 2026-11-20 |
| `assets/ic2/textures/models/armor/quantum_layer_1.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 回交纹理.zip 内 `assets/minecraft/textures/models/armor/ic2_quantum_layer_1.png`，原样适配本版装备路径 | 2026-11-20 |
| `assets/ic2/textures/models/armor/quantum_layer_1_overlay.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 回交纹理.zip 内 `assets/minecraft/textures/models/armor/quantum_1_overlay.png`，原样适配本版装备路径 | 2026-11-20 |
| `assets/ic2/textures/models/armor/quantum_layer_2.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 回交纹理.zip 内 `assets/minecraft/textures/models/armor/ic2_quantum_layer_2.png`，原样适配本版装备路径 | 2026-11-20 |
| `assets/ic2/textures/models/armor/quantum_layer_2_overlay.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 回交纹理.zip 内 `assets/minecraft/textures/models/armor/quantum_2_overlay.png`，原样适配本版装备路径 | 2026-11-20 |
| `assets/ic2/equipment/alloy.json` | new | IC2 重建项目 | CC BY-SA 4.0 | 本项目编写的原生装备图层索引 | 2026-11-20 |
| `assets/ic2/equipment/hazmat.json` | new | IC2 重建项目 | CC BY-SA 4.0 | 本项目编写的原生装备图层索引 | 2026-11-20 |
| `assets/ic2/equipment/nano.json` | new | IC2 重建项目 | CC BY-SA 4.0 | 本项目编写的原生装备图层索引 | 2026-11-20 |
| `assets/ic2/equipment/night_vision.json` | new | IC2 重建项目 | CC BY-SA 4.0 | 本项目编写的原生装备图层索引 | 2026-11-20 |
| `assets/ic2/equipment/quantum.json` | new | IC2 重建项目 | CC BY-SA 4.0 | 本项目编写的原生装备图层索引 | 2026-11-20 |
| `assets/ic2/equipment/solar_helmet.json` | new | IC2 重建项目 | CC BY-SA 4.0 | 本项目编写的原生装备图层索引 | 2026-11-20 |
| `assets/ic2/equipment/static_boots.json` | new | IC2 重建项目 | CC BY-SA 4.0 | 本项目编写的原生装备图层索引 | 2026-11-20 |
| `assets/ic2/textures/entity/signs/rubber.png` | new | 用户提供的 IC2 重绘项目 | CC BY-SA 4.0（项目登记） | 回交纹理.zip 内 `assets/minecraft/textures/entity/signs/rubber.png`，原样适配橡胶告示牌 | 2026-11-20 |
