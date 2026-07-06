
## Flag Quiz Fabric Mod (26.1.2)

Client-side Fabric quiz mod for Lunar/Fabric profile `vanilla-qol`.

### Features

- 196 country flags (all UN member states + Palestine + Kosovo).
- `/flagquiz` toggle (also supports `/flagquiz on` and `/flagquiz off`).
- Shows one flag popup every timer interval.
- Tracks right/wrong globally and per-country.
- Prioritizes countries you miss more often, so missed flags appear more frequently.
- Runtime timer control command.

### Commands

- `/flagquiz` - Toggle quiz on/off.
- `/flagquiz on` - Enable quiz.
- `/flagquiz off` - Disable quiz.
- `/flagquiz timer` - Show current interval.
- `/flagquiz timer <seconds>` - Set interval (5-600 seconds).
- `/flagquiz stats` - Show overall right/wrong totals.

### Build

```powershell
.\gradlew.bat build
```

Output jar:

`build\libs\flag-quiz-fabric-1.0.0.jar`

### Lunar install path

`C:\Users\Leo\.lunarclient\profiles\vanilla-qol\mods`
