package com.biolab.messaging.controller;

import com.biolab.messaging.dto.*;
import com.biolab.messaging.service.MessagingService;
import com.biolab.common.security.CurrentUserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/messages") @Tag(name = "Messaging")
public class MessagingController {

    private final MessagingService msgService;
    public MessagingController(MessagingService ms) { this.msgService = ms; }

    @GetMapping("/conversations")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Success"), @ApiResponse(responseCode = "401", description = "Unauthorized"), @ApiResponse(responseCode = "403", description = "Forbidden")})
    @Operation(summary = "List user's conversations")
    public ResponseEntity<List<ConversationDto>> conversations() {
        return ResponseEntity.ok(msgService.listConversations(CurrentUserContext.require().userId()));
    }

    @GetMapping("/conversations/{convId}")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Success"), @ApiResponse(responseCode = "404", description = "Not found")})
    @Operation(summary = "Get messages in a conversation")
    public ResponseEntity<Page<MessageDto>> messages(@PathVariable UUID convId, Pageable pageable) {
        return ResponseEntity.ok(msgService.listMessages(convId, pageable));
    }

    @PostMapping("/conversations/{convId}")
    @Operation(summary = "Send a message")
    public ResponseEntity<MessageDto> send(@PathVariable UUID convId, @Valid @RequestBody SendMessageRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(msgService.sendMessage(convId, CurrentUserContext.require().userId(), req));
    }

    @PostMapping("/conversations")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Created"), @ApiResponse(responseCode = "400", description = "Validation error")})
    @Operation(summary = "Create a conversation")
    public ResponseEntity<ConversationDto> create(@RequestParam(required = false) UUID projectId, @RequestParam String title) {
        var user = CurrentUserContext.require();
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(msgService.createConversation(projectId, title, user.userId(), UUID.fromString(user.orgId())));
    }

    @PostMapping("/conversations/{convId}/read")
    @Operation(summary = "Mark conversation as read")
    public ResponseEntity<Void> markRead(@PathVariable UUID convId) {
        msgService.markRead(convId, CurrentUserContext.require().userId());
        return ResponseEntity.noContent().build();
    }
}
