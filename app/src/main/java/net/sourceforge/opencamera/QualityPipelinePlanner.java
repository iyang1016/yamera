package net.sourceforge.opencamera;

import net.sourceforge.opencamera.cameracontroller.CameraController;

/**
 * Chooses the capture pipeline that backs the high-level quality profile when
 * the user leaves photo mode on standard.
 */
public class QualityPipelinePlanner {
    public static final String PROFILE_AUTO = "preference_quality_profile_auto";
    public static final String PROFILE_MAX_DETAIL = "preference_quality_profile_max_detail";
    public static final String PROFILE_LOW_LIGHT = "preference_quality_profile_low_light";
    public static final String PROFILE_FAST = "preference_quality_profile_fast";

    public static final String NR_MODE_NORMAL = "preference_nr_mode_normal";
    public static final String NR_MODE_LOW_LIGHT = "preference_nr_mode_low_light";

    public static class Capabilities {
        public final boolean is_video;
        public final boolean supports_dro;
        public final boolean supports_hdr;
        public final boolean supports_noise_reduction;
        public final boolean supports_extension_auto;
        public final boolean supports_extension_night;
        public final boolean raw_only;
        public final boolean has_low_light_scene_hint;
        public final boolean low_light_scene;

        public Capabilities(boolean is_video,
                            boolean supports_dro,
                            boolean supports_hdr,
                            boolean supports_noise_reduction,
                            boolean supports_extension_auto,
                            boolean supports_extension_night,
                            boolean raw_only) {
            this(is_video, supports_dro, supports_hdr, supports_noise_reduction, supports_extension_auto, supports_extension_night, raw_only, false, false);
        }

        public Capabilities(boolean is_video,
                            boolean supports_dro,
                            boolean supports_hdr,
                            boolean supports_noise_reduction,
                            boolean supports_extension_auto,
                            boolean supports_extension_night,
                            boolean raw_only,
                            boolean has_low_light_scene_hint,
                            boolean low_light_scene) {
            this.is_video = is_video;
            this.supports_dro = supports_dro;
            this.supports_hdr = supports_hdr;
            this.supports_noise_reduction = supports_noise_reduction;
            this.supports_extension_auto = supports_extension_auto;
            this.supports_extension_night = supports_extension_night;
            this.raw_only = raw_only;
            this.has_low_light_scene_hint = has_low_light_scene_hint;
            this.low_light_scene = low_light_scene;
        }
    }

    public static class Plan {
        public final MyApplicationInterface.PhotoMode photo_mode;
        public final String nr_mode;
        public final boolean slow_capture_warning;
        public final String reason;

        private Plan(MyApplicationInterface.PhotoMode photo_mode,
                     String nr_mode,
                     boolean slow_capture_warning,
                     String reason) {
            this.photo_mode = photo_mode;
            this.nr_mode = nr_mode;
            this.slow_capture_warning = slow_capture_warning;
            this.reason = reason;
        }
    }

    public static class HDRProcessingSettings {
        public final HDRProcessor.TonemappingAlgorithm tonemapping_algorithm;
        public final String contrast_enhancement;
        public final String reason;

        private HDRProcessingSettings(HDRProcessor.TonemappingAlgorithm tonemapping_algorithm,
                                      String contrast_enhancement,
                                      String reason) {
            this.tonemapping_algorithm = tonemapping_algorithm;
            this.contrast_enhancement = contrast_enhancement;
            this.reason = reason;
        }
    }

    public static Plan planForStandardMode(String quality_profile, Capabilities capabilities) {
        if( capabilities.is_video ) {
            return standardPlan("video_standard");
        }
        if( capabilities.raw_only ) {
            return standardPlan("raw_only_standard");
        }
        if( PROFILE_FAST.equals(quality_profile) ) {
            return standardPlan("fast_profile");
        }
        else if( PROFILE_LOW_LIGHT.equals(quality_profile) ) {
            return lowLightPlan(capabilities);
        }
        else if( PROFILE_MAX_DETAIL.equals(quality_profile) ) {
            return maxDetailPlan(capabilities);
        }
        return autoPlan(capabilities);
    }

    public static Plan planForExplicitMode(MyApplicationInterface.PhotoMode photo_mode, String nr_mode) {
        String planned_nr_mode = photo_mode == MyApplicationInterface.PhotoMode.NoiseReduction ? nr_mode : NR_MODE_NORMAL;
        if( planned_nr_mode == null ) {
            planned_nr_mode = NR_MODE_NORMAL;
        }
        return new Plan(photo_mode, planned_nr_mode, isSlowCaptureMode(photo_mode), "explicit_photo_mode");
    }

    public static String getPlanBadgeText(String quality_profile, Plan plan) {
        return getProfileBadgePrefix(quality_profile) + " " + getPhotoModeBadgeLabel(plan.photo_mode);
    }

