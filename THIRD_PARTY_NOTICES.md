# Third-party notices## PixelPlayerOSS

MiniMusic’s landscape player arrangement adapts the `FullPlayerLandscapeContent` layout structure from PixelPlayerOSS.

Source: https://github.com/PixelPlayerHQ/PixelPlayerOSS
License: GNU General Public License, version 3 (GPLv3). The applicable license text is included in the PixelPlayerOSS repository at https://github.com/PixelPlayerHQ/PixelPlayerOSS/blob/main/LICENSE.

MiniMusic-specific playback state, artwork rendering, metadata, controls, queue behavior, lyrics navigation, miniplayer behavior, and slider implementation remain separate from PixelPlayerOSS.

## Gramophone

MiniMusic’s lyrics engine reuses Gramophone's GPL-3.0 lyrics parsing, demuxing, and ranking stack (`logic/utils/SemanticLyrics.kt`, `logic/utils/LrcUtils.kt` from https://github.com/FoedusProgramme/Gramophone).

This component is licensed under the GNU General Public License, version 3 (GPLv3). The applicable license text is included in the Gramophone repository at https://github.com/FoedusProgramme/Gramophone/blob/beta/LICENSE.

MiniMusic's decoder, renderer, embedded-metadata extraction road, ranking preference, and display wiring remain separate from Gramophone.
