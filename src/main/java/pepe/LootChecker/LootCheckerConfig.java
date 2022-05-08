package pepe.LootChecker;

public class LootCheckerConfig {
    public long Seed = 0;
    public String Dimension = "OVERWORLD";
    public String TargetState = "STRUCTURES";
    public int Threads = 4;
    public String OutputFilename = "out.txt";
    public String FilteredSearchLocationsFile = "inputLocations.csv";
    public String[] SearchItems = {"minecraft:enchanted_golden_apple"};
    public String OutputFormatString = "waypoint:%s:T:%d:%d:%d:%d:false:0:gui.xaero_default:false:0:false\n";
}
