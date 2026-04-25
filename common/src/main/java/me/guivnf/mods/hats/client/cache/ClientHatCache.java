package me.guivnf.mods.hats.client.cache;

import me.guivnf.mods.hats.HatsMod;
import me.guivnf.mods.hats.common.hat.HatDefinition;
import me.guivnf.mods.hats.common.hat.HatMeta;
import me.guivnf.mods.hats.common.hat.HatModelData;
import me.guivnf.mods.hats.common.hat.HatPack;
import me.guivnf.mods.hats.common.hat.HatPart;
import me.guivnf.mods.hats.common.hat.HatRegistry;
import me.guivnf.mods.hats.common.network.HatsNetwork;
import me.guivnf.mods.hats.common.network.packet.PacketRequestHatData;
import me.guivnf.mods.hats.common.network.packet.PacketHatManifest.ManifestEntry;
import net.minecraft.client.Minecraft;

import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ClientHatCache
{
    private static final Map<UUID, HatPart> ENTITY_HATS = new ConcurrentHashMap<>();

    private static UUID localPlayerUuid;
    private static int localTokens = 0;
    @Nullable
    private static HatPart localEquipped;
    private static final List<HatPart> LOCAL_INVENTORY = new ArrayList<>();

    private static final Map<String, byte[][]> FRAGMENT_BUFFERS = new ConcurrentHashMap<>();
    private static final Map<String, Integer> FRAGMENT_COUNTS = new ConcurrentHashMap<>();
    private static final Map<String, Integer> RECEIVED_FRAGMENTS = new ConcurrentHashMap<>();
    private static final Map<String, HatModelData> PENDING_MODELS = new ConcurrentHashMap<>();
    private static final Map<String, HatMeta> PENDING_METAS = new ConcurrentHashMap<>();
    private static final Map<String, String> PENDING_PACK_IDS = new ConcurrentHashMap<>();

    public static void setEntityHat(UUID entityUuid, @Nullable HatPart hat)
    {
        if (hat == null) {
            ENTITY_HATS.remove(entityUuid);
        } else {
            ENTITY_HATS.put(entityUuid, hat);
        }
    }

    @Nullable
    public static HatPart getEntityHat(UUID entityUuid)
    {
        return ENTITY_HATS.get(entityUuid);
    }

    public static void setPlayerInventory(UUID owner, int tokens, @Nullable HatPart equipped, List<HatPart> inventory)
    {
        localPlayerUuid = owner;
        localTokens = tokens;
        localEquipped = equipped;
        LOCAL_INVENTORY.clear();
        LOCAL_INVENTORY.addAll(inventory);
    }

    public static List<HatPart> getLocalInventory()
    {
        return Collections.unmodifiableList(LOCAL_INVENTORY);
    }

    public static int getLocalTokens()
    {
        return localTokens;
    }

    @Nullable
    public static HatPart getLocalEquipped()
    {
        return localEquipped;
    }

    public static void processManifest(Map<String, ManifestEntry> entries)
    {
        List<String> toRequest = new ArrayList<>();
        for (ManifestEntry entry : entries.values()) {
            HatDefinition local = HatRegistry.get(entry.name());
            if (local == null || !Arrays.equals(hashTexture(local.textureBytes), entry.hash())) {
                toRequest.add(entry.name());
            }
        }

        if (!toRequest.isEmpty()) {
            dev.architectury.networking.NetworkManager.sendToServer(
                HatsNetwork.REQUEST_HAT_DATA,
                PacketRequestHatData.encode(toRequest)
            );
        }
    }

    public static void receiveFragment(String hatName, String packId, int fragmentIndex, int totalFragments,
            byte[] chunk, @Nullable HatModelData model, @Nullable HatMeta meta)
    {
        FRAGMENT_BUFFERS.computeIfAbsent(hatName, k -> new byte[totalFragments][]);
        FRAGMENT_COUNTS.put(hatName, totalFragments);
        FRAGMENT_BUFFERS.get(hatName)[fragmentIndex] = chunk;
        RECEIVED_FRAGMENTS.merge(hatName, 1, Integer::sum);

        if (model != null) PENDING_MODELS.put(hatName, model);
        if (meta != null) PENDING_METAS.put(hatName, meta);
        PENDING_PACK_IDS.put(hatName, packId);

        if (RECEIVED_FRAGMENTS.getOrDefault(hatName, 0).equals(totalFragments)) {
            assembleHat(hatName);
        }
    }

    private static void assembleHat(String hatName)
    {
        byte[][] chunks = FRAGMENT_BUFFERS.remove(hatName);
        FRAGMENT_COUNTS.remove(hatName);
        RECEIVED_FRAGMENTS.remove(hatName);

        HatModelData model = PENDING_MODELS.remove(hatName);
        HatMeta meta = PENDING_METAS.remove(hatName);
        String packId = PENDING_PACK_IDS.remove(hatName);

        if (model == null) {
            HatsMod.LOGGER.warn("Assembled hat '{}' with no model data — skipping", hatName);
            return;
        }

        int totalBytes = 0;
        for (byte[] chunk : chunks) {
            if (chunk != null) totalBytes += chunk.length;
        }

        byte[] textureBytes = new byte[totalBytes];
        int pos = 0;
        for (byte[] chunk : chunks) {
            if (chunk == null) continue;
            System.arraycopy(chunk, 0, textureBytes, pos, chunk.length);
            pos += chunk.length;
        }

        HatPack pack = new HatPack(packId, packId, "", "1.0", "server");
        HatDefinition def = new HatDefinition(hatName, model, textureBytes, meta != null ? meta : new HatMeta(), pack);

        HatDefinition existing = HatRegistry.get(hatName);
        if (existing != null) {
            for (me.guivnf.mods.hats.common.hat.HatDefinition acc : existing.getAccessories()) {
                def.addAccessory(acc);
            }
        }

        HatRegistry.register(def);

        HatsMod.LOGGER.debug("Received and registered server hat '{}'", hatName);
    }

    public static void clear()
    {
        ENTITY_HATS.clear();
        LOCAL_INVENTORY.clear();
        localEquipped = null;
        localTokens = 0;
        FRAGMENT_BUFFERS.clear();
        FRAGMENT_COUNTS.clear();
        RECEIVED_FRAGMENTS.clear();
        PENDING_MODELS.clear();
        PENDING_METAS.clear();
        PENDING_PACK_IDS.clear();
    }

    private static byte[] hashTexture(byte[] data)
    {
        if (data == null) return new byte[0];
        try {
            return java.security.MessageDigest.getInstance("SHA-256").digest(data);
        } catch (Exception e) {
            return new byte[0];
        }
    }
}
