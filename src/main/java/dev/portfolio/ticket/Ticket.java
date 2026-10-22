package dev.portfolio.ticket;
import java.time.LocalDateTime;
public record Ticket(Long id, String title, String description, String category,
                     String status, int version, LocalDateTime createdAt) {}
