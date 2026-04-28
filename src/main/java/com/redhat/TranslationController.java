package com.redhat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

@RestController
public class TranslationController {

    @Value("${translation.default-language:EN}")
    private String defaultLanguage;

    @Value("${translation.file:translations.json}")
    private String translationFile;

    @GetMapping("/")
    public RedirectView redirectRoot() {
        return new RedirectView("/helloworld-by-language");
    }

    @GetMapping(value = "/helloworld-by-language", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getTranslation() {
        try {
            String countryCode = defaultLanguage.toUpperCase();

            InputStream is = getClass().getClassLoader().getResourceAsStream(translationFile);
            if (is == null) {
                return ResponseEntity.internalServerError()
                        .body("Could not find " + translationFile + " in resources.");
            }

            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> jsonObject = mapper.readValue(json, Map.class);

            if (jsonObject != null && jsonObject.containsKey("translations")) {
                @SuppressWarnings("unchecked")
                Map<String, String> translations = (Map<String, String>) jsonObject.get("translations");

                String translation = translations.getOrDefault(
                        countryCode,
                        translations.get("EN")
                );

                String timestamp = Instant.now().toString();
                return ResponseEntity.ok(translation + " @ " + timestamp);
            } else {
                return ResponseEntity.internalServerError()
                        .body("Invalid JSON format in " + translationFile);
            }

        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body("Error reading " + translationFile + ": " + e.getMessage());
        }
    }
}
