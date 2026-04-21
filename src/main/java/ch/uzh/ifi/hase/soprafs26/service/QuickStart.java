package ch.uzh.ifi.hase.soprafs26.service;

import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Feature.Type;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.protobuf.ByteString;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class QuickStart {
    private static final Pattern LEADING_LIST_MARKER =
            Pattern.compile("^(?:[-*\\u2022]|\\d+[.)]|\\[\\s*[xX ]\\s*\\])\\s+");
    private static final Pattern STARTS_WITH_QUANTITY =
            Pattern.compile("^\\d+(?:[.,]\\d+)?\\s*(?:x|kg|g|mg|l|ml|pcs?|pack|packs|bottle|bottles)?\\b.*",
                    Pattern.CASE_INSENSITIVE);

    public static void main(String... args) throws Exception {
        try (ImageAnnotatorClient vision = ImageAnnotatorClient.create()) {

            String fileName = "src/main/resources/shopping-list_testimage2.webp";

            Path path = Paths.get(fileName);
            byte[] data = Files.readAllBytes(path);
            ByteString imgBytes = ByteString.copyFrom(data);

            List<AnnotateImageRequest> requests = new ArrayList<>();
            Image img = Image.newBuilder().setContent(imgBytes).build();
            Feature feat = Feature.newBuilder().setType(Type.TEXT_DETECTION).build();
            AnnotateImageRequest request =
                    AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
            requests.add(request);

            BatchAnnotateImagesResponse response = vision.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();

            System.out.format("Text detection result for %s%n", fileName);
            for (AnnotateImageResponse res : responses) {
                if (res.hasError()) {
                    System.out.format("Error: %s%n", res.getError().getMessage());
                    return;
                }

                List<EntityAnnotation> texts = res.getTextAnnotationsList();
                if (texts.isEmpty()) {
                    System.out.println("No text found.");
                    continue;
                }

                String detectedText = texts.get(0).getDescription();
                System.out.println(detectedText);

                List<String> shoppingItems = extractShoppingListItems(detectedText);
                if (!shoppingItems.isEmpty()) {
                    System.out.println("Detected shopping list:");
                    for (String item : shoppingItems) {
                        System.out.format("- %s%n", item);
                    }
                } else {
                    System.out.println("No shopping list detected.");
                }
            }
        }
    }

    static List<String> extractShoppingListItems(String ocrText) {
        String[] rawLines = ocrText.split("\\R");
        List<String> cleanedLines = new ArrayList<>();

        for (String rawLine : rawLines) {
            String normalized = normalizeLine(rawLine);
            if (!normalized.isBlank()) {
                cleanedLines.add(normalized);
            }
        }

        if (!looksLikeShoppingList(cleanedLines)) {
            return new ArrayList<>();
        }

        List<String> items = new ArrayList<>();
        for (String line : cleanedLines) {
            if (isListItemCandidate(line)) {
                items.add(stripListMarker(line));
            }
        }

        return items;
    }

    private static boolean looksLikeShoppingList(List<String> lines) {
        if (lines.size() < 2) {
            return false;
        }

        int likelyItems = 0;
        for (String line : lines) {
            if (isListItemCandidate(line)) {
                likelyItems++;
            }
        }

        return likelyItems >= 2;
    }

    private static boolean isListItemCandidate(String line) {
        if (line.length() > 45) {
            return false;
        }

        if (LEADING_LIST_MARKER.matcher(line).find() || STARTS_WITH_QUANTITY.matcher(line).find()) {
            return true;
        }

        String normalized = line.toLowerCase(Locale.ROOT);
        if (normalized.matches("^[a-zA-Z][a-zA-Z\\s-]{1,35}$")) {
            // Short alphabetic lines are often plain item names in handwritten/printed lists.
            return !normalized.contains("total") && !normalized.contains("receipt");
        }

        return false;
    }

    private static String normalizeLine(String line) {
        return line.replaceAll("\\s+", " ").trim();
    }

    private static String stripListMarker(String line) {
        String cleaned = LEADING_LIST_MARKER.matcher(line).replaceFirst("").trim();
        return cleaned.replaceAll("\\s+", " ");
    }
}
