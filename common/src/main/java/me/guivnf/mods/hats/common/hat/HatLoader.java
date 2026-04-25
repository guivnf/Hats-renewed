package me.guivnf.mods.hats.common.hat;

import me.guivnf.mods.hats.HatsMod;
import me.guivnf.mods.hats.common.hat.reader.GeoJsonReader;
import me.guivnf.mods.hats.common.hat.reader.TblReader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

public class HatLoader
{
    private static Thread loaderThread;
    private static CountDownLatch latch;

    public static void beginLoading()
    {
        latch = new CountDownLatch(1);
        loaderThread = new Thread(HatLoader::load, "HatsLoader");
        loaderThread.setDaemon(true);
        loaderThread.start();
    }

    public static void awaitAndFinish()
    {
        if (latch == null) return;

        if (latch.getCount() > 0) {
            HatsMod.LOGGER.info("Waiting for hat loader thread...");
            try {
                latch.await();
            } catch (InterruptedException e) {
                HatsMod.LOGGER.error("Interrupted while waiting for hat loader: {}", e.getMessage());
                Thread.currentThread().interrupt();
            }
        }

        loaderThread = null;
        latch = null;
    }

    private static void load()
    {
        try {
            HatRegistry.clear();

            Path hatsDir = HatsMod.hatsDirectory;
            ensureDirectory(hatsDir);

            List<HatDefinition> allHats = new ArrayList<>();
            List<HatDefinition> accessories = new ArrayList<>();

            loadBuiltinHats(allHats, accessories);
            loadDirectory(hatsDir, allHats, accessories);

            allHats     = deduplicate(allHats);
            accessories = deduplicate(accessories);

            for (HatDefinition hat : allHats) {
                hat.attachAccessories(accessories);
                HatRegistry.register(hat);
                registerAccessoriesRecursively(hat);
            }

            for (HatDefinition leftover : accessories) {
                HatsMod.LOGGER.warn("Accessory '{}' could not find its parent '{}'", leftover.name, leftover.meta.accessoryFor);
            }
        } catch (Exception e) {
            HatsMod.LOGGER.error("Hat loading failed: {}", e.getMessage(), e);
        } finally {
            latch.countDown();
        }
    }

    private static final String BUILTIN_HATS_PATH = "/assets/hats/legacy_tabula/hats";
    private static final String NEW_HATS_PATH     = "/assets/hats/new_hats";

    private static void loadBuiltinHats(List<HatDefinition> hats, List<HatDefinition> accessories)
    {
        loadBuiltinPath(BUILTIN_HATS_PATH, hats, accessories);
        loadBuiltinPath(NEW_HATS_PATH, hats, accessories);
    }

