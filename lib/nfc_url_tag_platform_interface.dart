import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'src/method_channel_nfc_url_tag.dart';

abstract class NfcUrlTagPlatform extends PlatformInterface {
  NfcUrlTagPlatform() : super(token: _token);

  static final Object _token = Object();

  static NfcUrlTagPlatform _instance = MethodChannelNfcUrlTag();

  static NfcUrlTagPlatform get instance => _instance;

  static set instance(NfcUrlTagPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<bool> isNfcAvailable() {
    throw UnimplementedError('isNfcAvailable() has not been implemented.');
  }

  Future<bool> isHceSupported() {
    throw UnimplementedError('isHceSupported() has not been implemented.');
  }

  Future<bool> isNfcEnabled() {
    throw UnimplementedError('isNfcEnabled() has not been implemented.');
  }

  Future<void> openNfcSettings() {
    throw UnimplementedError('openNfcSettings() has not been implemented.');
  }

  Future<void> startSharing({required String url}) {
    throw UnimplementedError('startSharing() has not been implemented.');
  }

  Future<void> stopSharing() {
    throw UnimplementedError('stopSharing() has not been implemented.');
  }

  Stream<void> get tagReadStream {
    throw UnimplementedError('tagReadStream has not been implemented.');
  }

  Stream<bool> get nfcEnabledStream {
    throw UnimplementedError('nfcEnabledStream has not been implemented.');
  }
}
