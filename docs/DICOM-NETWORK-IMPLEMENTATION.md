# DICOMネットワーク通信機能 実装状況とエラー対応

## 概要

このドキュメントは、DICOMネットワーク通信機能（C-ECHO、C-FIND、C-MOVE）の実装状況、発生したエラー、修正方法をまとめたものです。

最終更新: 2024年

---

## 実装目的

OrthancサーバーからDICOMデータを取得できるようにするため、DICOMネットワーク通信機能を実装します。

---

## 実装クラス

### 1. DicomEchoSCU
- **目的**: C-ECHO（接続確認）
- **機能**: Orthancサーバーへの接続を確認
- **状態**: ✅ 実装完了（エラー修正済み）

### 2. DicomQuerySCU
- **目的**: C-FIND（検索）
- **機能**: 
  - Studyレベルの検索
  - Seriesレベルの検索
- **状態**: ✅ 実装完了（エラー修正済み）

### 3. DicomMoveSCU
- **目的**: C-MOVE（取得）
- **機能**:
  - Studyの取得
  - Seriesの取得
- **状態**: ✅ 実装完了（エラー修正済み）

---

## 発生したエラーと修正

### 1. AssociationのAutoCloseable問題

**エラー**:
```
エラー: 不適合な型: try-with-resourceは変数型に使用できません
(AssociationをAutoCloseableに変換できません)
```

**発生箇所**:
- `DicomEchoSCU.java:46`
- `DicomQuerySCU.java:94, 152`
- `DicomMoveSCU.java:105`

**修正方法**:
```java
// ❌ 誤ったコード
try (Association as = ae.connect(remoteAE, null)) {
    // ...
}

// ✅ 正しいコード
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

**理由**: `Association`は`AutoCloseable`を実装していないため、try-with-resourcesは使用できません。

### 2. getInt()の引数不足

**エラー**:
```
エラー: getIntに適切なメソッドが見つかりません(int)
メソッド Attributes.getInt(int,int)は使用できません
```

**発生箇所**:
- `DicomEchoSCU.java:52`
- `DicomMoveSCU.java:122`

**修正方法**:
```java
// ❌ 誤ったコード
int status = attributes.getInt(Tag.Status);

// ✅ 正しいコード
int status = attributes.getInt(Tag.Status, -1);
```

**理由**: `getInt()`メソッドはデフォルト値を指定する必要があります。

### 3. UID定数名が存在しない

**エラー**:
```
エラー: シンボルを見つけられません
シンボル:   変数 StudyRootQueryRetrieveInformationModelFIND
場所: クラス UID
```

**発生箇所**:
- `DicomQuerySCU.java:98, 156`
- `DicomMoveSCU.java:111`

**修正方法**:
```java
// ✅ 正しい定数名
UID.StudyRootQueryRetrieveInformationModelFind  // Find（大文字F）
UID.StudyRootQueryRetrieveInformationModelMove  // Move（大文字M）
```

### 4. Tag定数名が存在しない

**エラー**:
```
エラー: シンボルを見つけられません
シンボル:   変数 NumberOfCompletedSubOperations
場所: クラス Tag
```

**発生箇所**:
- `DicomMoveSCU.java:125-127`

**修正方法**:
```java
// ✅ 正しい定数名
Tag.NumberOfCompletedSubOperations
Tag.NumberOfFailedSubOperations
Tag.NumberOfRemainingSubOperations
```

---

## 実装例

### C-ECHOの実装

```java
Association as = null;
try {
    as = ae.connect(remoteAE, null);
    DimseRSP rsp = as.cecho();
    int status = rsp.getCommand().getInt(Tag.Status, -1);
    return status == 0;
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

### C-FINDの実装

```java
Association as = null;
try {
    as = ae.connect(remoteAE, null);
    DimseRSP rsp = as.cfind(
        UID.StudyRootQueryRetrieveInformationModelFind,
        Priority.MEDIUM,
        keys,
        UID.ImplicitVRLittleEndian,
        0,  // autoCancel
        Integer.MAX_VALUE  // capacity
    );
    // レスポンス処理
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

### C-MOVEの実装

```java
Association as = null;
try {
    as = ae.connect(remoteAE, null);
    DimseRSP rsp = as.cmove(
        UID.StudyRootQueryRetrieveInformationModelMove,
        Priority.MEDIUM,
        keys,
        UID.ImplicitVRLittleEndian,
        destinationAET
    );
    while (rsp.next()) {
        Attributes cmd = rsp.getCommand();
        int status = cmd.getInt(Tag.Status, -1);
        int completed = cmd.getInt(Tag.NumberOfCompletedSubOperations, 0);
        int failed = cmd.getInt(Tag.NumberOfFailedSubOperations, 0);
        int remaining = cmd.getInt(Tag.NumberOfRemainingSubOperations, 0);
        // 進捗処理
    }
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

---

## Orthancサーバー設定

通常のOrthancサーバー設定:
- **AET**: ORTHANC
- **Host**: localhost
- **Port**: 4242

設定は設定ファイルまたは環境変数で変更可能。

---

## 参考資料

詳細なAPIリファレンスは以下を参照：
- `DCM4CHE-API-REFERENCE.md` - dcm4che API完全リファレンス
- dcm4che公式ドキュメント: https://www.dcm4che.org/
- dcm4che GitHub: https://github.com/dcm4che/dcm4che

---

## 変更履歴

- 2024年: DICOM Network関連のドキュメント（26-27, 30-31）をこのドキュメントに統合

