package pepe.EgapChecker;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.ProtoChunk;
import nl.jellejurre.seedchecker.SeedChecker;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static net.minecraft.block.entity.BlockEntityType.CHEST;
import static net.minecraft.item.Items.HEART_OF_THE_SEA;
import static net.minecraft.item.Items.ENCHANTED_GOLDEN_APPLE;
import static nl.jellejurre.seedchecker.SeedCheckerDimension.OVERWORLD;
import static nl.jellejurre.seedchecker.TargetState.STRUCTURES;

public class EgapChecker {
    private static boolean SearchChunk(SeedChecker checker, int cx, int cz, DateTimeFormatter dtf) {
        ProtoChunk Chunk = checker.getOrBuildChunk(cx, cz);
        Map<BlockPos, BlockEntity> BE = Chunk.getBlockEntities();
        boolean foundChest = false;
        for (BlockPos pos : BE.keySet()) {
            BlockEntity entity = BE.get(pos);
            if (entity.getType() == CHEST) {
                foundChest = true;
                List<ItemStack> ChestItems = checker.generateChestLoot(pos);
                for (ItemStack item : ChestItems) {
                    if (item.isOf(HEART_OF_THE_SEA) || item.isOf(ENCHANTED_GOLDEN_APPLE)) {
                        System.out.printf(dtf.format(LocalDateTime.now())+"Loot: [% 12d][% 4d][% 12d] % 3d [%s]\n", pos.getX(), pos.getY(), pos.getZ(), item.getCount(), item.getItem().toString());
                        try {
                            Files.write(Paths.get("./Out.txt"),
                                String.format("waypoint:Treasure:T:%d:%d:%d:5:false:0:gui.xaero_default:false:0:false\n", pos.getX(), pos.getY(), pos.getZ()).getBytes(),
                                StandardOpenOption.APPEND);
                        } catch (IOException e) {
                            System.out.println(dtf.format(LocalDateTime.now())+"IOException");
                            e.printStackTrace();
                        }
                    // } else {
                    //     System.out.printf(dtf.format(LocalDateTime.now())+"Loot: [% 12d][% 4d][% 12d] % 3d [%s]\n", pos.getX(), pos.getY(), pos.getZ(), item.getCount(), item.getItem().toString());
                    }
                }
            }
        }
        return foundChest;
    }
    private static ArrayList<BlockPos> LoadTreasurePositions(String filename) {
        ArrayList<BlockPos> positions = new ArrayList<BlockPos>();
        FileInputStream fstream;
        try {
            fstream = new FileInputStream(filename);
        } catch(FileNotFoundException e) {
            System.out.println("File not found!");
            return positions;
        }
        System.out.println("Creating reader");
        BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
        String strLine;
        System.out.println("Reading file");
        try {
            while ((strLine = br.readLine()) != null) {
                Scanner s = new Scanner(strLine).useDelimiter("\\s*, \\s*");
                if (s.hasNextInt()) {
                    int ogx = s.nextInt();
                    int ogz = s.nextInt();
                    positions.add(new BlockPos(ogx/16, 64, ogz/16));
                } else {
                    System.out.printf("Skipping non coordinate line [%s]\n", strLine);
                }
                s.close();
            }
            fstream.close();
        } catch(IOException e) {
            System.out.println("IOException");
            return positions;
        }
        System.out.printf("Readed %d objects\n", positions.size());
        return positions;
    }
    public static void main(String[] args) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss ");
        System.out.println(dtf.format(LocalDateTime.now())+"Opening file");
        ArrayList<BlockPos> positions = LoadTreasurePositions("./treasures.csv");
        System.out.println(dtf.format(LocalDateTime.now())+"Creating checker");
        SeedChecker Checker = new SeedChecker(-529968648997884325L, STRUCTURES, OVERWORLD);
        System.out.println(dtf.format(LocalDateTime.now())+"Starting up");
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(4);
        for (BlockPos p : positions) {
            executor.submit(() -> {
                if (!SearchChunk(Checker, p.getX(), p.getZ(), dtf) &&
                    !SearchChunk(Checker, p.getX()-1, p.getZ(), dtf) &&
                    !SearchChunk(Checker, p.getX()+1, p.getZ(), dtf) &&
                    !SearchChunk(Checker, p.getX(), p.getZ()-1, dtf) &&
                    !SearchChunk(Checker, p.getX(), p.getZ()+1, dtf) &&
                    !SearchChunk(Checker, p.getX()-1, p.getZ()-1, dtf) &&
                    !SearchChunk(Checker, p.getX()-1, p.getZ()+1, dtf) &&
                    !SearchChunk(Checker, p.getX()+1, p.getZ()-1, dtf) &&
                    !SearchChunk(Checker, p.getX()+1, p.getZ()+1, dtf)) {
                    System.out.printf(dtf.format(LocalDateTime.now())+"Failed to find chest at chunk [% 8d][% 8d]\n", p.getX(), p.getZ());
                } else {
                    System.out.printf(dtf.format(LocalDateTime.now())+"Checked chunk [% 8d][% 8d]\n", p.getX(), p.getZ());
                }
            });
        }
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            System.out.println(e.toString());
        }
        System.out.println(dtf.format(LocalDateTime.now())+"Finished");
    }
}
