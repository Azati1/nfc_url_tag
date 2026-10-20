import 'nfc_url_tag_platform_interface.dart';

/// Emulates an NFC Type 4 tag that broadcasts a URL via Android Host Card
/// Emulation. Any NFC-enabled reader (another phone, iPhone with CoreNFC,
/// standard NFC reader apps) that taps the device will receive the URL as
/// an NDEF URI record and typically open it in a browser.
///
/// Android only — iOS does not permit third-party NFC tag emulation.
class NfcUrlTag {
  NfcUrlTag._();

  static final NfcUrlTag instance = NfcUrlTag._();

  /// True if the device has an NFC chip.
  Future<bool> isNfcAvailable() => NfcUrlTagPlatform.instance.isNfcAvailable();

  /// True if the device supports Host Card Emulation.
  Future<bool> isHceSupported() => NfcUrlTagPlatform.instance.isHceSupported();

  /// True if NFC is currently enabled in system settings.
  Future<bool> isNfcEnabled() => NfcUrlTagPlatform.instance.isNfcEnabled();

  /// Opens the system NFC settings screen.
  Future<void> openNfcSettings() =>
      NfcUrlTagPlatform.instance.openNfcSettings();

  /// Starts emulating an NFC tag that broadcasts [url].
  ///
  /// [url] may be any absolute URI (http(s), custom app deeplinks, etc.) — the
  /// scheme is validated loosely against RFC 3986.
  ///
  /// Emulation runs until you call [stopSharing]. If you want a time-bounded
  /// share, wrap this with your own `Timer`.
  Future<void> startSharing({required String url}) =>
      NfcUrlTagPlatform.instance.startSharing(url: url);

  /// Stops emulation.
  Future<void> stopSharing() => NfcUrlTagPlatform.instance.stopSharing();

  /// Emits each time a reader successfully reads the emulated tag.
  Stream<void> get tagReadStream => NfcUrlTagPlatform.instance.tagReadStream;

  /// Emits `true` / `false` when NFC is toggled on/off in system settings.
  Stream<bool> get nfcEnabledStream =>
      NfcUrlTagPlatform.instance.nfcEnabledStream;
}
