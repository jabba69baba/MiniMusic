# MiniMusic

A minimal, fully offline Android music player built with Jetpack Compose and
**Material 3 Expressive**. It combines Gramophone's philosophy (lean,
MediaStore-only, strict Material theming) with PixelPlayer's modern Compose/Media3
foundation — without any of the network-dependent extras (no lyrics fetching from
the web, no Deezer artist art, no AI playlists, no Chromecast). Everything plays
from files already on the device.

## Features

- Library browsing: **Songs**, **Albums**, **Artists** tabs, with live search and
  a draggable **alphabet fast-scroller** (with a letter popup) on the Songs list
- Tap an album or artist to drill into its song list
- Full playback: play/pause, next/previous, seek, **shuffle**, **repeat (off/all/one)**
- Persistent mini player + full-screen "now playing" screen with queue ("Up next")
- **Lyrics**, read straight out of the song file's own embedded ID3 `USLT` tag —
  no network lookup, no `.lrc` sidecar files required
- **Settings** screen — Appearance (dynamic color, theme mode), Player (auto-open
  lyrics, resume on launch), Content (minimum track length, rescan library), About
- Background playback via a `MediaSessionService` (system notification, lock-screen
  controls, audio-focus handling, headphone-unplug pause — all standard Media3 behavior)
- Material 3 **Expressive** theming throughout: `MaterialExpressiveTheme`, expressive
  motion, an expressive shape scale plus custom **blob** and **cookie/scallop**
  shapes on the album art and transport controls, dynamic color (Monet) on
  Android 12+, and a warm amber fallback palette on older devices
- **Google Sans Flex** typography, fetched via the downloadable Google Fonts
  provider (Google released it on Google Fonts under the OFL in Nov 2025)
- A launcher icon styled after wired earphones
- Zero network permissions beyond the font fetch's Play services call. Zero
  analytics. Zero ads.

## Not included (by design, for now)

Tag editing, home-screen widgets, Chromecast, AI playlists, folder browsing, and
user-created playlists — left out to keep this focused. The architecture (see
below) makes most of these straightforward to add later; see "Suggestions" below
for what I'd tackle next.

## Getting started

1. Open the project root in Android Studio (Ladybug or newer recommended).
2. Let Gradle sync — on first sync, Android Studio will offer to generate/repair
   the Gradle wrapper jar if it's missing; accept that prompt.
   - If you hit an "Incompatible Gradle JVM version" error, go to
     **Settings → Build, Execution, Deployment → Build Tools → Gradle** and set
     **Gradle JDK** to one of the bundled `jbr-17`/`jbr-21` options rather than
     whatever system JDK you have installed.
3. Run on a device or emulator running Android 8.0 (API 26) or newer.
4. Grant the music-library permission when prompted — the app cannot see any files
   without it, and it never requests anything beyond local media access.

> The emulator's default AVD has no music on it. Push a few files first, e.g.:
> `adb push my_song.mp3 /sdcard/Music/` then reboot the emulator once so MediaStore
> picks it up. For lyrics, the file needs an ID3v2 `USLT` frame — most tag editors
> (Mp3tag, Kid3) can add one for testing.

## Architecture

```
data/
  model/           Song, Album, Artist — simple data classes
  MusicRepository  Reads MediaStore (IO dispatcher); derives Album/Artist lists
                   by grouping songs — no separate cache to keep in sync
  LyricsReader     Parses the ID3v2 USLT frame straight out of the audio file
  SettingsRepository  DataStore-backed preferences, exposed as a Flow
playback/
  MusicService     MediaSessionService hosting a single ExoPlayer + MediaSession
  PlayerController Connects a MediaController to the service, exposes
                   StateFlow<PlaybackUiState>, and forwards transport commands
ui/
  theme/           Color/Shape/Type + MiniMusicTheme (MaterialExpressiveTheme),
                   plus ExpressiveShapes.kt (blob shape, scalloped "cookie" shape)
  viewmodel/       LibraryViewModel, PlayerViewModel, SettingsViewModel
  components/      SongListItem, AlbumGridItem, ArtistListItem, MiniPlayer,
                   AlphabetScrollbar
  screens/         LibraryScreen, FilteredSongsScreen, PlayerScreen (Now
                   Playing + Lyrics panels), SettingsScreen, PermissionScreen
  navigation/      Single NavHost: library -> album/artist drill-down -> player
                   / settings
```

Dependency injection is intentionally manual (`MainApplication` holds one instance
each of `MusicRepository`, `PlayerController`, `SettingsRepository`, and
`LyricsReader`) — a DI framework would add more ceremony than value at this size.
`MainActivity` is the only Activity; playback continues in the background through
`MusicService` regardless of whether the activity is visible.

## Suggestions for what's next

A few things worth considering as you keep building this out, roughly in the order
I'd tackle them:

1. **Playlists** — the single biggest feature gap for a "basic functionality"
   player. A small Room database (`playlists`, `playlist_songs` tables) plus a
   `PlaylistRepository` would slot in cleanly; `SongListItem` and
   `FilteredSongsScreen` are already reusable as-is for showing playlist contents.
2. **Folder / "all tracks" view with sort options** — you scoped this out
   initially, but it's a small addition once playlists exist: one more
   `MediaStore.Audio.Media.RELATIVE_PATH` projection column, following the same
   "derive, don't cache" pattern the Albums/Artists tabs already use.
3. **Widen lyrics support** — right now only ID3v2 `USLT` (MP3) is read. FLAC
   (Vorbis comment `LYRICS` tag) and M4A/AAC (`\xa9lyr` atom) are common enough
   in real libraries that it's worth adding both; `LyricsReader` is structured so
   each format is its own small parsing function, so this is additive, not a
   rewrite.
4. **Synced (word/line-timed) lyrics** — some files carry a synced `SYLT` ID3
   frame instead of (or alongside) `USLT`. If you want the karaoke-style
   highlight-as-it-plays effect Gramophone has, that's the frame to parse next,
   and `PlaybackUiState.positionMs` (already exposed) is exactly what you'd
   diff against to know which line to highlight.
5. **Home-screen widget** — a small Glance widget module reading
   `PlaybackUiState` (via a lightweight broadcast or content-provider bridge to
   `MusicService`) for a "now playing" widget with play/pause/skip.
6. **Equalizer** — Android's system `AudioEffect`/`Equalizer` APIs hook directly
   into ExoPlayer's audio session ID, which `PlayerController` already has
   access to (`player.audioSessionId`) — a self-contained addition, no new
   architecture needed.
7. **Sleep timer** — a genuinely "basic" feature many players have that this one
   doesn't yet; a simple countdown in `PlayerController` calling `pause()` on
   expiry, surfaced as a button on the Player screen.

Happy to build out any of these next — just say which.
