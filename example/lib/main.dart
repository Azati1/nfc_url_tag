import 'dart:async';

import 'package:flutter/material.dart';
import 'package:nfc_url_tag/nfc_url_tag.dart';

void main() => runApp(const _App());

class _App extends StatelessWidget {
  const _App();

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'nfc_url_tag demo',
      theme: ThemeData(colorSchemeSeed: Colors.indigo, useMaterial3: true),
      home: const _HomePage(),
    );
  }
}

class _HomePage extends StatefulWidget {
  const _HomePage();

  @override
  State<_HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<_HomePage> {
  final _tag = NfcUrlTag.instance;
  final _urlController = TextEditingController(text: 'https://flutter.dev');

  bool? _nfcAvailable;
  bool? _hceSupported;
  bool _nfcEnabled = false;
  bool _sharing = false;
  int _tagReadCount = 0;
  DateTime? _lastRead;

  StreamSubscription<bool>? _nfcSub;
  StreamSubscription<void>? _tagSub;

  @override
  void initState() {
    super.initState();
    _refreshCapabilities();
    _nfcSub = _tag.nfcEnabledStream.listen((enabled) {
      setState(() => _nfcEnabled = enabled);
    });
    _tagSub = _tag.tagReadStream.listen((_) {
      setState(() {
        _tagReadCount += 1;
        _lastRead = DateTime.now();
      });
    });
  }

  @override
  void dispose() {
    _nfcSub?.cancel();
    _tagSub?.cancel();
    _urlController.dispose();
    super.dispose();
  }

  Future<void> _refreshCapabilities() async {
    final available = await _tag.isNfcAvailable();
    final hce = await _tag.isHceSupported();
    final enabled = await _tag.isNfcEnabled();
    if (!mounted) return;
    setState(() {
      _nfcAvailable = available;
      _hceSupported = hce;
      _nfcEnabled = enabled;
    });
  }

  Future<void> _start() async {
    final url = _urlController.text.trim();
    if (url.isEmpty) {
      _snack('URL is empty');
      return;
    }
    try {
      await _tag.startSharing(url: url);
      setState(() => _sharing = true);
    } catch (e) {
      _snack('startSharing failed: $e');
    }
  }

  Future<void> _stop() async {
    try {
      await _tag.stopSharing();
    } catch (e) {
      _snack('stopSharing failed: $e');
    } finally {
      if (mounted) setState(() => _sharing = false);
    }
  }

  void _snack(String msg) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text(msg)));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('nfc_url_tag'),
        actions: [
          IconButton(
            tooltip: 'Refresh capabilities',
            icon: const Icon(Icons.refresh),
            onPressed: _refreshCapabilities,
          ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          _capabilitiesCard(),
          const SizedBox(height: 16),
          _sharingCard(),
          const SizedBox(height: 16),
          _tagReadsCard(),
        ],
      ),
    );
  }

  Widget _capabilitiesCard() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Device', style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 8),
            _statusRow('NFC chip', _nfcAvailable),
            _statusRow('HCE supported', _hceSupported),
            _statusRow('NFC enabled', _nfcEnabled),
            const SizedBox(height: 12),
            FilledButton.tonalIcon(
              icon: const Icon(Icons.settings),
              label: const Text('Open NFC settings'),
              onPressed: _tag.openNfcSettings,
            ),
          ],
        ),
      ),
    );
  }

  Widget _sharingCard() {
    final canStart = (_hceSupported ?? false) && _nfcEnabled && !_sharing;
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Text('Sharing', style: Theme.of(context).textTheme.titleMedium),
                const Spacer(),
                Chip(
                  label: Text(_sharing ? 'ACTIVE' : 'IDLE'),
                  backgroundColor:
                      _sharing ? Colors.green.shade100 : Colors.grey.shade200,
                ),
              ],
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _urlController,
              enabled: !_sharing,
              decoration: const InputDecoration(
                border: OutlineInputBorder(),
                labelText: 'URL',
                hintText: 'https://example.com',
              ),
              keyboardType: TextInputType.url,
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                  child: FilledButton.icon(
                    icon: const Icon(Icons.play_arrow),
                    label: const Text('Start'),
                    onPressed: canStart ? _start : null,
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: OutlinedButton.icon(
                    icon: const Icon(Icons.stop),
                    label: const Text('Stop'),
                    onPressed: _sharing ? _stop : null,
                  ),
                ),
              ],
            ),
            if (!(_hceSupported ?? true))
              Padding(
                padding: const EdgeInsets.only(top: 12),
                child: Text(
                  'This device does not support Host Card Emulation.',
                  style: TextStyle(color: Colors.red.shade700),
                ),
              )
            else if (!_nfcEnabled)
              const Padding(
                padding: EdgeInsets.only(top: 12),
                child: Text('NFC is turned off. Enable it to start sharing.'),
              ),
          ],
        ),
      ),
    );
  }

  Widget _tagReadsCard() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Tag reads', style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 8),
            Row(
              children: [
                Icon(
                  Icons.nfc,
                  color:
                      _tagReadCount > 0 ? Colors.green : Colors.grey.shade500,
                ),
                const SizedBox(width: 8),
                Text(
                  '$_tagReadCount',
                  style: Theme.of(context).textTheme.headlineMedium,
                ),
                const SizedBox(width: 12),
                if (_lastRead != null)
                  Text(
                    'last at ${_formatTime(_lastRead!)}',
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
              ],
            ),
            const SizedBox(height: 4),
            Text(
              'Increments each time a reader reads the emulated tag.',
              style: Theme.of(context).textTheme.bodySmall,
            ),
          ],
        ),
      ),
    );
  }

  Widget _statusRow(String label, bool? value) {
    final Icon icon;
    if (value == null) {
      icon = Icon(Icons.help_outline, color: Colors.grey.shade500);
    } else if (value) {
      icon = const Icon(Icons.check_circle, color: Colors.green);
    } else {
      icon = const Icon(Icons.cancel, color: Colors.red);
    }
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 2),
      child: Row(children: [icon, const SizedBox(width: 8), Text(label)]),
    );
  }

  String _formatTime(DateTime dt) {
    String two(int n) => n.toString().padLeft(2, '0');
    return '${two(dt.hour)}:${two(dt.minute)}:${two(dt.second)}';
  }
}
