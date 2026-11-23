package com.example.demo.chat;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/store/chat")
public class StorefrontChatController {
    private final StorefrontChatService chat;
    public StorefrontChatController(StorefrontChatService chat) { this.chat = chat; }

    @GetMapping("/status")
    public ChatDtos.Status status() { return chat.status(); }

    @PostMapping("/messages")
    public ChatDtos.Response reply(@Valid @RequestBody ChatDtos.Request request, HttpServletRequest http) {
        // Never trust a client-supplied role, user ID, or X-Forwarded-For for authorization/rate limiting.
        return chat.reply(http.getRemoteAddr(), request);
    }
}
