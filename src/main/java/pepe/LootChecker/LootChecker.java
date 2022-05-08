package pepe.LootChecker;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.ProtoChunk;
import nl.jellejurre.seedchecker.SeedChecker;
import nl.jellejurre.seedchecker.SeedCheckerDimension;
import nl.jellejurre.seedchecker.TargetState;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static net.minecraft.block.entity.BlockEntityType.CHEST;
import static net.minecraft.item.Items.HEART_OF_THE_SEA;
import static net.minecraft.item.Items.ENCHANTED_GOLDEN_APPLE;
import static nl.jellejurre.seedchecker.SeedCheckerDimension.OVERWORLD;
import static nl.jellejurre.seedchecker.TargetState.STRUCTURES;

public class LootChecker {
    public static LootCheckerConfig config;
    public static final Logger LOGGER = Logger.getGlobal();
    public static FileWriter output;

    public static int SearchChunk(SeedChecker checker, int cx, int cz) {
        ProtoChunk Chunk = checker.getOrBuildChunk(cx, cz);
        Map<BlockPos, BlockEntity> BE = Chunk.getBlockEntities();
        int foundChests = 0;
        for (BlockPos pos : BE.keySet()) {
            BlockEntity entity = BE.get(pos);
            if (entity.getType() == CHEST) {
                foundChests++;
                List<ItemStack> ChestItems = checker.generateChestLoot(pos);
                for (ItemStack item : ChestItems) {
                    for (int i = 0; i < config.SearchItems.length; i++) {
                        if (config.SearchItems[i].equalsIgnoreCase(item.getItem().toString())) {
                            LOGGER.info(String.format("Loot: [% 12d][% 4d][% 12d] % 3d [%s]\n", pos.getX(), pos.getY(), pos.getZ(), item.getCount(), item.getItem().toString()));
                            try {
                                output.write(String.format(config.OutputFormatString, item.getItem().toString(), pos.getX(), pos.getY(), pos.getZ(), i));
                            } catch (IOException e) {
                                LOGGER.log(Level.INFO, "IO exception while reading input file", e);
                            }
                            break;
                        }
                    }
                }
            }
        }
        return foundChests;
    }

    public static ArrayList<BlockPos> LoadTreasurePositions(String filename) {
        ArrayList<BlockPos> positions = new ArrayList<>();
        FileInputStream fstream;
        try {
            fstream = new FileInputStream(filename);
        } catch (FileNotFoundException e) {
            LOGGER.log(Level.INFO, "File not found", e);
            return positions;
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
        String strLine;
        try {
            while ((strLine = br.readLine()) != null) {
                Scanner s = new Scanner(strLine).useDelimiter("\\s*, \\s*");
                if (s.hasNextInt()) {
                    int ogx = s.nextInt();
                    int ogz = s.nextInt();
                    positions.add(new BlockPos(ogx / 16, 64, ogz / 16));
                }
                s.close();
            }
            fstream.close();
        } catch (IOException e) {
            LOGGER.log(Level.INFO, "IO exception while reading input file", e);
            return positions;
        }
        LOGGER.info(String.format("Loaded %d positions", positions.size()));
        return positions;
    }

    public static LootCheckerConfig LoadConfig(String[] args) {
        String path = "config.json";
        if (args.length > 0) {
            path = args[1];
        }
        LOGGER.log(Level.INFO, "Loading config " + "config.json");
        LootCheckerConfig ret;
        try {
            ObjectMapper mapper = new ObjectMapper();
            ret = mapper.readValue(new File(path), LootCheckerConfig.class);
        } catch (IOException e) {
            LOGGER.log(Level.INFO, "IO exception while reading config file", e);
            throw new RuntimeException(e);
        }
        return ret;
    }

    public static void main(String[] args) {
        config = LoadConfig(args);
        LOGGER.info("Opening output file...");
        try {
            output = new FileWriter(new File(config.OutputFilename).getAbsoluteFile(), true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        LOGGER.info("Loading filtered locations...");
        ArrayList<BlockPos> positions = LoadTreasurePositions(config.FilteredSearchLocationsFile);
        LOGGER.info("Creating checker...");
        SeedChecker Checker = new SeedChecker(config.Seed, TargetState.valueOf(config.TargetState), SeedCheckerDimension.valueOf(config.Dimension));
        LOGGER.info(String.format("Creating thread pool of %d threads...", config.Threads));
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(config.Threads);
        LOGGER.info("Starting search...");
        for (BlockPos p : positions) {
            executor.submit(() -> {
                int found = SearchChunk(Checker, p.getX(), p.getZ());
                found += SearchChunk(Checker, p.getX() - 1, p.getZ());
                found += SearchChunk(Checker, p.getX() + 1, p.getZ());
                found += SearchChunk(Checker, p.getX(), p.getZ() - 1);
                found += SearchChunk(Checker, p.getX(), p.getZ() + 1);
                found += SearchChunk(Checker, p.getX() - 1, p.getZ() - 1);
                found += SearchChunk(Checker, p.getX() - 1, p.getZ() + 1);
                found += SearchChunk(Checker, p.getX() + 1, p.getZ() - 1);
                found += SearchChunk(Checker, p.getX() + 1, p.getZ() + 1);
                LOGGER.info(String.format("Checked chunk [% 8d][% 8d]: found %d chests", p.getX(), p.getZ(), found));
            });
        }
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            System.out.println(e);
        }
        LOGGER.info("Finished.");
    }
}
