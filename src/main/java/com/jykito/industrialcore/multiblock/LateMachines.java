package com.jykito.industrialcore.multiblock;

/**
 * Late multiblock shapes. The source is the author's builds, captured with the structure wand.
 * Layers bottom to top, rows from the face inwards, symbols left to right (see {@link LateMultiblockPattern}).
 */
public final class LateMachines {

    private LateMachines() {}

    /** Industrial macerator 5x5x4: 9 mechanisms, glass at the core and in the middle of the sides, a funnel ring on top. */
    public static final LateMultiblockPattern MACERATOR = new LateMultiblockPattern(1,
            new String[]{"CCCCC", "CCCCC", "CCCCC", "CCCCC", "CCCCC"},
            new String[]{"CGKGC", "CMMMC", "GMMMG", "CMMMC", "CCCCC"},
            new String[]{"CCCCC", "CCCCC", "CCCCC", "CCCCC", "CCCCC"},
            new String[]{".....", ".CCC.", ".C.C.", ".CCC.", "....."});

    /**
     * Industrial furnace 5x3x5: a tall monolith, 9 mechanisms in a 3x3 column at the back,
     * a 3x3 glass window above the core. Shape approved from the render, not captured with the wand yet.
     */
    public static final LateMultiblockPattern FURNACE = new LateMultiblockPattern(1,
            new String[]{"CCCCC", "CCCCC", "CCCCC"},
            new String[]{"CGKGC", "CMMMC", "CCCCC"},
            new String[]{"CGGGC", "CMMMC", "CCCCC"},
            new String[]{"CGGGC", "CMMMC", "CCCCC"},
            new String[]{"CCCCC", "CCCCC", "CCCCC"});

    /**
     * Industrial compressor 3x5x5: a narrow front, 9 mechanisms in a 1x3x3 column at the back,
     * a long side window on one side. Shape approved from the render, not captured with the wand yet.
     */
    public static final LateMultiblockPattern COMPRESSOR = new LateMultiblockPattern(1,
            new String[]{"CCC", "CCC", "CCC", "CCC", "CCC"},
            new String[]{"CKC", "CMC", "CMC", "CMC", "CCC"},
            new String[]{"CGC", "GMC", "GMC", "GMC", "CCC"},
            new String[]{"CGC", "GMC", "GMC", "GMC", "CCC"},
            new String[]{"CCC", "CCC", "CCC", "CCC", "CCC"});

    /**
     * Industrial rolling mill 7x4x3: a long low line, 10 mechanisms in two rows,
     * glass at the core and a strip along the roof. Two modes: rolling and drawing. Shape from the render.
     */
    public static final LateMultiblockPattern ROLLING_MILL = new LateMultiblockPattern(1,
            new String[]{"CCCCCCC", "CCCCCCC", "CCCCCCC", "CCCCCCC"},
            new String[]{"CGGKGGC", "CMMMMMC", "CMMMMMC", "CCCCCCC"},
            new String[]{"CCCCCCC", "CGGGGGC", "CGGGGGC", "CCCCCCC"});

    /**
     * Industrial ore washer "Bath" 5x5x4: 9 mechanisms, glass at the core, a glass strip on top,
     * a window on one side; the top tier is a rim, open inside (cells not checked).
     * Shape approved from the render, not captured with the wand yet.
     */
    public static final LateMultiblockPattern WASHER = new LateMultiblockPattern(1,
            new String[]{"CCCCC", "CCCCC", "CCCCC", "CCCCC", "CCCCC"},
            new String[]{"CGKGC", "CMMMC", "CMMMG", "CMMMC", "CCCCC"},
            new String[]{"CCCCC", "CGGGC", "CGGGC", "CGGGC", "CCCCC"},
            new String[]{"CCCCC", "C...C", "C...C", "C...C", "CCCCC"});

    /**
     * Industrial assembler "Ring" 7x7x3: 16 mechanisms in a ring around a solid centre,
     * glass along the front at the core and in a ring on the roof. Shape approved from the render, not captured with the wand yet.
     */
    public static final LateMultiblockPattern CRAFTER = new LateMultiblockPattern(1,
            new String[]{"CCCCCCC", "CCCCCCC", "CCCCCCC", "CCCCCCC", "CCCCCCC", "CCCCCCC", "CCCCCCC"},
            new String[]{"CGGKGGC", "CMMMMMC", "CMCCCMC", "CMCCCMC", "CMCCCMC", "CMMMMMC", "CCCCCCC"},
            new String[]{"CCCCCCC", "CGGGGGC", "CGCCCGC", "CGCCCGC", "CGCCCGC", "CGGGGGC", "CCCCCCC"});

