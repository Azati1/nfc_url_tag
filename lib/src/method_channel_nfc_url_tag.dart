import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import '../nfc_url_tag_platform_interface.dart';

class MethodChannelNfcUrlTag extends NfcUrlTagPlatform {
  @visibleForTesting
  static const methodChannel = MethodChannel('com.azati.nfc_url_tag/methods');

  @visibleForTesting
  static const tagReadEventChannel = EventChannel(
    'com.azati.nfc_url_tag/events/tag_read',
  );

  @visibleForTesting
  static const nfcStatusEventChannel = EventChannel(
    'com.azati.nfc_url_tag/events/nfc_status',
  );

  Stream<void>? _tagReadStream;
  Stream<bool>? _nfcEnabledStream;

  @override
  Future<bool> isNfcAvailable() async {
    final result = await methodChannel.invokeMethod<bool>('isNfcAvailable');
    return result ?? false;
  }

  @override
  Future<bool> isHceSupported() async {
    final result = await methodChannel.invokeMethod<bool>('isHceSupported');
    return result ?? false;
  }

  @override
  Future<bool> isNfcEnabled() async {
    final result = await methodChannel.invokeMethod<bool>('isNfcEnabled');
    return result ?? false;
  }

  @override
  Future<void> openNfcSettings() {
    return methodChannel.invokeMethod<void>('openNfcSettings');
  }

  @override
  Future<void> startSharing({required String url}) {
    return methodChannel.invokeMethod<void>('startSharing', {'url': url});
  }

  @override
  Future<void> stopSharing() {
    return methodChannel.invokeMethod<void>('stopSharing');
  }

  @override
  Stream<void> get tagReadStream {
    return _tagReadStream ??=
        tagReadEventChannel.receiveBroadcastStream().cast<void>();
  }

  @override
  Stream<bool> get nfcEnabledStream {
    return _nfcEnabledStream ??= nfcStatusEventChannel
        .receiveBroadcastStream()
        .map((event) => event as bool);
  }
}
