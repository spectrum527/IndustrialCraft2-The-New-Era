# IC2 美术工作区

在 `rebuild/assets/` 内按包内路径作画。例如打粉机的正面存为 `ic2/textures/blocks/machine/processing/basic/macerator_front.png`，成品中的路径会多一层 `assets/`。项目自有的路径保留 `ic2` 及原相对路径。参考包给其他命名空间提供的装备层，由 Client 核对目标版装备模型后登记最终路径。

| 新作目录（相对本目录） | 内容 |
|---|---|
| `ic2/textures/blocks/{machine,generator,general}/` | 机器工作面、发电机、通用壳体及方向面 |
| `ic2/textures/blocks/wiring/` | 电缆世界模型、储电箱、充电台、变压器 |
| `ic2/textures/items/` | 物品图标、线缆手持图、装备图标及各档状态 |
| `<namespace>/textures/…` | 其他被模型使用的表面，包括装备穿戴层、实体贴图和共享材质 |

1. 按[纹理规格](../spec/assets/textures.md)领取一批路径，使用空白画布绘制。同一资源供多个模型使用时只交付一次。
2. 每个成品路径在 [SOURCES.md](SOURCES.md) 登记，含作者、原创来源、许可与日期。工作草图及私有参考保存在打包目录以外。
3. 游戏美术采用 CC BY-SA 4.0。标识见[品牌条款](../docs/branding.md)。原版图像留在私有参考库。旧 `assets/ia2/`、`data/ia2/` 资源及外部目录中的 PNG 和动画元数据由构建过滤，包括机器 GUI 及穿戴层。原图在参考实例中预览。两版生产验收都使用无外部目录的构建。
4. 基础切片已导入用户回交的新绘纹理，并补绘独立橡胶树叶；[对象与纹理目录](CATALOG.md)区分参考注册对象和本轮目标。[逐文件来源与哈希](PRODUCTION-TEXTURES.csv)和作者许可见[来源登记](SOURCES.md)。客户端和实际重载验收另行记录，不能仅凭文件存在判定通过。
