# Yamera Image Processing Roadmap

## Goal

Improve still image quality while keeping capture predictable. There is no literal "highest quality without trade offs" path in mobile imaging: multi-frame HDR, night capture, denoise, RAW output, and Ultra HDR all trade speed, memory, storage, device support, or motion handling. Yamera should expose those choices through a clear quality profile instead of hiding the cost.

## Quality Profiles

1. **Auto Quality**
   - Default for most users.
   - Prefer Camera2 when supported.
   - Prefer vendor Camera Extensions Auto/HDR/Night when available.
   - Fall back to Open Camera's existing DRO/HDR/NoiseReduction modes.
   - Keep JPEG/WebP quality at 95 for normal saves.

2. **Max Detail**
   - Best for still scenes.
   - Enable RAW+JPEG where supported.
   - Prefer multi-frame HDR/NoiseReduction.
   - Use 100 quality for intermediate captures before post-processing.
   - Surface motion warnings when using slower capture modes.

3. **Low Light**
   - Prefer Camera Extension Night where available.
   - Fall back to Open Camera NoiseReduction.
   - Keep UI feedback explicit because night capture can take multiple seconds.

4. **Fast**
   - Preserve capture latency.
   - Avoid multi-frame modes unless the user selects them.
   - Keep continuous focus and preview responsiveness as the priority.

## Implementation Steps

1. Add a `preference_quality_profile` setting with Auto Quality, Max Detail, Low Light, and Fast.
2. In the popup mode selector, promote supported extension modes: Auto, HDR, Night, Bokeh, Beauty.
3. Add a small status pill in the viewfinder for active quality mode and slow-capture warnings.
4. For Android 14+ devices that support JPEG_R, make Ultra HDR an explicit option alongside standard JPEG.
5. Add telemetry-free debug logging for selected pipeline, capture count, processing time, and output format.
6. Add automated tests for profile preference mapping and supported-mode fallback decisions.

## Current Source Support

- Camera2 default is enabled when the device supports it.
- JPEG/WebP save quality defaults to 95.
- `preference_quality_profile` is available in Photo settings, with Auto Quality, Max Detail, Low Light, and Fast Capture profiles.
- Quality profiles currently adjust saved image quality and continuous-focus latency policy.
- Existing Open Camera photo modes already include HDR, DRO, NoiseReduction, and Camera2 extension modes where available.
- CameraController2 already probes JPEG_R support on Android 14+ devices.

## References

- Material Symbols: https://developers.google.com/fonts/docs/material_symbols
- Android Camera Extensions: https://developer.android.com/media/camera/camera-extensions
- Android Ultra HDR image format: https://developer.android.com/media/platform/hdr-image-format
