# Loot checker

Wrapper around seed-checker with easy config for mainstream loot checking

## Configuration

File `config.json` contains example configuration, all fields should be self-explanatory.

`Dimension` must be one of those: `OVERWORLD`, `NETHER`, `END`\
`TargeState` must be one of those: `NO_STRUCTURES`, `STRUCTURES`, `WITH_MOBS`, `FULL`
(from least computational power to most)\
`SearchItems` should be taken from wiki or in-game debug info and represent valid item id (not numerical), if it
will not match any item in existence it will be ignored (allowing to manipulate index in format string)

`FilteredSearchLocationsFile` must point to a csv with 2 columns of integers that indicate **coordinates to structures**,
can be prepared with all sorts of apps but tested on [cubiomes-viewer](https://github.com/Cubitect/cubiomes-viewer).
Line that do not start with number will be ignored. Those coordinates will be rounded up to chunk coordinates.
> Example:
> ```
> desert_pyramid, 4
> 10352, -67024
> 10976, -66720
> 11008, -67344
> 1104, -44736
> ```

## Output format

Output format can be controlled by setting `OutputFormatString` in config to printf-like format string ([docs](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/Formatter.html#syntax))\
Arguments are following: item id as string, x, y, z as ints, index of match from `SearchItems` array (starting from 0). 

## Credit

Made by [FlexCoral](github.com/maxsupermanhd) (discord: @MaX#6717) using [seed-checker](https://github.com/jellejurre/seed-checker)

SeedChecker by [jellejurre](https://github.com/jellejurre) (discord: @jellejurre#8585) and [dragontamerfred](https://github.com/KalleStruik) (discord: @dragontamerfred#2779), published with help from WearBlackAllDay