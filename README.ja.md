# BitwigOSC

[English](README.md)

[TouchOSC](https://hexler.net/touchosc) から Bitwig Studio を、OSC で直接操作するための Bitwig の拡張です。

- 画面の上には、どのページでも再生、停止、ループ、今の小節.拍が出ます。再生と停止は灰色の上に白い ▶ ■ で、再生中は再生のボタンが Bitwig の橙になります。
- 8 本のトラックが Bitwig と同じく上から下に 1 行ずつ並び、mute / solo / arm と横向きの音量フェーダーを操作できます。▲ ▼ でトラックを 1 本ずつずらせます(Clips と同じ範囲で、Bitwig の枠も動きます)。
- 2 枚目のタブ「Clips」で、8 × 8 のマス(行がトラック、列がシーン)からクリップを起動できます。シーンの起動、トラックの停止、範囲を 1 つずつずらすボタン(▲ ▼ はトラック、◀ ▶ はシーン)もあり、クリップの状態は色で見えます。
- Bitwig 側で変えた値とトラックの色は、TouchOSC に送り返します。
- レイアウトを開くと、Bitwig の今の状態に揃います。

Bitwig Studio 6.1(Controller API 25)、Windows 11、iPad の TouchOSC で動作を確かめています。

## 入れ方

1. [Releases](../../releases) から `BitwigOSC.bwextension` と `BitwigOSC.tosc` を取ってきます。
2. `BitwigOSC.bwextension` を `ドキュメント/Bitwig Studio/Extensions/` に置きます。
3. Bitwig の **Settings > Controllers > + Add Controller** で、**BitwigOSC > TouchOSC** を追加します。
4. 追加したコントローラーの設定で、**TouchOSC host** に TouchOSC を動かす機器の IP アドレスを入れます。
5. TouchOSC で `BitwigOSC.tosc` を開き、**Connections > OSC** を次のように設定します。
   - **Host**: Bitwig を動かす PC の IP アドレス
   - **Send port**: `10001`
   - **Receive port**: `10002`

ポートや送り先を変えると、拡張は自動で起動し直します。

## 設定

| 設定 | 初期値 | 意味 |
|---|---|---|
| Receive port | 10001 | TouchOSC → Bitwig |
| TouchOSC host | 127.0.0.1 | 拡張から送る先 |
| Send port | 10002 | Bitwig → TouchOSC |
| Log incoming messages | オン | 届いたメッセージを Control Script Console に出す |

## OSC のアドレス

`/{番号}/...` の番号は、ミキサーに今出ている 8 本の上からの順番です(1〜8)。`/mixer/scrollUp`・`scrollDown` でずらせます。`/0/...` はトランスポート(再生やループ)です。
詳しい決まりは [docs/specs/osc-protocol.md](docs/specs/osc-protocol.md) にあります。

| アドレス | 値 | 向き・意味 |
|---|---|---|
| `/{1-8}/volume`、`/{1-8}/pan` | 0.0〜1.0 | 両方向 |
| `/{1-8}/mute`・`solo`・`arm`・`active` | 1.0 / 0.0 | 両方向 |
| `/0/playState`、`/0/loopEnabled` | 1.0 / 0.0 | 両方向 |
| `/0/stop` | 1.0 | TouchOSC → Bitwig。再生を止める |
| `/0/position` | 文字 | Bitwig → TouchOSC。今の小節.拍(例 `12.3`) |
| `/{1-8}/colorR`・`colorG`・`colorB` | 0.0〜1.0 | Bitwig → TouchOSC。トラックがない位置は黒 |
| `/sync` | なし | TouchOSC → Bitwig。今の状態を全部送り返す |
| `/ping` | なし | TouchOSC → Bitwig。`/pong` を返す |
| `/clip/{1-8}/{1-8}/launch` | 1.0 | TouchOSC → Bitwig。トラック・シーンのマスのクリップを起動する |
| `/clip/{1-8}/stop`、`/scene/{1-8}/launch` | 1.0 | TouchOSC → Bitwig。トラックを止める / シーンを起動する |
| `/mixer/scrollUp`・`scrollDown` | 1.0 | TouchOSC → Bitwig。トラックの範囲を 1 本ずらす(`/clip/scrollUp`・`scrollDown` と同じ) |
| `/clip/scrollUp`・`scrollDown`・`scrollLeft`・`scrollRight` | 1.0 | TouchOSC → Bitwig。上下はトラック、左右はシーンの範囲を 1 つずらす |
| `/clip/{1-8}/{1-8}/colorR`・`colorG`・`colorB` | 0.0〜1.0 | Bitwig → TouchOSC。マスの色。止まっていると暗く、録音中は赤 |
| `/clip/{1-8}/colorR`・`colorG`・`colorB` | 0.0〜1.0 | Bitwig → TouchOSC。マスの範囲のトラックの色 |

`/clip`・`/scene` の番号はマスの範囲の中の位置です。トラックの範囲はミキサーと同じで、上下にずらすと両方のページが一緒に動きます。

このアドレスを送るレイアウトなら、自作のレイアウトでも使えます。

## うまく動かないとき

Bitwig で **Ctrl + Enter** を押して `console` と入れ、**Show Control Script Console** を開いて BitwigOSC を選ぶと、拡張のログが見えます。

| ログ | 意味 |
|---|---|
| `BitwigOSC 0.5.0: listening on 10001, sending to <送り先>:10002` | 拡張が起動した |
| `OSC ignored: <アドレス> (unknown address)` | 上の表にないアドレスが届いた |
| `OSC out: /pong` は出るのに TouchOSC に届かない | **TouchOSC host** が機器の IP アドレスになっているかを確かめる。UDP は届かなくてもエラーにならない |

## ソースからビルドする

JDK 21 以上が要ります。Gradle Wrapper は同梱しています。

```
cd extension
./gradlew test              # テスト
./gradlew installExtension  # ビルドして ドキュメント/Bitwig Studio/Extensions/ に置く
./gradlew generateLayout    # touchosc/BitwigOSC.tosc を作り直す
```

`touchosc/BitwigOSC.tosc` は手で直さないでください。`extension/src/touchosc/` から作るファイルで、コミットしたものが古いと `./gradlew test` が失敗します。

## ライセンス

[MIT](LICENSE)

これは非公式のプロジェクトで、Bitwig GmbH や Hexler とは関係がなく、両社の承認も受けていません。Bitwig と Bitwig Studio は Bitwig GmbH の商標です。TouchOSC は Hexler の商標です。
