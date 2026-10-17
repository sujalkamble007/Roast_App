package dev.roastapp.share.service;

public interface R2StorageService {
    boolean uploadCard(String fileName, byte[] imageBytes);
    boolean isConfigured();
}
