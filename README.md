# Nearwave

An offline walkie-talkie for Android. Hold a button and the people near you hear you, with no mobile network, no Wi-Fi network and no internet. Built for family members who end up seated apart on planes, buses and trains, and simple enough for parents and grandparents to use.

## What it does

- **Push to talk.** Hold the big amber button and speak. Everyone connected on your channel hears you live. A "tap to start, tap to stop" mode is available in settings for unsteady hands.
- **Buzz.** One tap makes everyone's phone vibrate hard and shows a notification, even with the screen off.
- **Messages.** Short texts plus one-tap quick messages ("Are you okay?", "Come to me", "I need help").
- **Radar.** Shows who on your channel is near, a few rows away, or far. Phones can tell signal strength but not direction, so people move in and out on the rings rather than around them.
- **Channels.** One person creates a channel and reads out its 8-character code. Everyone else types it once. Phones on other channels never see you.

## How it works

| Piece | Technology | Why |
|---|---|---|
| Links between phones | [Google Nearby Connections](https://developers.google.com/nearby/connections/overview) (`P2P_CLUSTER`) | Bluetooth + Wi-Fi peer-to-peer with no network, handles discovery and encryption |
| Radar distance | Bluetooth Low Energy beacon + RSSI | Nearby Connections doesn't expose signal strength |
| Voice | 16 kHz mono, G.711 mu-law, 40 ms frames | Zero dependencies, clear speech, about 128 kbps |
| Staying reachable | Foreground service (`connectedDevice`) | Keeps the link alive with the screen off so buzzes arrive |
| UI | Jetpack Compose, Material 3, custom design system | See [docs/DESIGN.md](docs/DESIGN.md) |

There is no server and no account. Everything is stored on the phone.

## Install on your phone

Every push to `main` builds an APK on GitHub Actions.

1. On your phone, open this repository on GitHub and go to **Releases → Latest build**.
2. Tap the `.apk` file to download it, then open it. Allow installing from your browser if Android asks.
3. Later builds install over the old one (they share a signing key), so your channel and name are kept.

Build artifacts are also attached to each run under the **Actions** tab.

## Using it

1. Everyone installs Nearwave and enters their name.
2. One person taps **Create a new channel** and reads the code out. Everyone else taps **Join with a code**.
3. Allow the permissions. On a plane: turn on flight mode, then switch Bluetooth (and Wi-Fi) back on.
4. Keep Nearwave running. For reliable buzzes, open **Settings → Running in the background** and set Nearwave to unrestricted. Xiaomi, Oppo, Vivo, Realme and Samsung phones are aggressive about closing apps.

## Building

You don't need a computer: GitHub Actions builds it (see `.github/workflows/build.yml`). To build locally, open the folder in Android Studio (Ladybug or newer, JDK 17), or with Gradle 8.11 installed:

```bash
base64 -d keystore/debug.keystore.b64 > keystore/debug.keystore
gradle assembleDebug
```

## Project layout

```
app/src/main/java/app/nearwave/
  core/      Identity and channel codes, wire protocol, data models
  radio/     Nearby Connections links, BLE beacon for the radar, RadioController (state)
  audio/     Microphone capture, speaker playback, mu-law codec
  service/   Foreground service, notifications and vibration
  ui/        Design system (theme/), components, screens
```

## Known limits (v0.1)

- **Range** depends on the phones and how full the space is. Expect roughly 8–15 m over Bluetooth in a full cabin, more when Wi-Fi kicks in. Test with your own phones.
- **Radar distance is approximate.** Bluetooth signal strength jumps around; bands are smoothed over a few seconds.
- **Android only.** The packet format in `core/Protocol.kt` is kept transport-neutral so an iPhone version over Bluetooth Low Energy could be added later.
- **Requires Google Play services** (for Nearby Connections).
- `P2P_CLUSTER` is the most flexible strategy but mostly stays on Bluetooth. If voice stutters at range, lower `AudioConfig.SAMPLE_RATE` to `8_000`.

## Roadmap

- [ ] Field test on a bus or train and tune the radar thresholds
- [ ] Opus voice codec (about 16 kbps, better at range)
- [ ] QR code to join a channel
- [ ] Buzz one person from the radar
- [ ] Relay through a family member in the middle to extend range
- [ ] Hindi and Marathi translations

## Signing key

`keystore/debug.keystore.b64` is a **debug** key committed on purpose so every CI build has the same signature and installs as an update. Don't use it for a Play Store release. If you make the repository private or publish to the Play Store, create a real key and store it in GitHub secrets.

## License

MIT. See [LICENSE](LICENSE).
