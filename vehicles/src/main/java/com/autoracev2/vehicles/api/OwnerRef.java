package com.autoracev2.vehicles.api;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Stable owner identity for AutoRaceV2. Username is the mapping key.
 * Slot numbers and player-list order are never used as identity.
 */
public final class OwnerRef {
    private final String username;
    private final UUID playerUuid;

    public OwnerRef(String username, UUID playerUuid) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("owner username must not be blank");
        }
        this.username = username.trim();
        this.playerUuid = playerUuid;
    }

    public static OwnerRef named(String username) {
        return new OwnerRef(username, null);
    }

    public String username() {
        return username;
    }

    public Optional<UUID> playerUuid() {
        return Optional.ofNullable(playerUuid);
    }

    public OwnerRef withPlayerUuid(UUID uuid) {
        return new OwnerRef(username, uuid);
    }

    public boolean sameOwner(OwnerRef other) {
        return other != null && username.equals(other.username);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof OwnerRef other)) {
            return false;
        }
        return username.equals(other.username) && Objects.equals(playerUuid, other.playerUuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, playerUuid);
    }

    @Override
    public String toString() {
        return playerUuid == null ? username : username + "/" + playerUuid;
    }
}
