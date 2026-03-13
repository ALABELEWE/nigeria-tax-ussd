package com.taxhelp.nigerian_tax_ussd.util;


import org.apache.commons.lang3.StringUtils;

import java.text.Normalizer;
import java.util.regex.Pattern;

// Utility class for handling Yoruba text and diacrities in SMS
public class YorubaTextUtil {

    private static final Pattern YORUBA_DIACRITICS_PATTERN = Pattern.compile(
            "[ṣṢẹẸọỌòÒèÈàÀùÙìÌ]"
    );

    // Check if test conatins Yoruba diacritics that require Unicode SMS
    public static boolean containsYorubaDiacritics(String text) {
        if (StringUtils.isEmpty(text)) {
            return false;
        }
        return YORUBA_DIACRITICS_PATTERN.matcher(text).find();
    }

    // Remove Yoruba diacritics for standard SMS
    // Use this only if unicode SMS is not available or too expensive
    public static String removeDiacritics(String text) {
        if (StringUtils.isEmpty(text)) {
            return text;
        }

        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);

        String withoutDiacritics = normalized.replaceAll("\\p{M}", "");

        //Manual replacements for Yoruba-specific charaters not handled by normalization
        withoutDiacritics = withoutDiacritics
                .replace('ṣ', 's')
                .replace('Ṣ', 'S')
                .replace('ẹ', 'e')
                .replace('Ẹ', 'E')
                .replace('ọ', 'o')
                .replace('Ọ', 'O');
        return withoutDiacritics;
    }

    // Get estimated SMS cost multiplier based on encoding
    public static double getSmsCostMultiplier(boolean requiresUnicode){
        return requiresUnicode ? 2.0 : 1.0;
    }

    // Get character limit for SMS based on encoding
    public static int getSmsCharacterLimit(boolean requiresUnicode){
        return requiresUnicode ? 100 : 160;
    }


    // Calculate number of SMS segments needed
    public static int calculateSmsSegments(String text, boolean requiresUnicode){
        if (StringUtils.isEmpty(text)) {
            return 0;
        }

        int charLimit = getSmsCharacterLimit(requiresUnicode);
        int length = text.length();

        return (int) Math.ceil((double) length / charLimit);
    }


    // Get SMS encoding info for logging/debugging
    public static String getSmsEncodingInfo(String text){
        boolean requiresUnicode =  containsYorubaDiacritics(text);
        int segments = calculateSmsSegments(text, requiresUnicode);
        int characterLimit = getSmsCharacterLimit(requiresUnicode);
        double costMultiplier = getSmsCostMultiplier(requiresUnicode);

        return String.format(
                "Encoding: %s | Chars: %d/%d | Segments: %d | Cost: %.1fx",
                requiresUnicode ? "Unicode" : "Standard",
                text.length(),
                characterLimit,
                segments,
                costMultiplier
        );
    }

}
