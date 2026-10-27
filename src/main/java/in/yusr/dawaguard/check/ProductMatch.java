package in.yusr.dawaguard.check;

import in.yusr.dawaguard.cdsco.FlaggedBatch;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Batch codes are not unique across manufacturers, so a batch hit only counts when the product matches too.
 * Compares the meaningful words (ingredients, brand) on the strip with the CDSCO record.
 */
final class ProductMatch {

    private static final Set<String> NOISE = Set.of(
            "tablet", "tablets", "capsule", "capsules", "syrup", "suspension", "injection", "oral", "solution",
            "gastro", "resistant", "film", "coated", "dispersible", "prolonged", "release", "sustained", "extended",
            "hydrochloride", "sodium", "potassium", "with", "and", "for", "pharma", "pharmaceuticals", "limited",
            "ltd", "pvt", "private", "the", "paediatric", "pediatric", "drops", "cream", "ointment", "gel");

    private ProductMatch() {
    }

    static boolean sameProduct(StripReading reading, FlaggedBatch batch) {
        Set<String> strip = words(reading.brandName() + " " + reading.genericName());
        if (strip.isEmpty()) {
            // Nothing to compare against (for example a typed-in batch number only): let the batch decide.
            return true;
        }
        Set<String> record = words(batch.getProductName() + " " + batch.getBrandName());
        return strip.stream().anyMatch(record::contains);
    }

    private static Set<String> words(String text) {
        if (text == null) {
            return Set.of();
        }
        return Arrays.stream(text.toLowerCase(Locale.ROOT).split("[^a-z]+"))
                .filter(w -> w.length() >= 4 && !NOISE.contains(w) && !w.equals("null"))
                .map(w -> w.replace("amoxycillin", "amoxicillin"))
                .collect(Collectors.toSet());
    }
}
