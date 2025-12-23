# Java Swing JSplitPane のよくある問題と対策

## 問題の概要

Java Swingの`JSplitPane`を使用する際、以下のような問題が頻繁に発生します：

1. **ディバイダー位置が設定されない**
   - `setDividerLocation(int)`を呼んでも、コンポーネントが表示される前に呼ぶと無視される
   - `setResizeWeight()`と`setDividerLocation()`を併用すると、`setResizeWeight()`が優先される

2. **ウィンドウリサイズ時にディバイダー位置が変わる**
   - ウィンドウをリサイズすると、設定したディバイダー位置が維持されない

3. **コンポーネントのサイズが意図通りにならない**
   - `setPreferredSize()`や`setMinimumSize()`を設定しても、レイアウトマネージャーが自動調整する

## 対策

### 1. ステータスバーの固定

ステータスバーのような固定高さのコンポーネントは、`JSplitPane`から外して`BorderLayout.SOUTH`に直接配置する：

```java
// ❌ 悪い例: JSplitPaneでステータスバーを管理
JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
mainSplit.setTopComponent(contentPanel);
mainSplit.setBottomComponent(statusPanel); // これだと縮む

// ✅ 良い例: BorderLayout.SOUTHに直接配置
JPanel mainPanel = new JPanel(new BorderLayout());
mainPanel.add(contentPanel, BorderLayout.CENTER);
mainPanel.add(statusPanel, BorderLayout.SOUTH);
statusPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25)); // 高さ固定
```

### 2. ディバイダー位置の設定タイミング

`setDividerLocation()`は、コンポーネントが表示された後に呼ぶ必要がある：

```java
// ❌ 悪い例: コンポーネント追加直後に呼ぶ
rightSplit.setDividerLocation(720); // 無視される可能性がある

// ✅ 良い例: SwingUtilities.invokeLater()で遅延実行
SwingUtilities.invokeLater(() -> {
    rightSplit.setDividerLocation(720);
});
```

### 3. ウィンドウリサイズ時の対応

ウィンドウリサイズ時にディバイダー位置を再設定する：

```java
addComponentListener(new ComponentAdapter() {
    @Override
    public void componentResized(ComponentEvent e) {
        updateDividerLocation();
    }
});
```

### 4. setResizeWeightとsetDividerLocationの併用

`setResizeWeight()`と`setDividerLocation()`を併用すると、`setResizeWeight()`が優先される場合がある。
固定高さが必要な場合は、`setResizeWeight()`を使わない：

```java
// ❌ 悪い例: 両方設定すると、setResizeWeightが優先される
rightSplit.setResizeWeight(0.8);
rightSplit.setDividerLocation(720); // 無視される可能性がある

// ✅ 良い例: 固定高さが必要な場合は、setResizeWeightを使わない
// rightSplit.setResizeWeight(0.8); // コメントアウト
rightSplit.setDividerLocation(720);
```

### 5. 複数の方法を試行

より確実にするため、複数の方法を試行する：

```java
private void updateDividerLocation() {
    if (rightSplit == null) return;
    
    int dividerLocation = calculateDividerLocation();
    
    // 方法1: 通常のsetDividerLocation
    rightSplit.setDividerLocation(dividerLocation);
    
    // 方法2: パーセンテージで設定
    double percentage = (double) dividerLocation / availableHeight;
    rightSplit.setDividerLocation(percentage);
    
    // 方法3: 再描画を強制
    SwingUtilities.invokeLater(() -> {
        rightSplit.setDividerLocation(dividerLocation);
        rightSplit.revalidate();
        rightSplit.repaint();
    });
}
```

## 参考

- [Java Swing JSplitPane Documentation](https://docs.oracle.com/javase/tutorial/uiswing/components/splitpane.html)
- [JSplitPane setDividerLocation not working](https://stackoverflow.com/questions/5507254/jsplitpane-setdividerlocation-not-working)

