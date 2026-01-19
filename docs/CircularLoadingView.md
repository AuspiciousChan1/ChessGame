# CircularLoadingView

自定义圆环进度加载视图组件

## 功能特性

CircularLoadingView 是一个Android自定义View组件，实现了以下功能：

### 1. 圆环分段
- 圆环被分为 n 等分（n 可配置，默认为 8）
- 每个分段可以独立显示暗色或亮色

### 2. 进度指示箭头
- 从圆心指向圆环的箭头
- 起始方向向上（12点钟方向）
- 顺时针旋转表示进度
- 100% 进度 = 完整一圈（360度）

### 3. 进度显示
- 接受 0.0 到 1.0 的浮点数作为进度值
- 当进度超过 1/n 时，对应的分段从暗色变为亮色
- 例如：8个分段时，进度 > 0.125 时第一个分段变亮

### 4. 多色配置
- 支持配置多组暗色/亮色对
- 分段循环选取颜色对
- 默认提供3组颜色：
  - 灰色/绿色
  - 浅橙/深橙
  - 浅蓝/深蓝

## 使用方法

### 基本使用

```kotlin
// 在代码中创建
val loadingView = CircularLoadingView(context)

// 设置进度 (0.0 - 1.0)
loadingView.setProgress(0.5f) // 50% 进度

// 设置分段数
loadingView.setSegments(12) // 12个分段
```

### XML布局中使用

```xml
<com.chenjili.chessgame.pages.chess.ui.CircularLoadingView
    android:id="@+id/loading_view"
    android:layout_width="300dp"
    android:layout_height="300dp" />
```

### 自定义颜色

```kotlin
// 设置单一颜色方案
loadingView.setSingleColorPair(
    darkColor = Color.parseColor("#BBDEFB"),
    lightColor = Color.parseColor("#2196F3")
)

// 设置多组颜色方案
loadingView.setColorPairs(listOf(
    CircularLoadingView.ColorPair(
        Color.parseColor("#FFCDD2"),
        Color.parseColor("#F44336")
    ),
    CircularLoadingView.ColorPair(
        Color.parseColor("#C8E6C9"),
        Color.parseColor("#4CAF50")
    )
))

// 添加单个颜色对到现有列表
loadingView.addColorPair(
    darkColor = Color.parseColor("#E1BEE7"),
    lightColor = Color.parseColor("#9C27B0")
)
```

## API 文档

### 方法

#### setProgress(value: Float)
设置当前进度值
- **参数**: value - 进度值，范围 0.0 到 1.0
- **说明**: 0.0 表示 0%，1.0 表示 100%

#### setSegments(count: Int)
设置圆环分段数量
- **参数**: count - 分段数量，最小为 1
- **说明**: 更多分段可以提供更精细的进度显示

#### setSingleColorPair(darkColor: Int, lightColor: Int)
设置单一颜色方案（清除现有所有颜色对）
- **参数**: 
  - darkColor - 未完成分段的颜色
  - lightColor - 已完成分段的颜色

#### setColorPairs(pairs: List<ColorPair>)
设置多组颜色方案
- **参数**: pairs - ColorPair 列表
- **说明**: 分段将循环使用这些颜色对

#### addColorPair(darkColor: Int, lightColor: Int)
添加一个颜色对到现有列表
- **参数**: 
  - darkColor - 未完成分段的颜色
  - lightColor - 已完成分段的颜色

### 数据类

#### ColorPair
颜色对数据类
- **darkColor: Int** - 暗色（未完成状态）
- **lightColor: Int** - 亮色（已完成状态）

## 示例

项目中包含了一个完整的演示Activity：`LoadingDemoActivity`

演示内容包括：
- 通过滑块调整进度（0% - 100%）
- 通过滑块调整分段数（3 - 20）
- 切换不同的颜色方案：
  - 默认三色方案
  - 单色蓝色方案
  - 彩虹六色方案

运行应用后，在主菜单选择"Loading View Demo"即可查看演示。

## 实现细节

### 绘制逻辑

1. **圆环分段绘制**
   - 使用 Path 绘制每个分段
   - 每个分段是一个圆环扇形
   - 根据进度判断分段是暗色还是亮色

2. **箭头绘制**
   - 箭头从圆心开始
   - 长度延伸到圆环外边缘
   - 角度根据进度计算：-90° + progress * 360°

3. **颜色循环**
   - 使用模运算 `i % colorPairs.size` 循环选取颜色对
   - 不同分段可以有不同的颜色组合

### 性能优化

- 使用 Paint 对象复用
- 采用 Path 对象复用
- RectF 对象复用避免频繁创建
- 抗锯齿处理提供流畅显示

## 扩展建议

未来可以添加的功能：
1. XML 属性支持（通过 attrs.xml 定义）
2. 动画效果（进度变化时的平滑过渡）
3. 自定义箭头样式
4. 可配置的圆环宽度
5. 分段间隙配置
6. 文字显示（百分比或自定义文本）

## 兼容性

- 最低 Android API 级别：29 (Android 10)
- 使用标准 Android Canvas API
- 无外部依赖
