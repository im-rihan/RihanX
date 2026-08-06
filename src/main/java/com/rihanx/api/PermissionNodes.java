package com.rihanx.api;

/**
 * Central permission node constants for RihanX.
 */
public final class PermissionNodes {

    public static final String ROOT = "rihanx";
    public static final String ALL = ROOT + ".*";
    public static final String USE = ROOT + ".use";
    public static final String ADMIN = ROOT + ".admin";

    public static final String SLIME = ROOT + ".slime";
    public static final String SLIME_ALL = SLIME + ".*";
    public static final String SLIME_NEAREST = SLIME + ".nearest";
    public static final String SLIME_SEARCH = SLIME + ".search";
    public static final String SLIME_DENSITY = SLIME + ".density";
    public static final String SLIME_MAP = SLIME + ".map";
    public static final String SLIME_TP = SLIME + ".tp";

    public static final String WORLD = ROOT + ".world";
    public static final String WORLD_ALL = WORLD + ".*";
    public static final String WORLD_INFO = WORLD + ".info";
    public static final String WORLD_SEED = WORLD + ".seed";
    public static final String WORLD_WEATHER = WORLD + ".weather";
    public static final String WORLD_DIFFICULTY = WORLD + ".difficulty";
    public static final String WORLD_TIME = WORLD + ".time";
    public static final String WORLD_BORDER = WORLD + ".border";
    public static final String WORLD_SPAWN = WORLD + ".spawn";
    public static final String WORLD_SETSPAWN = WORLD + ".setspawn";

    public static final String FIND = ROOT + ".find";
    public static final String FIND_ALL = FIND + ".*";
    public static final String FIND_BIOME = FIND + ".biome";
    public static final String FIND_STRUCTURE = FIND + ".structure";

    public static final String CHUNK = ROOT + ".chunk";
    public static final String CHUNK_ALL = CHUNK + ".*";
    public static final String CHUNK_INFO = CHUNK + ".info";
    public static final String CHUNK_LOAD = CHUNK + ".load";
    public static final String CHUNK_UNLOAD = CHUNK + ".unload";
    public static final String CHUNK_REGENERATE = CHUNK + ".regenerate";
    public static final String CHUNK_BORDER = CHUNK + ".border";
    public static final String CHUNK_ENTITIES = CHUNK + ".entities";
    public static final String CHUNK_TILEENTITIES = CHUNK + ".tileentities";

    public static final String TP = ROOT + ".tp";
    public static final String TP_ALL = TP + ".*";
    public static final String TP_POS = TP + ".pos";
    public static final String TP_PLAYER = TP + ".player";
    public static final String TP_WORLD = TP + ".world";
    public static final String TP_BIOME = TP + ".biome";
    public static final String TP_STRUCTURE = TP + ".structure";
    public static final String TP_CHUNK = TP + ".chunk";
    public static final String TP_RANDOM = TP + ".random";
    public static final String TP_SAFE = TP + ".safe";
    public static final String TP_BACK = TP + ".back";
    public static final String TP_HOME = TP + ".home";
    public static final String TP_HERE = TP + ".here";

    public static final String PLAYER = ROOT + ".player";
    public static final String PLAYER_ALL = PLAYER + ".*";
    public static final String PLAYER_INFO = PLAYER + ".info";
    public static final String PLAYER_HEAL = PLAYER + ".heal";
    public static final String PLAYER_FEED = PLAYER + ".feed";
    public static final String PLAYER_FLY = PLAYER + ".fly";
    public static final String PLAYER_SPEED = PLAYER + ".speed";
    public static final String PLAYER_FREEZE = PLAYER + ".freeze";
    public static final String PLAYER_UNFREEZE = PLAYER + ".unfreeze";
    public static final String PLAYER_VANISH = PLAYER + ".vanish";
    public static final String PLAYER_PING = PLAYER + ".ping";
    public static final String PLAYER_GOD = PLAYER + ".god";
    public static final String PLAYER_GAMEMODE = PLAYER + ".gamemode";
    public static final String PLAYER_CLEAREFFECTS = PLAYER + ".cleareffects";

    public static final String INVENTORY = ROOT + ".inventory";
    public static final String INVENTORY_ALL = INVENTORY + ".*";
    public static final String INVENTORY_SEE = INVENTORY + ".see";
    public static final String INVENTORY_ENDER = INVENTORY + ".ender";
    public static final String INVENTORY_CLEAR = INVENTORY + ".clear";
    public static final String INVENTORY_REPAIR = INVENTORY + ".repair";

