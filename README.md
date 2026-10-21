# nfc_url_tag

Emulate an NFC Forum **Type 4 Tag** on Android that broadcasts a URL. Any
NFC-enabled reader (another phone, iPhone with CoreNFC, standard NFC reader
apps) that taps the device receives the URL as an NDEF URI record and
typically opens it in a browser — no companion reader app required.

![An Android phone shares a URL with nfc_url_tag; an iPhone taps it and opens the link in Safari](https://raw.githubusercontent.com/Azati1/nfc_url_tag/main/doc/demo.gif)

> **Android only.** iOS does not permit third-party NFC tag emulation at the
> OS level (Apple restricts the NFC chip to Apple Pay / Wallet).

## Install

```yaml
dependencies:
  nfc_url_tag: ^0.1.0
```

Requires Android 5.0+ (API 21) with NFC and Host Card Emulation
(`android.hardware.nfc.hce`).

## Usage

```dart
import 'package:nfc_url_tag/nfc_url_tag.dart';

final tag = NfcUrlTag.instance;

if (await tag.isHceSupported() && await tag.isNfcEnabled()) {
  await tag.startSharing(url: 'https://example.com');

  tag.tagReadStream.listen((_) {
    // A reader just read the emulated tag.
  });

  // ...later
  await tag.stopSharing();
}
```

### Reacting to NFC being toggled

```dart
tag.nfcEnabledStream.listen((enabled) {
  // Update UI when the user toggles NFC in settings.
});

await tag.openNfcSettings(); // Jump to the system NFC screen.
```

## API

| Method | Returns | Notes |
|---|---|---|
| `isNfcAvailable()` | `Future<bool>` | Device has an NFC chip. |
| `isHceSupported()` | `Future<bool>` | Device supports Host Card Emulation. |
| `isNfcEnabled()` | `Future<bool>` | NFC is currently turned on. |
| `openNfcSettings()` | `Future<void>` | Opens system NFC settings. |
| `startSharing({url})` | `Future<void>` | Starts tag emulation. |
| `stopSharing()` | `Future<void>` | Stops tag emulation. |
| `tagReadStream` | `Stream<void>` | Fires each time a reader reads the tag. |
| `nfcEnabledStream` | `Stream<bool>` | Fires when NFC is toggled on/off. |

## How it works

Registers a `HostApduService` that advertises the NFC Forum **NDEF Type 4
Tag** AID (`D2760000850101`) — the same AID any standard NFC reader selects
when it wants to read an NDEF tag. The service responds to the NFC Forum
Type 4 Tag command set with a single NDEF URI record.

## License

MIT.
