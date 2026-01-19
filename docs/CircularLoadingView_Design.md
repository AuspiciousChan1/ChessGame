# CircularLoadingView 组件设计说明

## 视觉示例

### 8分段圆环，50%进度

```
           ↑ 箭头指向进度方向
          /
         /
    [●] [●]       ● = 亮色 (已完成)
  [●]     [○]     ○ = 暗色 (未完成)
 
 [●]   ⭘   [○]    ⭘ = 圆心
 
  [○]     [○]
    [○] [○]

分段编号: 0-7
完成进度: 0-3 段亮色，4-7段暗色
进度值: 0.5 (50%)
```

## 组件结构

### 1. 圆环 (Ring)
- **外半径**: size * 0.4
- **内半径**: size * 0.25
- **分段数**: 可配置（默认8）
- **起始角度**: -90° (12点钟方向)
- **分段角度**: 360° / segments

### 2. 箭头 (Arrow)
- **起点**: 圆心
- **终点**: 外半径 + 10px
- **角度计算**: -90° + progress * 360°
- **形状**: 三角形箭头

### 3. 颜色系统

#### 默认配色方案
```
分段0: 灰色(暗) → 绿色(亮)   #CCCCCC → #4CAF50
分段1: 浅橙(暗) → 深橙(亮)   #FFCCBC → #FF5722
分段2: 浅蓝(暗) → 深蓝(亮)   #C5CAE9 → #3F51B5
分段3: 灰色(暗) → 绿色(亮)   (循环)
...
```

## 进度计算逻辑

### 分段高亮判断
```kotlin
for (i in 0 until segments) {
    val segmentEndProgress = (i + 1).toFloat() / segments
    val isLit = progress >= segmentEndProgress
    
    // 示例: segments = 8
    // 分段0: segmentEndProgress = 0.125
    // 分段1: segmentEndProgress = 0.250
    // ...
    // 分段7: segmentEndProgress = 1.000
}
```

### 进度到角度转换
```kotlin
angle = -90° + progress * 360°

进度0%:   angle = -90°   (向上)
进度25%:  angle = 0°     (向右)
进度50%:  angle = 90°    (向下)
进度75%:  angle = 180°   (向左)
进度100%: angle = 270°   (向上，完成一圈)
```

## 使用场景

### 1. 文件下载进度
```kotlin
val loadingView = CircularLoadingView(context)
loadingView.setSegments(10)  // 10个分段，每段代表10%

// 监听下载进度
downloadManager.addListener { progress ->
    loadingView.setProgress(progress / 100f)
}
```

### 2. 任务完成度
```kotlin
// 5个任务，每完成一个任务更新进度
val totalTasks = 5
var completedTasks = 0

loadingView.setSegments(totalTasks)

fun onTaskComplete() {
    completedTasks++
    loadingView.setProgress(completedTasks.toFloat() / totalTasks)
}
```

### 3. 游戏关卡进度
```kotlin
// 12关卡，彩虹配色
loadingView.setSegments(12)
loadingView.setColorPairs(rainbowColors)

fun updateLevel(currentLevel: Int) {
    loadingView.setProgress(currentLevel / 12f)
}
```

## 技术细节

### 坐标系统
- Canvas 坐标系：X轴向右，Y轴向下
- 角度系统：0°为水平向右，逆时针为正（标准数学系统）
- 圆环起点：-90°（向上，12点钟方向）

### Path绘制
每个分段由以下部分组成：
1. 外弧（外半径）
2. 连接线（从外弧末端到内弧末端）
3. 内弧（内半径，反向）
4. 连接线（从内弧末端回到外弧起点）

### 性能考虑
- Paint对象预创建并复用
- Path对象在绘制前reset()
- RectF对象复用
- 使用ANTI_ALIAS_FLAG提供平滑边缘

## 代码示例

### 完整示例
```kotlin
class MyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val loadingView = CircularLoadingView(this).apply {
            // 配置分段数
            setSegments(8)
            
            // 设置初始进度
            setProgress(0.0f)
            
            // 自定义颜色
            setSingleColorPair(
                darkColor = Color.LTGRAY,
                lightColor = Color.BLUE
            )
        }
        
        // 模拟进度更新
        var progress = 0f
        Handler(Looper.getMainLooper()).postDelayed(object : Runnable {
            override fun run() {
                progress += 0.01f
                if (progress <= 1.0f) {
                    loadingView.setProgress(progress)
                    Handler(Looper.getMainLooper()).postDelayed(this, 50)
                }
            }
        }, 50)
        
        setContentView(loadingView)
    }
}
```
