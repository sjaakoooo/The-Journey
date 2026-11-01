# The Journey

A Fabric mod (Minecraft 1.21.1) that adds a cinematic "Year in Review" recap to the End Credits
and to a stand-alone screen (default key: J).

## Recap experience

* **End Credits**: a small arrow tab on the left opens the recap menu. The credits stay readable
  (they glide to the free side on wide screens; on narrow screens the menu covers them, dimmed).
  The credits wait at their end while the menu is open.
* **Pages**: Highlights, Stats, Photos, Achievements, Playtime, Settings (gear icon) and a final screen.
* **Export**: "Export image" saves a clean poster to the `screenshots` folder.
* **Themes / animations / audio**: all in the in-recap settings and in the Cloth Config screen
  ("Recap experience"). Everything is saved in `config/the-journey.json`.

## What is real data

* Statistics: Minecraft's own player statistics.
* Moments (first diamond, Nether, End, dragon) and achievements: dates from the player's advancement progress.
* "Longest session": measured by the mod itself since it was installed (`config/the-journey-sessions.json`).
* Photos: taken at milestones while playing. Nothing is invented; sections with no data are hidden or say so.

## Video

Minecraft keeps no gameplay footage and real video needs an encoder (extra dependency), so there is no video.
Instead, with "Short moment clips" on, about 3 seconds of frames (4 per second) are recorded right *after*
a first death, Wither or Ender Dragon kill, and played back as a short flip-book. Clips only exist if they
were recorded while playing, and are kept or deleted together with the other photos (see below).

## Setup

See the [Fabric Documentation](https://docs.fabricmc.net/develop/getting-started/creating-a-project#setting-up).

## License

This template is available under the CC0 license.

## Photos

Milestone photos and moment clips stay on this computer (`the-journey-temp/<world>`) so you can look at them
again with the J key. Turn off "Keep photos after the credits" and they are deleted when the credits close.
"Delete saved photos now" in the recap settings removes them at any time. The many small background
screenshots taken in the End are always deleted after the credits.
