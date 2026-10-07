package dev.apexban.core.service;

import dev.apexban.core.model.Punishment;
import dev.apexban.core.model.PunishmentType;
import dev.apexban.core.model.Target;
import dev.apexban.core.storage.SqlStorage;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Punishment logic shared by all platforms. Database methods block; cache methods are thread-safe
 * and non-blocking so they can be used from chat and login handlers.
 */
public final class PunishmentService {
    private static final long BAN_CACHE_TTL_MILLIS = 5L * 60L * 1000L;

    private final SqlStorage storage;
    private final ConcurrentMap<UUID, Punishment> bans = new ConcurrentHashMap<UUID, Punishment>();
    private final ConcurrentMap<UUID, Punishment> mutes = new ConcurrentHashMap<UUID, Punishment>();

    public PunishmentService(SqlStorage storage) {
        this.storage = storage;
    }

    /**
     * Records the player, refreshes the caches from the database and returns the active ban, if any.
     */
    public Punishment onLogin(UUID uuid, String name) throws SQLException {
        long now = System.currentTimeMillis();
        storage.upsertPlayer(uuid, name, now);
        Punishment ban = storage.findActive(uuid, PunishmentType.BAN, now);
        if (ban != null) {
            bans.put(uuid, ban);
            return ban;
        }
        bans.remove(uuid);
        Punishment mute = storage.findActive(uuid, PunishmentType.MUTE, now);
        if (mute != null) {
            mutes.put(uuid, mute);
        } else {
            mutes.remove(uuid);
        }
        return null;
    }

    public void onQuit(UUID uuid) {
        mutes.remove(uuid);
    }

    public Punishment cachedBan(UUID uuid) {
        return live(bans, uuid);
    }

    public Punishment cachedMute(UUID uuid) {
        return live(mutes, uuid);
    }

    public void cacheBan(Punishment punishment) {
        bans.put(punishment.getUuid(), punishment);
    }

    public void cacheMute(Punishment punishment) {
        mutes.put(punishment.getUuid(), punishment);
    }

    private static Punishment live(ConcurrentMap<UUID, Punishment> map, UUID uuid) {
        Punishment punishment = map.get(uuid);
        if (punishment == null) {
            return null;
        }
        if (punishment.isExpired(System.currentTimeMillis())) {
            map.remove(uuid, punishment);
            return null;
        }
        return punishment;
    }

    public Punishment findActive(PunishmentType type, UUID uuid) throws SQLException {
        return storage.findActive(uuid, type, System.currentTimeMillis());
    }

    public Punishment punish(PunishmentType type, Target target, String operator, String reason,
                             long durationMillis, String serverId) throws SQLException {
        long now = System.currentTimeMillis();
        long expires = (type == PunishmentType.KICK || durationMillis <= 0L)
                ? Punishment.PERMANENT
                : now + durationMillis;
        Punishment draft = new Punishment(0L, type, target.getUuid(), target.getName(), operator, reason,
                now, expires, serverId);
        Punishment saved = storage.insert(draft);
        if (type == PunishmentType.BAN) {
            bans.put(saved.getUuid(), saved);
        }
        return saved;
    }

    /**
     * Lifts the active ban or mute. Returns false when nothing was active.
     */
    public boolean revoke(PunishmentType type, UUID uuid, String operator) throws SQLException {
        long now = System.currentTimeMillis();
        boolean wasActive = storage.findActive(uuid, type, now) != null;
        storage.deactivate(uuid, type, operator, now);
        if (type == PunishmentType.BAN) {
            bans.remove(uuid);
        } else if (type == PunishmentType.MUTE) {
            mutes.remove(uuid);
        }
        return wasActive;
    }

    /**
     * Resolves a player name to a UUID using the players table filled at login. Returns null when the
     * player has never joined and unknown players are not allowed.
     */
    public Target resolve(String name, boolean allowUnknown) throws SQLException {
        Target known = storage.findPlayerByName(name);
        if (known != null) {
            return known;
        }
        if (allowUnknown) {
            UUID offline = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
            return new Target(offline, name);
        }
        return null;
    }

    public void pruneBans(long now) {
        Iterator<Map.Entry<UUID, Punishment>> iterator = bans.entrySet().iterator();
        while (iterator.hasNext()) {
            Punishment punishment = iterator.next().getValue();
            if (punishment.isExpired(now) || now - punishment.getCreatedAt() > BAN_CACHE_TTL_MILLIS) {
                iterator.remove();
            }
        }
    }

    /**
     * Applies the result of a database poll for the given online players and returns the bans that
     * were issued elsewhere and must now be enforced here.
     */
    public List<Punishment> applySync(Collection<UUID> online, List<Punishment> active) {
        Set<UUID> muted = new HashSet<UUID>();
        List<Punishment> toEnforce = new ArrayList<Punishment>();
        for (Punishment punishment : active) {
            if (punishment.getType() == PunishmentType.BAN) {
                bans.put(punishment.getUuid(), punishment);
                toEnforce.add(punishment);
            } else if (punishment.getType() == PunishmentType.MUTE) {
                mutes.put(punishment.getUuid(), punishment);
                muted.add(punishment.getUuid());
            }
        }
        for (UUID uuid : online) {
            if (!muted.contains(uuid)) {
                mutes.remove(uuid);
            }
        }
        return toEnforce;
    }
}
