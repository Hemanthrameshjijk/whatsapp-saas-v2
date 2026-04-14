package com.whatsappai.controller;

import com.whatsappai.entity.CampaignClick;
import com.whatsappai.entity.CampaignLink;
import com.whatsappai.repository.CampaignClickRepository;
import com.whatsappai.repository.CampaignLinkRepository;
import com.whatsappai.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Optional;

@RestController
@RequestMapping("/l")
@Slf4j
@RequiredArgsConstructor
public class LinkRedirectController {

    private final CampaignLinkRepository linkRepository;
    private final CampaignClickRepository clickRepository;
    private final NotificationService notificationService;

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode, 
                                          @RequestParam(required = false) String p) {
        log.info("Tracking click for code: {}", shortCode);
        
        Optional<CampaignLink> linkOpt = linkRepository.findByShortCode(shortCode);
        if (linkOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        CampaignLink link = linkOpt.get();
        
        // Log the click
        CampaignClick click = CampaignClick.builder()
            .linkId(link.getId())
            .customerPhone(p) // Optional phone param for better tracking
            .build();
        clickRepository.save(click);

        // Update total click count
        link.setClickCount(link.getClickCount() + 1);
        linkRepository.save(link);

        // Notify Dashboard in real-time
        if (link.getBusinessId() != null) {
            String source = link.getInfluencerName() != null ? link.getInfluencerName() : "Broadcast Link";
            notificationService.pushSseMarketingEvent(
                link.getBusinessId(), 
                "Link", 
                "Link Clicked!", 
                source + " just generated a click."
            );
        }

        log.debug("Redirecting to: {}", link.getOriginalUrl());
        return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create(link.getOriginalUrl()))
            .build();
    }
}
