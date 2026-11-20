# 基础切片资源生成

`generate_resources.py` 为四个目标生成适配对应数据包格式的模型、状态、物品定义、配方、战利品和世界生成 JSON。静态绘图和语言分别存于仓库根目录 `assets/ic2/textures/` 与 `assets/ic2/lang/`，来源见 `assets/SOURCES.md`。脚本不读取私有参考或重绘工作目录。配方布局依据本项目的 39 项行为规格；锤子和剪线钳合成后的保留及耐久损耗由对应平台物品实现。

NeoForge 的 `processResources` 自动运行生成器，输出到各模块的 `build/generated/ic2-resources/`。需要 Python 3.9 及以上，可用 `-Pic2.python=/path/to/python3` 指定解释器。Forge 目标也可单独生成：

```sh
python3 src/datagen/generate_resources.py --target forge-1.19.2 --output build/generated/ic2-resources
```

`--output` 指向可清空的生成目录；不要指向已有源文件。生成结果以 `assets/`、`data/` 为顶层，直接合并进资源包。1.19.2 使用复数 `recipes`、`loot_tables`、`tags/blocks`；1.21.1 使用单数目录和旧式物品模型；26.x 还生成 `assets/ic2/items/` 与装备定义，并使用 26.x 配方及树配置格式。修改脚本后应分别运行四版资源检查并在目标游戏中验证加载、配方、世界生成、放置及重载。

扩展储电及电缆可用 `--extended-energy` 启用；运输管、泵盖、储罐与对应材料使用 `--logistics`，并自动包含扩展能量和材料批次。NeoForge 生产构建默认启用该批，使用 `-Pic2.logistics=false` 可生成基础切片作诊断；可用 `-Pic2.extendedEnergy=true` 在诊断中单独加入扩展能量。四种管径及两种材质按素材台账的几何和着色生成。前三种储罐、扳手、撬棍和空单元采用独立游戏配方簿的观测布局。钢锭、钢板和钢外壳的生产链及铱罐所需铱尚待完成；没有用猜测性的替代配方补足。

被动材料可单独用 `--materials`（`-Pic2.materials=true`）启用，物流批次隐含该选项。113 个模型的图片和双语名称来自 `material-assets.tsv`；`observed-material-recipes.json` 只保留参考配方簿中输入输出明确、且当前目标已规划注册的工作台及熔炉配方。熔炉时间和经验来自 `spec/registry/repair-smelting.tsv`，而非按名称推测。其他未知对象、候选物品和多产物机器配方未据此注册。
