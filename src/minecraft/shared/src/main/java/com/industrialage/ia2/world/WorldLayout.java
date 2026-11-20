package com.industrialage.ia2.world;

/** Native pixel coordinates measured from the supplied IC2 GUI atlases. */
final class WorldLayout {
    final String texture;
    final int width, height, playerY, hotbarY;
    final int[] x, y, page;
    final int pages;

    private WorldLayout(
            String texture, int width, int height, int playerY, int hotbarY, int size, int pages) {
        this.texture = texture;
        this.width = width;
        this.height = height;
        this.playerY = playerY;
        this.hotbarY = hotbarY;
        this.pages = pages;
        x = new int[size];
        y = new int[size];
        page = new int[size];
        java.util.Arrays.fill(x, -1000);
        java.util.Arrays.fill(y, -1000);
        java.util.Arrays.fill(page, -2);
    }

    private void at(int i, int px, int py) {
        at(i, px, py, -1);
    }

    private void at(int i, int px, int py, int onPage) {
        x[i] = px;
        y[i] = py;
        page[i] = onPage;
    }

    static WorldLayout of(WorldMachineBlock.Kind kind) {
        WorldLayout l =
                switch (kind) {
                    case CROP_HARVESTER ->
                            new WorldLayout("guicropharvester", 176, 166, 84, 142, 9, 1);
                    case CROPMATRON -> new WorldLayout("guicropmatron", 176, 192, 110, 168, 18, 1);
                    case CLASSIC_CROPMATRON ->
                            new WorldLayout("gui_cropmatron_classic", 176, 166, 84, 142, 9, 1);
                    case TERRAFORMER -> new WorldLayout(null, 176, 166, 84, 142, 9, 1);
                    case MINER -> new WorldLayout("guiminer", 176, 166, 84, 142, 9, 1);
                    case ADVANCED_MINER ->
                            new WorldLayout("guiadvminer", 176, 203, 121, 179, 18, 1);
                    case BATCH_CRAFTER ->
                            new WorldLayout("guibatchcrafter", 176, 206, 124, 182, 27, 1);
                    case ITEM_BUFFER -> new WorldLayout("guiitembuffer", 176, 233, 151, 209, 27, 1);
                    case SORTING_MACHINE ->
                            new WorldLayout("guisortingmachine", 212, 243, 161, 219, 27, 2);
                    case WEIGHTED_DISTRIBUTOR ->
                            new WorldLayout(
                                    "guiweighteditemdistributor", 176, 211, 129, 187, 27, 3);
                    case CHUNK_LOADER ->
                            new WorldLayout("guichunkloader", 176, 250, 168, 226, 9, 1);
                };
        switch (kind) {
            case CROP_HARVESTER -> {
                for (int i = 0; i < 8; i++) l.at(i, 48 + 18 * (i % 5), 17 + 18 * (i / 5));
                l.at(8, 16, 53);
            }
            case MINER -> {
                for (int i = 0; i < 3; i++) l.at(i, 8, 22 + 18 * i);
                for (int i = 3; i < 8; i++)
                    l.at(i, 44 + 18 * ((i - 3) % 4), 22 + 18 * ((i - 3) / 4));
                l.at(8, 152, 58);
            }
            case ADVANCED_MINER -> {
                l.at(0, 8, 26);
                for (int i = 1; i < 8; i++)
                    l.at(i, 36 + 18 * ((i - 1) % 5), 44 + 18 * ((i - 1) / 5));
                l.at(8, 8, 80);
                l.at(9, 152, 26);
                for (int i = 10; i < 18; i++)
                    l.at(i, 36 + 18 * ((i - 3) % 5), 44 + 18 * ((i - 3) / 5));
            }
            case CROPMATRON -> {
                l.at(0, 49, 27);
                l.at(1, 67, 27);
                l.at(2, 57, 56);
                for (int i = 3; i < 8; i++) l.at(i, 8 + 18 * (i - 3), 80);
                l.at(8, 134, 80);
                for (int i = 9; i < 13; i++) l.at(i, 152, 26 + 18 * (i - 9));
                l.at(13, 75, 56);
                l.at(14, 98, 80);
                l.at(15, 116, 80);
            }
            case CLASSIC_CROPMATRON -> {
                for (int i = 0; i < 3; i++) l.at(i, 62 + 36 * i, 20);
                for (int i = 3; i < 8; i++)
                    l.at(i, 62 + 36 * ((i - 3) % 3), 38 + 18 * ((i - 3) / 3));
                l.at(8, 8, 58);
            }
            case TERRAFORMER -> {
                l.at(0, 53, 34);
                l.at(8, 116, 34);
            }
            case BATCH_CRAFTER -> {
                for (int i = 0; i < 9; i++) {
                    l.at(i, 8 + 18 * i, 84);
                    l.at(i + 18, 30 + 18 * (i % 3), 17 + 18 * (i / 3));
                }
                for (int i = 9; i < 17; i++) l.at(i, 8 + 18 * (i - 9), 102);
                l.at(17, 8, 62);
            }
            case ITEM_BUFFER -> {
                for (int i = 0; i < 26; i++)
                    l.at(i, 8 + 18 * (i % 4) + (i % 8 >= 4 ? 90 : 0), 18 + 18 * (i / 8));
                l.at(26, 35, 128);
            }
            case SORTING_MACHINE -> {
                for (int i = 0; i < 18; i++) l.at(i, 8 + 18 * (i % 9), 141, i / 9);
                for (int i = 18; i < 24; i++) l.at(i, 80, 19 + 20 * (i - 18));
                l.at(24, 188, 215);
            }
            case WEIGHTED_DISTRIBUTOR -> {
                for (int i = 0; i < 18; i++) l.at(i, 8 + 18 * (i % 9), 108, i / 9);
                for (int i = 18; i < 24; i++) l.at(i, 146, 10 + 16 * (i - 18), 2);
                l.at(25, 8, 89, 2);
            }
            case CHUNK_LOADER -> l.at(8, 8, 143);
        }
        return l;
    }
}
