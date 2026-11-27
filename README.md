# BitwigOSC

[日本語](README.ja.md)

A Bitwig Studio controller extension that lets [TouchOSC](https://hexler.net/touchosc) control Bitwig directly over OSC.

- A header on every page with play, stop, loop and the current bar.beat. Play and stop are white ▶ ■ on grey, and the play button turns Bitwig orange while Bitwig is playing
- 8 tracks, one per row from top to bottom like Bitwig, each with mute / solo / arm and a horizontal volume fader. ▲ ▼ move the tracks one at a time, together with the Clips tab and the frame shown in Bitwig
- A second tab, "Clips": an 8 × 8 clip launcher grid (tracks are rows, scenes are columns) with scene launch, track stop and one-step scrolling (▲ ▼ tracks, ◀ ▶ scenes). Clip state is shown by colour
- Changes made in Bitwig, and the track colours, are sent back to TouchOSC
- Opening the layout fetches the current state from Bitwig

Tested with Bitwig Studio 6.1 (Controller API 25) on Windows 11 and TouchOSC on iPad.

## Install

1. Download `BitwigOSC.bwextension` and `BitwigOSC.tosc` from [Releases](../../releases).
2. Put `BitwigOSC.bwextension` in `Documents/Bitwig Studio/Extensions/`.
3. In Bitwig, open **Settings > Controllers > + Add Controller**, then add **BitwigOSC > TouchOSC**.
4. In the controller settings, set **TouchOSC host** to the IP address of the device that runs TouchOSC.
5. Open `BitwigOSC.tosc` in TouchOSC. Under **Connections > OSC**, set:
   - **Host**: the IP address of the PC that runs Bitwig
   - **Send port**: `10001`
   - **Receive port**: `10002`

The extension restarts by itself when you change a port or the host.

## Settings

| Setting | Default | |
|---|---|---|
| Receive port | 10001 | TouchOSC → Bitwig |
| TouchOSC host | 127.0.0.1 | Where the extension sends to |
| Send port | 10002 | Bitwig → TouchOSC |
| Log incoming messages | on | Prints received messages to the Control Script Console |

## OSC addresses

`/{n}/...` is the n-th track in the mixer view (1–8), counted from the top row after scrolling with `/mixer/scrollUp` / `scrollDown`. `/0/...` is the transport.

| Address | Value | |
|---|---|---|
| `/{1-8}/volume`, `/{1-8}/pan` | 0.0–1.0 | Both ways |
| `/{1-8}/mute`, `solo`, `arm`, `active` | 1.0 / 0.0 | Both ways |
| `/0/playState`, `/0/loopEnabled` | 1.0 / 0.0 | Both ways |
| `/0/stop` | 1.0 | TouchOSC → Bitwig. Stops playback |
| `/0/position` | string | Bitwig → TouchOSC. Current bar.beat, e.g. `12.3` |
| `/{1-8}/colorR`, `colorG`, `colorB` | 0.0–1.0 | Bitwig → TouchOSC. Black when there is no track |
| `/sync` | – | TouchOSC → Bitwig. Sends back the whole state |
| `/ping` | – | TouchOSC → Bitwig. Answers `/pong` |
| `/clip/{1-8}/{1-8}/launch` | 1.0 | TouchOSC → Bitwig. Launches the clip at track, scene |
| `/clip/{1-8}/stop`, `/scene/{1-8}/launch` | 1.0 | TouchOSC → Bitwig. Stops a track / launches a scene |
| `/mixer/scrollUp`, `scrollDown` | 1.0 | TouchOSC → Bitwig. Moves the tracks by one, same as `/clip/scrollUp` / `scrollDown` |
| `/clip/scrollUp`, `scrollDown`, `scrollLeft`, `scrollRight` | 1.0 | TouchOSC → Bitwig. Up / Down move the tracks by one, Left / Right move the scenes by one |
| `/clip/{1-8}/{1-8}/colorR`, `colorG`, `colorB` | 0.0–1.0 | Bitwig → TouchOSC. Slot colour, dimmed when stopped, red when recording |
| `/clip/{1-8}/colorR`, `colorG`, `colorB` | 0.0–1.0 | Bitwig → TouchOSC. Track colour of the clip grid |

Numbers under `/clip` and `/scene` are positions in the grid. The grid shares its tracks with the mixer, so scrolling up or down moves both pages.

You can also use the extension from your own layout, as long as it sends these addresses.

## Troubleshooting

To see the log in Bitwig, press **Ctrl + Enter**, type `console`, choose **Show Control Script Console**, and then select BitwigOSC.

| Log line | Meaning |
|---|---|
| `BitwigOSC 0.4.1: listening on 10001, sending to <host>:10002` | The extension started |
| `OSC ignored: <address> (unknown address)` | A message arrived with an address that is not in the table above |
| `OSC out: /pong`, but TouchOSC gets nothing | Check that **TouchOSC host** is the device's IP address. UDP does not report lost messages |

## Build from source

You need JDK 21 or later. The Gradle Wrapper is included.

```
cd extension
./gradlew test              # tests
./gradlew installExtension  # build and copy to Documents/Bitwig Studio/Extensions/
./gradlew generateLayout    # rebuild touchosc/BitwigOSC.tosc
```

Do not edit `touchosc/BitwigOSC.tosc` by hand. It is generated from `extension/src/touchosc/`, and `./gradlew test` fails if the committed file is out of date.

## License

[MIT](LICENSE)

This is an unofficial project. It is not affiliated with or endorsed by Bitwig GmbH or Hexler. Bitwig and Bitwig Studio are trademarks of Bitwig GmbH. TouchOSC is a trademark of Hexler.
