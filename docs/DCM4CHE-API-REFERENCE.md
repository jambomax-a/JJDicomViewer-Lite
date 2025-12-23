# dcm4che API完全リファレンス

## 概要

このドキュメントは、dcm4cheライブラリのAPI使用に関する完全なリファレンスです。実装時の注意点、正しい使用方法、エラー対応を含みます。

最終更新: 2024年（dcm4che-5.34.1を調査）

---

## 1. Associationクラス

### 場所
`org.dcm4che3.net.Association`

### 重要なメソッド

#### release() - 正常な終了
```java
public void release() throws IOException {
    state.writeAReleaseRQ(this);
}
```

**使用方法**:
- 正常な終了時に呼び出す
- `AutoCloseable`を実装していないため、try-with-resourcesは使用できない
- 手動で`close()`または`release()`を呼ぶ必要がある

### 使用パターン

```java
Association as = null;
try {
    as = ae.connect(remoteAE, null);
    // 操作を実行
} finally {
    if (as != null) {
        try {
            as.release();
            as.waitForSocketClose();
        } catch (IOException e) {
            logger.error("Failed to release association", e);
        }
    }
}
```

**注意点**:
- try-with-resourcesは使用不可
- 手動で`release()`を呼ぶ必要がある
- `waitForSocketClose()`で確実に終了するまで待つ

---

## 2. Attributes クラス

### getInt()メソッド

**正しい形式**:
```java
int value = attributes.getInt(tag, defaultValue);
```

**注意点**:
- `getInt(tag)`ではなく、`getInt(tag, defaultValue)`の形式
- デフォルト値を必ず指定する必要がある
- 既存コードでは`getInt(tag, 0)`または`getInt(tag, -1)`を使用

**使用例**:
```java
int status = rsp.getCommand().getInt(Tag.Status, -1);
int numRemaining = rsp.getCommand().getInt(Tag.NumberOfRemainingSubOperations, 0);
```

---

## 3. UID定数

### 確認した定数名（すべて正しい）

- `UID.StudyRootQueryRetrieveInformationModelFind` ✅
- `UID.StudyRootQueryRetrieveInformationModelMove` ✅
- `UID.VerificationSOPClass` ✅

### 使用例

```java
// Query操作
dataset.setString(Tag.QueryRetrieveLevel, VR.CS, "STUDY");
dataset.setString(Tag.PatientID, VR.LO, patientId);
cmds.putUID(Tag.AffectedSOPClassUID, UID.StudyRootQueryRetrieveInformationModelFind);

// Move操作
cmds.putUID(Tag.AffectedSOPClassUID, UID.StudyRootQueryRetrieveInformationModelMove);

// Echo操作
cmds.putUID(Tag.AffectedSOPClassUID, UID.VerificationSOPClass);
```

---

## 4. Tag定数

### 確認した定数名（すべて正しい）

- `Tag.Status` ✅
- `Tag.NumberOfCompletedSubOperations` ✅
- `Tag.NumberOfFailedSubOperations` ✅
- `Tag.NumberOfRemainingSubOperations` ✅

### 使用例

```java
int status = rsp.getCommand().getInt(Tag.Status, -1);
int numCompleted = rsp.getCommand().getInt(Tag.NumberOfCompletedSubOperations, 0);
int numFailed = rsp.getCommand().getInt(Tag.NumberOfFailedSubOperations, 0);
int numRemaining = rsp.getCommand().getInt(Tag.NumberOfRemainingSubOperations, 0);
```

---

## 5. よくあるエラーと修正

### エラー1: Associationのtry-with-resources使用

**エラー**:
```java
try (Association as = ae.connect(remoteAE, null)) {  // ❌ コンパイルエラー
    // ...
}
```

**修正**:
```java
Association as = null;
try {
    as = ae.connect(remoteAE, null);
    // ...
} finally {
    if (as != null) {
        try {
            as.release();
            as.waitForSocketClose();
        } catch (IOException e) {
            logger.error("Failed to release association", e);
        }
    }
}
```

### エラー2: getInt()のデフォルト値未指定

**エラー**:
```java
int status = attributes.getInt(Tag.Status);  // ❌ コンパイルエラー
```

**修正**:
```java
int status = attributes.getInt(Tag.Status, -1);  // ✅
```

---

## 6. 実装上の注意点

### Associationのリソース管理

1. **必ずrelease()を呼ぶ**
   - 正常終了時は`release()`を呼ぶ
   - エラー時もfinallyブロックで確実に`release()`を呼ぶ

2. **waitForSocketClose()の使用**
   - リソースの完全解放を保証するために使用
   - 非同期処理の完了を待つ場合に有効

### Attributesの値取得

1. **デフォルト値を常に指定**
   - `getInt()`, `getString()`などはデフォルト値を指定する
   - 値が存在しない場合の動作を明確にする

2. **nullチェック**
   - `getString()`はnullを返す可能性がある
   - nullチェックを適切に行う

---

## 7. 参考資料

- dcm4che公式ドキュメント: https://www.dcm4che.org/
- dcm4che GitHub: https://github.com/dcm4che/dcm4che
- dcm4che-5.34.1 ソースコード

---

## 変更履歴

- 2024年: dcm4che API関連のドキュメント（28-34）をこのドキュメントに統合

