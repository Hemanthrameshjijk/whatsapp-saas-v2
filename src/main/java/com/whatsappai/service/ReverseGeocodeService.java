package com.whatsappai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReverseGeocodeService {

    @Value("${geocode.provider:nominatim}")
    private String provider;

    @Value("${geocode.nominatim-url:https://nominatim.openstreetmap.org}")
    private String nominatimUrl;

    @Value("${geocode.google-maps-api-key:}")
    private String googleKey;

    private final RestTemplate restTemplate;

    public String reverseGeocode(double lat, double lng) {
        try {
            return switch (provider) {
                case "google" -> reverseGeocodeGoogle(lat, lng);
                default -> reverseGeocodeNominatim(lat, lng);
            };
        } catch (Exception e) {
            log.warn("Geocode failed for ({}, {}): {}", lat, lng, e.getMessage());
            return formatFallback(lat, lng);
        }
    }

    private String reverseGeocodeNominatim(double lat, double lng) {
        String url = nominatimUrl + "/reverse?lat={lat}&lon={lng}&format=json";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "WhatsAppAI-SaaS/1.0.0 (contact: admin@whatsappstore.com)");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        var response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class, Map.of("lat", lat, "lng", lng)).getBody();
        if (response != null && response.containsKey("display_name")) {
            return (String) response.get("display_name");
        }
        return formatFallback(lat, lng);
    }

    private String reverseGeocodeGoogle(double lat, double lng) {
        if (googleKey == null || googleKey.isBlank()) return formatFallback(lat, lng);
        String url = "https://maps.googleapis.com/maps/api/geocode/json?latlng={lat},{lng}&key={key}";
        Map<?, ?> response = restTemplate.getForObject(url, Map.class,
            Map.of("lat", lat, "lng", lng, "key", googleKey));
        if (response != null) {
            Object results = response.get("results");
            if (results instanceof java.util.List<?> list && !list.isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> first = (Map<String, Object>) list.get(0);
                return first.getOrDefault("formatted_address", formatFallback(lat, lng)).toString();
            }
        }
        return formatFallback(lat, lng);
    }

    private String formatFallback(double lat, double lng) {
        return String.format("Location: %.6f, %.6f", lat, lng);
    }
}