    public static final String ITEM = ROOT + ".item";
    public static final String ITEM_ALL = ITEM + ".*";
    public static final String ITEM_INFO = ITEM + ".info";
    public static final String ITEM_RENAME = ITEM + ".rename";
    public static final String ITEM_LORE = ITEM + ".lore";
    public static final String ITEM_ENCHANT = ITEM + ".enchant";
    public static final String ITEM_REPAIR = ITEM + ".repair";
    public static final String ITEM_GIVE = ITEM + ".give";
    public static final String ITEM_GIVE_OTHERS = ITEM_GIVE + ".others";
    public static final String ITEM_ENCHANT_FREE = ITEM_ENCHANT + ".free";

    public static final String SEARCH = ROOT + ".search";
    public static final String SEARCH_ALL = SEARCH + ".*";
    public static final String SEARCH_SLIME = SEARCH + ".slime";
    public static final String SEARCH_CAVE = SEARCH + ".cave";
    public static final String SEARCH_LAVA = SEARCH + ".lava";
    public static final String SEARCH_WATER = SEARCH + ".water";
    public static final String SEARCH_SPAWNER = SEARCH + ".spawner";
    public static final String SEARCH_VILLAGE = SEARCH + ".village";

    public static final String SERVER = ROOT + ".server";
    public static final String SERVER_ALL = SERVER + ".*";
    public static final String SERVER_INFO = SERVER + ".info";
    public static final String SERVER_TPS = SERVER + ".tps";
    public static final String SERVER_MSPT = SERVER + ".mspt";
    public static final String SERVER_MEMORY = SERVER + ".memory";
    public static final String SERVER_UPTIME = SERVER + ".uptime";

    public static final String PERFORMANCE = ROOT + ".performance";
    public static final String PERFORMANCE_ALL = PERFORMANCE + ".*";
    public static final String PERFORMANCE_CHUNKS = PERFORMANCE + ".chunks";
    public static final String PERFORMANCE_ENTITIES = PERFORMANCE + ".entities";

    public static final String PROTECT = ROOT + ".protect";
    public static final String PROTECT_ALL = PROTECT + ".*";
    public static final String PROTECT_BYPASS = PROTECT + ".bypass";
    public static final String PROTECT_ADMIN = PROTECT + ".admin";
    public static final String PROTECT_WAND = PROTECT + ".wand";
    public static final String PROTECT_FLAG = PROTECT + ".flag";
    public static final String PROTECT_REGION = PROTECT + ".region";

    public static final String EDIT = ROOT + ".edit";
    public static final String EDIT_ALL = EDIT + ".*";
    public static final String EDIT_WAND = EDIT + ".wand";
    public static final String EDIT_CLIPBOARD = EDIT + ".clipboard";
    public static final String EDIT_HISTORY = EDIT + ".history";

    public static final String HOME = ROOT + ".home";
    public static final String HOME_ALL = HOME + ".*";
    public static final String HOME_SET = HOME + ".set";
    public static final String HOME_DELETE = HOME + ".delete";
    public static final String HOME_LIST = HOME + ".list";
    public static final String HOME_TP = HOME + ".tp";

    public static final String WARP = ROOT + ".warp";
    public static final String WARP_ALL = WARP + ".*";
    public static final String WARP_SET = WARP + ".set";
    public static final String WARP_DELETE = WARP + ".delete";
    public static final String WARP_LIST = WARP + ".list";
    public static final String WARP_TP = WARP + ".tp";

    public static final String TPA = ROOT + ".tpa";
    public static final String TPA_ALL = TPA + ".*";
    public static final String TPA_HERE = TPA + ".here";
    public static final String TPA_ACCEPT = TPA + ".accept";
    public static final String TPA_DENY = TPA + ".deny";
    public static final String TPA_CANCEL = TPA + ".cancel";

    public static final String KIT = ROOT + ".kit";
    public static final String KIT_ALL = KIT + ".*";
    public static final String KIT_LIST = KIT + ".list";

    public static final String MSG = ROOT + ".msg";
    public static final String REPLY = ROOT + ".reply";
    public static final String AFK = ROOT + ".afk";
    public static final String SPAWN = ROOT + ".spawn";
    public static final String SETSPAWN = ROOT + ".setspawn";

    public static final String BASE = ROOT + ".base";
    public static final String BASE_ALL = BASE + ".*";
    public static final String BASE_BUILD = BASE + ".build";
    public static final String BASE_LIST = BASE + ".list";

    public static final String BYPASS_COOLDOWN = ROOT + ".bypass.cooldown";
    public static final String BYPASS_TELEPORT_DELAY = ROOT + ".bypass.teleportdelay";
    public static final String SEE_VANISHED = ROOT + ".see.vanished";

    private PermissionNodes() {
    }
}