    public static String getPlanBadgeText(String quality_profile, Plan plan, boolean ultra_hdr) {
        if( ultra_hdr ) {
            return getProfileBadgePrefix(quality_profile) + " UHDR";
        }
        return getPlanBadgeText(quality_profile, plan);
    }

    public static boolean shouldUseUltraHDR(String image_format_pref,
                                            MyApplicationInterface.PhotoMode photo_mode,
                                            boolean is_video,
                                            boolean using_camera_extension) {
        if( !"preference_image_format_jpeg_r".equals(image_format_pref) || is_video || using_camera_extension ) {
            return false;
        }
        switch( photo_mode ) {
            case DRO:
            case HDR:
            case NoiseReduction:
            case Panorama:
            case X_Auto:
            case X_HDR:
            case X_Night:
            case X_Bokeh:
            case X_Beauty:
                return false;
            case Standard:
            case ExpoBracketing:
            case FocusBracketing:
            case FastBurst:
            default:
                return true;
        }
    }

    public static String getProfileBadgePrefix(String quality_profile) {
        if( PROFILE_MAX_DETAIL.equals(quality_profile) ) {
            return "MAX";
        }
        else if( PROFILE_LOW_LIGHT.equals(quality_profile) ) {
            return "LOW";
        }
        else if( PROFILE_FAST.equals(quality_profile) ) {
            return "FAST";
        }
        return "AUTO";
    }

    public static String getPhotoModeBadgeLabel(MyApplicationInterface.PhotoMode photo_mode) {
        switch( photo_mode ) {
            case DRO:
                return "DRO";
            case HDR:
            case X_HDR:
                return "HDR";
            case NoiseReduction:
                return "NR";
            case X_Auto:
                return "X-AUTO";
            case X_Night:
                return "NIGHT";
            case X_Bokeh:
                return "BOKEH";
            case X_Beauty:
                return "BEAUTY";
            case FastBurst:
                return "BURST";
            case ExpoBracketing:
                return "EXPO";
            case FocusBracketing:
                return "FOCUS";
            case Panorama:
                return "PANO";
            case Standard:
            default:
                return "STD";
        }
    }

    public static HDRProcessingSettings planHDRProcessing(String quality_profile,
                                                          String tonemapping_pref,
                                                          String contrast_pref,
                                                          int iso,
                                                          long exposure_time) {
        if( PROFILE_MAX_DETAIL.equals(quality_profile) ) {
            return new HDRProcessingSettings(
                    HDRProcessor.TonemappingAlgorithm.TONEMAPALGORITHM_ACES,
                    "preference_hdr_contrast_enhancement_always",
                    "max_detail_aces_contrast"
            );
        }
        else if( PROFILE_LOW_LIGHT.equals(quality_profile) ) {
            return new HDRProcessingSettings(
                    HDRProcessor.default_tonemapping_algorithm_c,
                    "preference_hdr_contrast_enhancement_off",
                    "low_light_noise_control"
            );
        }
        else if( PROFILE_FAST.equals(quality_profile) ) {
            return new HDRProcessingSettings(
                    HDRProcessor.TonemappingAlgorithm.TONEMAPALGORITHM_CLAMP,
                    "preference_hdr_contrast_enhancement_off",
                    "fast_minimal_processing"
            );
        }
        else if( HDRProcessor.sceneIsLowLight(iso, exposure_time) ) {
            return new HDRProcessingSettings(
                    HDRProcessor.default_tonemapping_algorithm_c,
                    "preference_hdr_contrast_enhancement_off",
                    "auto_low_light_noise_control"
            );
        }
        return new HDRProcessingSettings(
                getTonemappingAlgorithmForPreference(tonemapping_pref),
                normaliseHDRContrastPreference(contrast_pref),
                "user_processing_preferences"
        );
    }

    public static HDRProcessor.TonemappingAlgorithm getTonemappingAlgorithmForPreference(String tonemapping_pref) {
        if( "preference_hdr_tonemapping_clamp".equals(tonemapping_pref) ) {
            return HDRProcessor.TonemappingAlgorithm.TONEMAPALGORITHM_CLAMP;
        }
        else if( "preference_hdr_tonemapping_exponential".equals(tonemapping_pref) ) {
            return HDRProcessor.TonemappingAlgorithm.TONEMAPALGORITHM_EXPONENTIAL;
        }
        else if( "preference_hdr_tonemapping_aces".equals(tonemapping_pref) ) {
            return HDRProcessor.TonemappingAlgorithm.TONEMAPALGORITHM_ACES;
        }
        return HDRProcessor.default_tonemapping_algorithm_c;
    }

    public static String normaliseHDRContrastPreference(String contrast_pref) {
        if( "preference_hdr_contrast_enhancement_off".equals(contrast_pref) ||
                "preference_hdr_contrast_enhancement_always".equals(contrast_pref) ) {
            return contrast_pref;
        }
        return "preference_hdr_contrast_enhancement_smart";
    }