    private static void loadBuiltinPath(String resourcePath, List<HatDefinition> hats, List<HatDefinition> accessories)
    {
        URL url = HatLoader.class.getResource(resourcePath);
        if (url == null) return;

        try {
            URI uri = url.toURI();
            String scheme = uri.getScheme();

            if ("file".equals(scheme)) {
                loadPack(HatPack.BUILTIN, Path.of(uri), hats, accessories);
                return;
            }

            if ("jar".equals(scheme)) {
                String uriStr = uri.toString();
                int sep = uriStr.indexOf("!/");
                URI jarUri = URI.create(uriStr.substring(0, sep));
                String innerPath = uriStr.substring(sep + 1);

                FileSystem fs;
                try {
                    fs = FileSystems.getFileSystem(jarUri);
                } catch (FileSystemNotFoundException e) {
                    fs = FileSystems.newFileSystem(jarUri, Collections.emptyMap());
                }
                loadPack(HatPack.BUILTIN, fs.getPath(innerPath), hats, accessories);
                return;
            }

            try {
                FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap());
                loadPack(HatPack.BUILTIN, fs.getPath(resourcePath), hats, accessories);
                return;
            } catch (Exception ignored) {}

            loadBuiltinPathFromCodeSource(resourcePath, hats, accessories);

        } catch (Exception e) {
            HatsMod.LOGGER.error("Failed to load builtin hats from '{}': {}", resourcePath, e.getMessage(), e);
        }
    }

    private static void loadBuiltinPathFromCodeSource(String resourcePath,
            List<HatDefinition> hats, List<HatDefinition> accessories)
    {
        try {
            java.security.CodeSource cs = HatLoader.class.getProtectionDomain().getCodeSource();
            if (cs == null) {
                HatsMod.LOGGER.warn("Cannot locate code source for builtin hat loading");
                return;
            }
            URI srcUri = cs.getLocation().toURI();
            Path srcPath = Path.of(srcUri);

            if (Files.isDirectory(srcPath)) {
                Path hatsPath = srcPath.resolve(resourcePath.substring(1));
                if (Files.exists(hatsPath)) {
                    loadPack(HatPack.BUILTIN, hatsPath, hats, accessories);
                }
                return;
            }

            try (FileSystem fs = FileSystems.newFileSystem(srcPath, (ClassLoader) null)) {
                Path hatsPath = fs.getPath(resourcePath);
                if (Files.exists(hatsPath)) {
                    loadPack(HatPack.BUILTIN, hatsPath, hats, accessories);
                }
            }
        } catch (Exception e) {
            HatsMod.LOGGER.error("Code-source fallback failed for '{}': {}", resourcePath, e.getMessage(), e);
        }
    }

    private static void loadDirectory(Path root, List<HatDefinition> hats, List<HatDefinition> accessories)
            throws IOException
    {
        if (!Files.exists(root)) return;

        try (Stream<Path> entries = Files.list(root)) {
            List<Path> sorted = entries.sorted().toList();
            for (Path entry : sorted) {
                String name = entry.getFileName().toString();

                if (Files.isDirectory(entry)) {
                    if (isHatDirectory(entry)) {
                        loadHatDirectory(entry, HatPack.fromDirectory(entry), hats, accessories);
                    } else {
                        loadPack(HatPack.fromDirectory(entry), entry, hats, accessories);
                    }
                } else if (name.endsWith(".zip")) {
                    loadZipPack(entry, hats, accessories);
                }
            }
        }
    }

    static void loadPack(HatPack pack, Path packRoot, List<HatDefinition> hats, List<HatDefinition> accessories)
            throws IOException
    {
        try (Stream<Path> entries = Files.list(packRoot)) {
            List<Path> sorted = entries.sorted().toList();
            for (Path entry : sorted) {
                String entryName = entry.getFileName().toString();

                if (Files.isDirectory(entry)) {
                    loadHatDirectory(entry, pack, hats, accessories);
                } else if (entryName.endsWith(".tbl")) {
                    HatDefinition hat = loadTbl(entry, pack);
                    if (hat != null) {
                        (hat.isAccessory() ? accessories : hats).add(hat);
                    }
                }
            }
        }
    }

    private static void loadZipPack(Path zipFile, List<HatDefinition> hats, List<HatDefinition> accessories)
    {
        String packId = zipFile.getFileName().toString().replaceAll("\\.zip$", "");
        try {
            URI uri = URI.create("jar:" + zipFile.toUri());
            try (FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
                Path root = fs.getPath("/");

                HatPack pack;
                Path packJson = root.resolve("pack.json");
                if (Files.exists(packJson)) {
                    try (InputStream in = Files.newInputStream(packJson)) {
                        pack = HatPack.fromZipEntry(packId, in);
                    }
                } else {
                    pack = new HatPack(packId, packId, "", "1.0", "unknown");
                }

                loadPack(pack, root, hats, accessories);
            }
        } catch (IOException e) {
            HatsMod.LOGGER.error("Failed to load hat pack zip '{}': {}", zipFile.getFileName(), e.getMessage());
        }
    }

    private static HatDefinition loadHatDirectory(Path dir, HatPack pack,
            List<HatDefinition> hats, List<HatDefinition> accessories)
    {
        String hatName = dir.getFileName().toString();

        Path geoJson = dir.resolve("model.geo.json");
        Path texture = dir.resolve("texture.png");
        Path metaJson = dir.resolve("meta.json");

        if (Files.exists(geoJson) && Files.exists(texture)) {
            try {
                HatModelData modelData = GeoJsonReader.read(geoJson);
                byte[] texBytes = Files.readAllBytes(texture);
                HatMeta meta = Files.exists(metaJson) ? GeoJsonReader.readMeta(metaJson) : new HatMeta();
                HatDefinition hat = new HatDefinition(hatName, modelData, texBytes, meta, pack);
                (hat.isAccessory() ? accessories : hats).add(hat);

                try (Stream<Path> entries = Files.list(dir)) {
                    entries.sorted().filter(Files::isDirectory).forEach(sub -> {
                        try {
                            loadHatDirectory(sub, pack, hats, accessories);
                        } catch (Exception e) {
                            HatsMod.LOGGER.error("Failed to load sub-directory '{}': {}", sub.getFileName(), e.getMessage());
                        }
                    });
                } catch (IOException e) {
                    HatsMod.LOGGER.error("Failed to scan subdirectories of '{}': {}", hatName, e.getMessage());
                }

                return hat;
            } catch (IOException e) {
                HatsMod.LOGGER.error("Failed to load geo.json hat '{}': {}", hatName, e.getMessage());
            }
        }

        List<Path> tbls    = new ArrayList<>();
        List<Path> subdirs = new ArrayList<>();
        try (Stream<Path> entries = Files.list(dir)) {
            entries.sorted().forEach(p -> {
                if (Files.isDirectory(p)) subdirs.add(p);
                else if (p.getFileName().toString().endsWith(".tbl")) tbls.add(p);
            });
        } catch (IOException e) {
            HatsMod.LOGGER.error("Failed to scan hat directory '{}': {}", hatName, e.getMessage());
            return null;
        }

        HatDefinition mainHat = null;

        for (Path tbl : tbls) {
            HatDefinition hat = loadTbl(tbl, pack);
            if (hat == null) continue;
            if (hat.isAccessory()) {
                accessories.add(hat);
            } else {
                hats.add(hat);
                if (mainHat == null) mainHat = hat;
            }
        }

        for (Path sub : subdirs) {
            HatDefinition subHat = loadHatDirectory(sub, pack, hats, accessories);
            if (mainHat == null) mainHat = subHat;
        }

        return mainHat;
    }

    private static List<HatDefinition> deduplicate(List<HatDefinition> list)
    {
        LinkedHashMap<String, HatDefinition> seen = new LinkedHashMap<>();
        for (HatDefinition hat : list) {
            seen.putIfAbsent(hat.getFullName(), hat);
        }
        return new ArrayList<>(seen.values());
    }

    private static void registerAccessoriesRecursively(HatDefinition hat)
    {
        for (HatDefinition acc : hat.getAccessories()) {
            HatRegistry.register(acc);
            registerAccessoriesRecursively(acc);
        }
    }

    static HatDefinition loadTbl(Path tbl, HatPack pack)
    {
        String rawName = tbl.getFileName().toString().replaceAll("\\.tbl$", "");
        try {
            HatModelData modelData = TblReader.readModel(tbl);
            byte[] texBytes = TblReader.readTexture(tbl);
            List<String> notes = TblReader.readNotes(tbl);
            HatMeta meta = HatMeta.fromTblNotes(notes);
            return new HatDefinition(rawName, modelData, texBytes, meta, pack);
        } catch (IOException e) {
            HatsMod.LOGGER.error("Failed to load tbl hat '{}': {}", rawName, e.getMessage());
        }
        return null;
    }

    private static boolean isHatDirectory(Path dir)
    {
        if (Files.exists(dir.resolve("model.geo.json"))) return true;
        try (Stream<Path> entries = Files.list(dir)) {
            return entries.anyMatch(p -> p.getFileName().toString().endsWith(".tbl"));
        } catch (IOException e) {
            return false;
        }
    }

    private static void ensureDirectory(Path dir) throws IOException
    {
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
    }
}
