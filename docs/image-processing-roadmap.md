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
- Quality profiles adjust saved image quality and continuous-focus latency policy.
- When the saved photo mode is Standard, `QualityPipelinePlanner` maps the active quality profile to the best supported capture pipeline:
  - Auto Quality uses current Camera2 ISO/exposure metadata when available; dark scenes prefer Camera Extension Night, low-light NoiseReduction, DRO, and Standard, while normal scenes prefer Camera Extension Auto and Standard.
  - Max Detail prefers Camera Extension Auto, then NoiseReduction, HDR, DRO, and Standard.
  - Low Light prefers Camera Extension Night, then low-light NoiseReduction, DRO, and Standard.
  - Fast keeps Standard to preserve latency.
- RAW-only capture stays Standard so the profile planner does not silently disable RAW output.
- Video mode stays Standard so still-photo profile automation does not interfere with video setup.
- The viewfinder quality badge now displays both profile and active pipeline, such as `MAX NR`, `LOW NIGHT`, `FAST STD`, or `AUTO X-AUTO`.
- The mode switch and quality badge use compact glass-style capsule backgrounds to move the camera UI closer to LMC/GCam-style controls without changing the core workflow.
- Automatic slow profile choices show hold-steady capture feedback for modes such as HDR and normal NoiseReduction.
- Ultra HDR/JPEG_R selection is now routed through a tested planner method and is disabled for pipelines that post-process the JPEG or use camera extensions. When active, the quality badge shows `UHDR`.
- HDR/DRO processing settings are profile-aware:
  - Max Detail uses ACES tonemapping with always-on local contrast enhancement.
  - Low Light uses default tonemapping with local contrast enhancement off to reduce noise amplification.
  - Fast uses clamp tonemapping with local contrast enhancement off when a slow processing mode is manually selected.
  - Auto keeps user HDR processing settings unless the ISO/exposure scene hint indicates low light, then it follows the Low Light noise-control path.
- Debug builds log the selected quality profile, planner reason, Ultra HDR state, effective HDR/DRO processing reason, process type, image count, format, JPEG quality, Camera2/extension path, ISO, exposure time, and total save time.
- Existing Open Camera photo modes already include HDR, DRO, NoiseReduction, and Camera2 extension modes where available.
- CameraController2 already probes JPEG_R support on Android 14+ devices.
- Unit coverage checks profile image quality, focus policy, planner fallback order, scene-aware Auto behavior, RAW-only behavior, video behavior, badge labels, explicit mode metadata, Ultra HDR decisions, profile-aware HDR/DRO processing settings, and slow-capture flags.

## References

- Material Symbols: https://developers.google.com/fonts/docs/material_symbols
- Android Camera Extensions: https://developer.android.com/media/camera/camera-extensions
- Android Ultra HDR image format: https://developer.android.com/media/platform/hdr-image-format
