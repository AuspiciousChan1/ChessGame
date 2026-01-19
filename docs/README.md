# CircularLoadingView - 圆环进度加载组件

## 项目概述

CircularLoadingView 是一个功能完整的 Android 自定义 View 组件，用于显示圆环形式的加载进度。

## 快速开始

### 在代码中使用

```kotlin
// 创建视图
val loadingView = CircularLoadingView(context)

// 设置进度 (0.0 到 1.0)
loadingView.setProgress(0.5f)  // 50%

// 设置分段数
loadingView.setSegments(8)
```

### 在XML中使用

```xml
<com.chenjili.chessgame.pages.chess.ui.CircularLoadingView
    android:id="@+id/loading_view"
    android:layout_width="300dp"
    android:layout_height="300dp" />
```

## 核心特性

### 1. 圆环分段显示
- 圆环被分为 n 个相等的分段
- 分段数可配置（默认 8 段）
- 每个分段独立显示颜色状态

### 2. 进度指示箭头
- 箭头从圆心出发指向圆环
- 起始方向：向上（12点钟方向）
- 旋转方向：顺时针
- 旋转一圈 = 100% 进度

### 3. 智能颜色高亮
- 进度超过 1/n 时，对应分段变为亮色
- 未达到的分段显示暗色
- 支持自动判断和切换

### 4. 多色配置系统
- 支持配置多组暗色/亮色对
- 分段循环使用颜色对
- 预设3组默认配色

## 可视化示例

```
         12点方向 (起点)
              ↑
         [分段 0]
            ● ●         ● = 亮色 (已完成)
        ●       ●       ○ = 暗色 (未完成)
   [S7] ●         ● [S1]
         ●   ⭘   ●     ⭘ = 圆心
  [S6] ○           ● [S2]
         ○   →   ○     → = 箭头
        ○       ○      
   [S5]   ○ ○   [S3]
         [分段 4]

进度: 50% (4/8 分段完成)
箭头: 指向 180° (6点方向)
```

## API 参考

### 主要方法

#### setProgress(value: Float)
设置当前进度
- **参数**: 0.0 到 1.0 的浮点数
- **示例**: `loadingView.setProgress(0.75f)` // 75%

#### setSegments(count: Int)
设置分段数量
- **参数**: 正整数（建议 3-20）
- **示例**: `loadingView.setSegments(12)` // 12分段

#### setSingleColorPair(darkColor: Int, lightColor: Int)
设置单一颜色方案
- **参数**: 两个颜色值（ARGB格式）
- **示例**:
```kotlin
loadingView.setSingleColorPair(
    darkColor = Color.LTGRAY,
    lightColor = Color.BLUE
)
```

#### setColorPairs(pairs: List<ColorPair>)
设置多组颜色方案
- **参数**: ColorPair 对象列表
- **示例**:
```kotlin
loadingView.setColorPairs(listOf(
    CircularLoadingView.ColorPair(Color.GRAY, Color.GREEN),
    CircularLoadingView.ColorPair(Color.LTGRAY, Color.BLUE)
))
```

#### addColorPair(darkColor: Int, lightColor: Int)
添加颜色对
- **参数**: 两个颜色值
- **作用**: 不清除现有颜色，只添加新的

## 使用场景

### 文件下载进度
```kotlin
val loadingView = CircularLoadingView(context)
loadingView.setSegments(10)

downloadManager.onProgress { bytesDownloaded, totalBytes ->
    val progress = bytesDownloaded.toFloat() / totalBytes
    loadingView.setProgress(progress)
}
```

### 任务完成跟踪
```kotlin
val tasks = listOf("Task1", "Task2", "Task3", "Task4", "Task5")
loadingView.setSegments(tasks.size)

fun onTaskComplete(taskIndex: Int) {
    val progress = (taskIndex + 1).toFloat() / tasks.size
    loadingView.setProgress(progress)
}
```

### 游戏关卡进度
```kotlin
loadingView.setSegments(12) // 12个关卡
loadingView.setProgress(currentLevel / 12f)
```

## 演示应用

项目包含完整的演示应用 `LoadingDemoActivity`，提供：

1. **进度调节滑块** - 实时调整 0-100% 进度
2. **分段数滑块** - 调整 3-20 个分段
3. **颜色方案按钮**:
   - 默认三色方案
   - 单色蓝色方案
   - 彩虹六色方案

运行应用后，在主菜单选择 "Loading View Demo" 即可体验。

## 技术实现

### 绘制流程

1. 计算圆环的外半径和内半径
2. 遍历每个分段：
   - 计算分段起始角度
   - 判断该分段是否已完成
   - 选择对应的颜色对
   - 绘制圆环扇形路径
3. 绘制进度指示箭头

### 关键算法

**分段完成判断**:
```kotlin
val segmentEndProgress = (i + 1).toFloat() / segments
val isLit = progress >= segmentEndProgress
```

**箭头角度计算**:
```kotlin
val angle = -90f + progress * 360f
```

**颜色循环选择**:
```kotlin
val colorPairIndex = i % colorPairs.size
```

## 性能优化

- Paint 对象预创建和复用
- Path 对象复用（每次 reset）
- RectF 对象复用
- 使用抗锯齿标志提供平滑渲染

## 项目结构

```
ChessGame/
├── app/src/main/java/com/chenjili/chessgame/
│   ├── pages/
│   │   ├── chess/ui/
│   │   │   └── CircularLoadingView.kt      # 核心组件
│   │   └── loading/
│   │       └── LoadingDemoActivity.kt      # 演示Activity
│   └── MainActivity.kt                      # 主菜单
├── docs/
│   ├── CircularLoadingView.md              # API文档
│   ├── CircularLoadingView_Design.md       # 设计文档
│   ├── IMPLEMENTATION_SUMMARY.md           # 实现总结
│   └── README.md                           # 本文件
└── app/src/main/AndroidManifest.xml        # 配置文件
```

## 文档

- **[API文档](CircularLoadingView.md)** - 详细的使用方法和API说明
- **[设计文档](CircularLoadingView_Design.md)** - 技术实现细节和原理
- **[实现总结](IMPLEMENTATION_SUMMARY.md)** - 开发过程和测试建议

## 兼容性

- **最低 Android 版本**: API 29 (Android 10)
- **目标 Android 版本**: API 36
- **依赖**: 仅 Android 标准库

## 扩展建议

### 当前版本可以考虑添加：

1. **XML 属性支持** - 通过 attrs.xml 定义自定义属性
2. **动画效果** - 进度变化时的平滑过渡动画
3. **自定义箭头** - 支持不同箭头样式
4. **圆环宽度配置** - 可调节圆环粗细
5. **分段间隙** - 分段之间的间隔配置
6. **中心文本** - 在圆心显示百分比文字
7. **触摸交互** - 支持点击或拖动设置进度

## 许可证

本项目是 ChessGame 项目的一部分。

## 贡献

欢迎提交 Issue 和 Pull Request！

## 更新日志

### v1.0.0 (2026-01-19)
- ✅ 初始版本发布
- ✅ 实现核心功能
- ✅ 添加演示应用
- ✅ 完善文档

## 联系方式

如有问题或建议，请通过 GitHub Issues 联系。