    /**
     * Industrial liquefier "Tower and bath" 6x3x6, tier two: advanced casing and glass,
     * needs a heat port. The core is in the tower front (on the left when looking at the machine), on the right a low bath
     * with a glass lid. 6 mechanisms. Shape approved from the render, not captured with the wand yet.
     */
    public static final LateMultiblockPattern LIQUEFIER = new LateMultiblockPattern(1,
            new String[]{"CCCCCC", "CCCCCC", "CCCCCC"},
            new String[]{"CGCCKC", "CMMCMC", "CCCCCC"},
            new String[]{"GGGCGC", "GGGGMG", "GGGCGC"},
            new String[]{"...CGC", "...CMG", "...CGC"},
            new String[]{"...CGC", "...CMG", "...CGC"},
            new String[]{"...CCC", "...CCC", "...CCC"}).advancedTier().requiresHeat();

    /**
     * Tier 2 blast furnace "Tunnel" 7x5x4: a long furnace, the roof meets at a glass ridge,
     * the core in the front between two glass blocks. 20 mechanisms, advanced casing and glass, needs a heat port.
     * Shape chosen by the author 2026-10-06 from the render (variant G), not captured with the wand.
     */
    public static final LateMultiblockPattern BLAST_FURNACE = new LateMultiblockPattern(1,
            new String[]{"CCCCCCC", "CCCCCCC", "CCCCCCC", "CCCCCCC", "CCCCCCC"},
            new String[]{"CGGKGGC", "CMMMMMC", "CMMMMMC", "CMMMMMC", "CCCCCCC"},
            new String[]{".......", "CCCCCCC", "CMMMMMC", "CCCCCCC", "......."},
            new String[]{".......", ".......", "CGGGGGC", ".......", "......."}).advancedTier().requiresHeat();

    /**
     * Industrial fluid enricher "Two tanks and a mixer" 9x5x3: round glass tanks on the sides
     * (corners not checked), a low mixer with the core between them. 9 mechanisms. Shape approved from the render.
     */
    public static final LateMultiblockPattern FLUID_ENRICHER = new LateMultiblockPattern(1,
            new String[]{".C.CCC.C.", "CCCCCCCCC", ".C.CCC.C."},
            new String[]{".G.GKG.G.", "GMGMMMGMG", ".G.CCC.G."},
            new String[]{".G.CCC.G.", "GMGCGCGMG", ".G.CCC.G."},
            new String[]{".G.....G.", "GMG...GMG", ".G.....G."},
            new String[]{".C.....C.", "CCC...CCC", ".C.....C."});

    /**
     * Late magnet 7x9x7, tier two: a slab with cut corners, on it a plinth with the core and four posts
     * at the corners, a thin trunk with glass and a spine of mechanisms, a crosspiece, a 3x3 head of mechanisms under a glass
     * dome, a crown of teeth. The slab sticks out two blocks in front of the core. 13 mechanisms.
     * The shape is the author's build captured with the wand (2026-10-04), with posts from variant V3.
     */
    public static final LateMultiblockPattern MAGNET = new LateMultiblockPattern(1,
            new String[]{".CCCCC.", "CCCCCCC", "CCCCCCC", "CCCCCCC", "CCCCCCC", "CCCCCCC", ".CCCCC."},
            new String[]{".......", ".C...C.", "..CKC..", "..CCC..", "..CCC..", ".C...C.", "......."},
            new String[]{".......", ".G...G.", "...G...", "..GMG..", "...G...", ".G...G.", "......."},
            new String[]{".......", ".C...C.", "...G...", "..GMG..", "...G...", ".C...C.", "......."},
            new String[]{".......", ".......", "...C...", "..CMC..", "...C...", ".......", "......."},
            new String[]{".......", "...C...", "...C...", ".CCMCC.", "...C...", "...C...", "......."},
            new String[]{".......", "..CCC..", ".CMMMC.", ".CMMMC.", ".CMMMC.", "..CCC..", "......."},
            new String[]{".......", "..C.C..", ".CGGGC.", "..GGG..", ".CGGGC.", "..C.C..", "......."},
            new String[]{".......", "..C.C..", ".C...C.", ".......", ".C...C.", "..C.C..", "......."}).advancedTier();
}
