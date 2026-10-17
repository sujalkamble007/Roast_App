package dev.roastapp.share.service;

public interface CardRenderer {
    byte[] renderCard(String roastText, String tone, String sources);
}
