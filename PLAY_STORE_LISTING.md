# Play Store 提出用テキスト集

Play Consoleの各入力欄にそのままコピー&ペーストできるよう、日本語・英語でまとめています。

## ストア掲載情報

### 短い説明 (80文字以内)

**日本語:**
```
Wi-Fi・モバイル回線・VPNの速度を常時計測。数値と色で今の回線状態がひと目でわかる。
```

**English:**
```
Live Wi-Fi/cellular/VPN speed monitor. See your connection's health at a glance, color-coded.
```

### 詳しい説明 (4000文字以内)

**日本語:**
```
NetSpeedは、今つながっているネットワークの速度を常時計測するシンプルな速度モニターです。

■ こんな方におすすめ
・VPN接続時に「ハンドシェイクは成功しているのに遅い」体感を数値で確認したい
・Wi-Fiとモバイル回線、どちらが速いか切り替えながら比べたい
・普段使っている回線の調子を、アプリを開きっぱなしにせず常時把握したい

■ 主な機能
・ワンタップのON/OFFボタンで常時計測を開始・停止
・ダウンロード速度・アップロード速度・Ping(応答速度)を同時に表示
・数値に応じて緑(快適)/黄(普通)/赤(低速)に自動で色分け
・Wi-Fi / モバイル回線 / VPN経由 / 有線LAN を自動判定して表示
・モバイル回線ではテストサイズを自動的に縮小し、通信量を節約
・直近の速度推移をミニグラフで表示
・計測間隔やしきい値は設定画面から調整可能
・ONの間はバックグラウンドでも計測を継続(通知で確認可能)
・ダークモード対応、日本語/英語自動切り替え

■ プライバシーについて
本アプリは個人情報を一切収集しません。速度計測のための匿名の通信のみを行い、計測結果は端末内にのみ保存されます。詳細はプライバシーポリシーをご確認ください。

■ 動作環境
Android のほか、Windows / Linux 版も同じアプリとして提供しています。
```

**English:**
```
NetSpeed is a simple, always-on network speed monitor for your current connection.

■ Who it's for
- Anyone who's noticed a VPN "connects fine but feels slow" and wants a number to confirm it
- Anyone comparing Wi-Fi vs. cellular speed while switching between them
- Anyone who wants to keep an eye on their connection without babysitting the app

■ Features
- One-tap ON/OFF toggle for continuous monitoring
- Download speed, upload speed, and ping shown together
- Automatic green (excellent) / amber (fair) / red (poor) color coding
- Detects and shows your current connection: Wi-Fi, cellular, VPN, or Ethernet
- Automatically shrinks test size on cellular to save your data plan
- A rolling mini-graph of recent speed history
- Adjustable interval and thresholds in Settings
- Keeps monitoring in the background while ON (visible via a notification)
- Dark mode support, automatic Japanese/English switching

■ Privacy
NetSpeed collects no personal data. It only makes anonymous requests needed to measure speed, and results are stored on your device only. See the privacy policy for details.

■ Also available for
Windows and Linux, built from the same codebase.
```

## Foreground Service 利用目的の申告

Play Console の「アプリのコンテンツ」→ Foreground Service の権限に関する宣言フォームに貼り付けてください。

**日本語:**
```
本アプリはユーザーが手動でON/OFFを切り替える「常時ネットワーク速度計測」機能を提供しています。ONの間、Foreground Service (dataSync) がダウンロード/アップロード速度とレイテンシの計測を一定間隔で継続し、常時通知でその状態を表示します。これは、ユーザーがVPNアプリなど他のアプリを操作している間もネットワーク速度の変化(例: VPN接続直後の速度低下)をリアルタイムで確認できるようにするためです。ユーザーがOFFに切り替えると、このサービスは即座に停止します。バックグラウンドでの位置情報取得や広告、データ収集などは一切行いません。
```

**English:**
```
The app provides a manual ON/OFF "continuous network speed monitoring" feature. While ON, a dataSync foreground service repeatedly measures download/upload throughput and latency, and shows the current reading in an ongoing notification. This lets the user watch network speed change in real time (e.g., a slowdown right after a VPN handshake) while they are operating another app, such as a VPN client. The service stops immediately when the user switches monitoring OFF. It does not perform background location access, ads, or data collection of any kind.
```

## データセーフティ / コンテンツレーティング 回答の目安

- 収集するユーザーデータ: **なし**
- 第三者とのデータ共有: **なし**
- データの暗号化・削除リクエスト対応: 該当データがないため対象外
- 広告の有無: **なし**
- アプリ内課金: **なし**
- 対象年齢 / コンテンツレーティング: 暴力・性的表現・ギャンブル要素など一切なし → 全年齢向け(Everyone)で回答可能
- アカウント登録・ログイン: **不要**
