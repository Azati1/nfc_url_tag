## 0.1.0

* Initial release.
* Android Host Card Emulation of an NFC Forum Type 4 Tag broadcasting an NDEF URI record.
* API: `startSharing`, `stopSharing`, `isNfcAvailable`, `isHceSupported`, `isNfcEnabled`,
  `openNfcSettings`, `tagReadStream`, `nfcEnabledStream`.
* Optional auto-stop after a `Duration`.
