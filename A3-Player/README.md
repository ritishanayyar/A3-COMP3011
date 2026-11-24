# COMP3011 Assignment 3 – Video Player

## Overview

A JavaFX-based video player developed for COMP3011 Assignment 3. It supports video and audio playback, speech-to-text subtitles and multiple frame-processing effects.

## Technologies

Java, JavaFX, JavaCV, FFmpeg, Maven and JUnit.

## Running the Application

Build the project:

```bash
mvn clean package
```

Run the player:

```bash
mvn javafx:run
```

To play a video with audio and subtitles enabled:

```bash
mvn javafx:run -Djavafx.args="-S -a /path/to/video.mp4"
```

## Command-Line Options

- `-h`: Display help.
- `-a`: Enable audio.
- `-S`: Enable subtitles.
- `-x`: Open maximised.
- `-1` / `-2`: Select display.

## Frame-Processing Effects

The player supports frame numbers, film scratches, flickering, black and white, warmer colour temperature, vignette, dust, jitter, mottling, light bleed and pepper effects.

Multiple effects can be combined and are applied in command-line order.

## Subtitles

The player communicates with the subtitle server to obtain speech-to-text subtitles. Subtitles are displayed near the bottom of the video with a dark background for readability.

The subtitle server must be running for subtitle generation to work.

## Testing

Run automated tests using:

```bash
mvn clean test
```