    public static int planNRBurstMaxImages(String quality_profile,
                                           boolean low_light_mode,
                                           int large_heap_memory_mb,
                                           int photo_megapixels) {
        if( PROFILE_FAST.equals(quality_profile) ) {
            return 4;
        }
        if( !low_light_mode ) {
            return CameraController.N_IMAGES_NR_DARK;
        }
        if( PROFILE_MAX_DETAIL.equals(quality_profile) ) {
            return CameraController.N_IMAGES_NR_DARK_LOW_LIGHT;
        }
        if( large_heap_memory_mb < 768 || photo_megapixels > 12000000 ) {
            return CameraController.N_IMAGES_NR_DARK;
        }
        return CameraController.N_IMAGES_NR_DARK_LOW_LIGHT;
    }

    public static boolean isSlowCaptureMode(MyApplicationInterface.PhotoMode photo_mode) {
        switch( photo_mode ) {
            case HDR:
            case ExpoBracketing:
            case FocusBracketing:
            case NoiseReduction:
            case Panorama:
            case X_HDR:
            case X_Night:
                return true;
            case DRO:
            case FastBurst:
            case Standard:
            case X_Auto:
            case X_Bokeh:
            case X_Beauty:
            default:
                return false;
        }
    }

    private static Plan autoPlan(Capabilities capabilities) {
        if( capabilities.has_low_light_scene_hint && capabilities.low_light_scene ) {
            return autoLowLightPlan(capabilities);
        }
        if( supportsPhotoExtension(capabilities.supports_extension_auto, capabilities) ) {
            return new Plan(MyApplicationInterface.PhotoMode.X_Auto, NR_MODE_NORMAL, false, "vendor_auto_extension");
        }
        return standardPlan("auto_standard");
    }

    private static Plan autoLowLightPlan(Capabilities capabilities) {
        if( supportsPhotoExtension(capabilities.supports_extension_night, capabilities) ) {
            return new Plan(MyApplicationInterface.PhotoMode.X_Night, NR_MODE_LOW_LIGHT, true, "auto_low_light_vendor_night_extension");
        }
        else if( capabilities.supports_noise_reduction ) {
            return new Plan(MyApplicationInterface.PhotoMode.NoiseReduction, NR_MODE_LOW_LIGHT, true, "auto_low_light_noise_reduction");
        }
        else if( capabilities.supports_dro ) {
            return new Plan(MyApplicationInterface.PhotoMode.DRO, NR_MODE_LOW_LIGHT, false, "auto_low_light_dro");
        }
        return standardPlan("auto_low_light_standard");
    }

    private static Plan maxDetailPlan(Capabilities capabilities) {
        if( supportsPhotoExtension(capabilities.supports_extension_auto, capabilities) ) {
            return new Plan(MyApplicationInterface.PhotoMode.X_Auto, NR_MODE_NORMAL, false, "max_detail_vendor_auto_extension");
        }
        else if( capabilities.supports_noise_reduction ) {
            return new Plan(MyApplicationInterface.PhotoMode.NoiseReduction, NR_MODE_NORMAL, true, "max_detail_noise_reduction");
        }
        else if( capabilities.supports_hdr ) {
            return new Plan(MyApplicationInterface.PhotoMode.HDR, NR_MODE_NORMAL, true, "max_detail_hdr");
        }
        else if( capabilities.supports_dro ) {
            return new Plan(MyApplicationInterface.PhotoMode.DRO, NR_MODE_NORMAL, false, "max_detail_dro");
        }
        return standardPlan("max_detail_standard");
    }

    private static Plan lowLightPlan(Capabilities capabilities) {
        if( supportsPhotoExtension(capabilities.supports_extension_night, capabilities) ) {
            return new Plan(MyApplicationInterface.PhotoMode.X_Night, NR_MODE_LOW_LIGHT, true, "low_light_vendor_night_extension");
        }
        else if( capabilities.supports_noise_reduction ) {
            return new Plan(MyApplicationInterface.PhotoMode.NoiseReduction, NR_MODE_LOW_LIGHT, true, "low_light_noise_reduction");
        }
        else if( capabilities.supports_dro ) {
            return new Plan(MyApplicationInterface.PhotoMode.DRO, NR_MODE_LOW_LIGHT, false, "low_light_dro");
        }
        return standardPlan("low_light_standard");
    }

    private static boolean supportsPhotoExtension(boolean extension_supported, Capabilities capabilities) {
        return !capabilities.is_video && extension_supported;
    }

    private static Plan standardPlan(String reason) {
        return new Plan(MyApplicationInterface.PhotoMode.Standard, NR_MODE_NORMAL, false, reason);
    }
}
