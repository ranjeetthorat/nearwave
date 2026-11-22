# Nearwave design system

## The brief

An offline walkie-talkie that people of every age can use on the first try, often in a cramped seat, sometimes in low light, sometimes without glasses. The screen has one job: make it obvious who is around and how to talk to them.

## Direction

Taken from handheld two-way radios. The body is calm: a cool haze background and a teal "channel" colour for everything connected. One element gets all the boldness: the round, signal-amber talk button, the "physical" button of the radio. Rose appears only for buzzes, so a buzz always looks different from anything else.

## Colour

| Token | Light | Dark | Used for |
|---|---|---|---|
| Haze (background) | `#F1F4F1` | `#0E1E1C` | Screen background |
| Ink (text) | `#12302B` | `#E2ECE8` | Text and icons |
| Channel teal | `#0D6B5F` | `#63C9B8` | Connected state, primary buttons, radar sweep, my messages |
| Signal amber | `#F4B400` | `#FFC52E` | The talk button, someone talking (avatar ring). Nothing else. |
| Buzz rose | `#C2304A` | `#FF8A9B` | Buzz button, incoming buzz banner |
| Ring | `#C7D3CD` | `#34524D` | Radar rings, dividers |

Avatars get one of six mid-tone fills, chosen from the person's id, so someone keeps the same colour on everyone's phone.

Defined in `ui/theme/Color.kt`. Material 3 roles are mapped in `ui/theme/Theme.kt`; colours Material has no slot for (talk, buzz, radar) live in `NwColors`, read with `NearwaveTheme.colors`.

## Type

The phone's own sans-serif, so text stays readable and respects the system font size. The scale starts larger than Material's default: body text is 18sp. Hierarchy comes from weight and size, never from all caps. Sentence case everywhere.

| Style | Size / line | Weight | Used for |
|---|---|---|---|
| displaySmall | 34 / 40 | Bold | First onboarding screen |
| headlineMedium | 28 / 34 | Bold | Screen titles |
| titleMedium | 18 / 24 | SemiBold | Banner titles, talk button label |
| bodyLarge | 18 / 26 | Regular | Main text, messages |
| labelLarge | 17 / 22 | SemiBold | Buttons |

The channel code is the only monospace text, at 30sp with wide tracking, because it is read aloud and copied by hand.

## Shape

Radius follows hierarchy: 10dp for small chips, 16dp for buttons, 24dp for banners and cards. Only the talk button and avatars are fully round.

## Spacing and touch

4dp grid (`ui/theme/Spacing.kt`). Screen margin 20dp. Touch targets are at least 56dp; the Buzz and Messages buttons are 64dp tall; the talk button is 168dp.

## Layout

```
Nearwave              [Channel K7QM-4XPA] (gear)
[ banner: problem, or incoming buzz / message ]

              . - ~ ~ ~ - .
          .'   Far          '.
        /       (R)           \
       |    Near     (A)       |      radar: fills the free space
       |       ( You )         |
        \           (P)       /
          '.               .'
              ' - ~ ~ ~ - '
        ● Connected to 3 people

   [ Buzz ]            [ Messages 2 ]

               (  🎙  )
             Hold to talk                big amber button
```

Content is left-aligned except the radar, status line and talk button, which sit on the centre line because they are the radio itself.

## Motion

- The radar sweep is the only ambient motion. It turns off when the system "Remove animations" setting is on.
- Everything else moves only in response to the person: the talk button sinks when pressed and sends out a ring while transmitting; avatars glide between rings when the distance changes.

## Words

Plain verbs, sentence case, no jargon. "Buzz", not "ping". "Channel", not "group" or "network". Errors say what happened and what to do ("Bluetooth is off. Turn it on to find people nearby."). Empty states tell people what to do next ("Looking for others. They join with code K7QM-4XPA.").

## Accessibility

- Respects system font size and dark mode.
- The radar has a spoken summary for TalkBack ("Ravi, near you; Asha, a few rows away").
- The talk button can be operated by TalkBack's double-tap, and "tap to talk" mode exists for anyone who can't hold a press.
- Colour is never the only signal: talking also shows in the status line, and buzzes carry an icon and words.
