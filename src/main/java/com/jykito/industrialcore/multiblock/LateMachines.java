package com.jykito.industrialcore.multiblock;

/**
 * Формы лейт-мультиблоков. Источник — постройки юзера, снятые структурным жезлом.
 * Слои снизу вверх, строки от лица вглубь, символы слева направо (см. {@link LateMultiblockPattern}).
 */
public final class LateMachines {

    private LateMachines() {}

    /** Промышленный мацератор 5×5×4: 9 механизмов, стекло у ядра и по центрам боков, сверху кольцо-воронка. */
    public static final LateMultiblockPattern MACERATOR = new LateMultiblockPattern(1,
            new String[]{"CCCCC", "CCCCC", "CCCCC", "CCCCC", "CCCCC"},
            new String[]{"CGKGC", "CMMMC", "GMMMG", "CMMMC", "CCCCC"},
            new String[]{"CCCCC", "CCCCC", "CCCCC", "CCCCC", "CCCCC"},
            new String[]{".....", ".CCC.", ".C.C.", ".CCC.", "....."});

    /**
     * Промышленная печь 5×3×5: высокий монолит, 9 механизмов колонной 3×3 в глубине,
     * над ядром окно 3×3 из стекла. Форма утверждена по рендеру, жезлом пока не снята.
     */
    public static final LateMultiblockPattern FURNACE = new LateMultiblockPattern(1,
            new String[]{"CCCCC", "CCCCC", "CCCCC"},
            new String[]{"CGKGC", "CMMMC", "CCCCC"},
            new String[]{"CGGGC", "CMMMC", "CCCCC"},
            new String[]{"CGGGC", "CMMMC", "CCCCC"},
            new String[]{"CCCCC", "CCCCC", "CCCCC"});

    /**
     * Промышленный компрессор 3×5×5: узкий фасад, 9 механизмов столбом 1×3×3 в глубине,
     * длинное боковое окно на одном боку. Форма утверждена по рендеру, жезлом пока не снята.
     */
    public static final LateMultiblockPattern COMPRESSOR = new LateMultiblockPattern(1,
            new String[]{"CCC", "CCC", "CCC", "CCC", "CCC"},
            new String[]{"CKC", "CMC", "CMC", "CMC", "CCC"},
            new String[]{"CGC", "GMC", "GMC", "GMC", "CCC"},
            new String[]{"CGC", "GMC", "GMC", "GMC", "CCC"},
            new String[]{"CCC", "CCC", "CCC", "CCC", "CCC"});

    /**
     * Промышленный прокатный стан 7×4×3: длинная низкая линия, 10 механизмов в два ряда,
     * стекло у ядра и полосой по крыше. Два режима — прокатка и волочение. Форма по рендеру.
     */
    public static final LateMultiblockPattern ROLLING_MILL = new LateMultiblockPattern(1,
            new String[]{"CCCCCCC", "CCCCCCC", "CCCCCCC", "CCCCCCC"},
            new String[]{"CGGKGGC", "CMMMMMC", "CMMMMMC", "CCCCCCC"},
            new String[]{"CCCCCCC", "CGGGGGC", "CGGGGGC", "CCCCCCC"});

    /**
     * Промышленный промыватель «Ванна» 5×5×4: 9 механизмов, стекло у ядра, полоса стекла сверху,
     * окно на одном боку; верхний ярус — бортик, внутри открыто (клетки не проверяются).
     * Форма утверждена по рендеру, жезлом пока не снята.
     */
    public static final LateMultiblockPattern WASHER = new LateMultiblockPattern(1,
            new String[]{"CCCCC", "CCCCC", "CCCCC", "CCCCC", "CCCCC"},
            new String[]{"CGKGC", "CMMMC", "CMMMG", "CMMMC", "CCCCC"},
            new String[]{"CCCCC", "CGGGC", "CGGGC", "CGGGC", "CCCCC"},
            new String[]{"CCCCC", "C...C", "C...C", "C...C", "CCCCC"});

    /**
     * Промышленный сборщик «Кольцо» 7×7×3: 16 механизмов кольцом вокруг сплошного центра,
     * стекло по фасаду у ядра и кольцом по крыше. Форма утверждена по рендеру, жезлом пока не снята.
     */
    public static final LateMultiblockPattern CRAFTER = new LateMultiblockPattern(1,
            new String[]{"CCCCCCC", "CCCCCCC", "CCCCCCC", "CCCCCCC", "CCCCCCC", "CCCCCCC", "CCCCCCC"},
            new String[]{"CGGKGGC", "CMMMMMC", "CMCCCMC", "CMCCCMC", "CMCCCMC", "CMMMMMC", "CCCCCCC"},
            new String[]{"CCCCCCC", "CGGGGGC", "CGCCCGC", "CGCCCGC", "CGCCCGC", "CGGGGGC", "CCCCCCC"});

    /**
     * Промышленный сжижитель «Башня и ванна» 6×3×6, второй тир: продвинутая обшивка и стекло,
     * нужен тепловой порт. Ядро в фасаде башни (слева, если смотреть на машину), справа низкая ванна
     * со стеклянной крышкой. 6 механизмов. Форма утверждена по рендеру, жезлом пока не снята.
     */
    public static final LateMultiblockPattern LIQUEFIER = new LateMultiblockPattern(1,
            new String[]{"CCCCCC", "CCCCCC", "CCCCCC"},
            new String[]{"CGCCKC", "CMMCMC", "CCCCCC"},
            new String[]{"GGGCGC", "GGGGMG", "GGGCGC"},
            new String[]{"...CGC", "...CMG", "...CGC"},
            new String[]{"...CGC", "...CMG", "...CGC"},
            new String[]{"...CCC", "...CCC", "...CCC"}).advancedTier().requiresHeat();

    /**
     * Доменная печь 2-го тира «Тоннель» 7×5×4: длинная печь, крыша сходится к стеклянному коньку,
     * ядро в фасаде между двумя стёклами. 20 механизмов, продвинутая обшивка и стекло, нужен тепловой порт.
     * Форма выбрана юзером 2026-10-06 по рендеру (вариант G), жезлом не снята.
     */
    public static final LateMultiblockPattern BLAST_FURNACE = new LateMultiblockPattern(1,
            new String[]{"CCCCCCC", "CCCCCCC", "CCCCCCC", "CCCCCCC", "CCCCCCC"},
            new String[]{"CGGKGGC", "CMMMMMC", "CMMMMMC", "CMMMMMC", "CCCCCCC"},
            new String[]{".......", "CCCCCCC", "CMMMMMC", "CCCCCCC", "......."},
            new String[]{".......", ".......", "CGGGGGC", ".......", "......."}).advancedTier().requiresHeat();

    /**
     * Промышленный обогатитель жидкостей «Два бака и смеситель» 9×5×3: по бокам круглые стеклянные баки
     * (углы не проверяются), между ними низкий смеситель с ядром. 9 механизмов. Форма утверждена по рендеру.
     */
    public static final LateMultiblockPattern FLUID_ENRICHER = new LateMultiblockPattern(1,
            new String[]{".C.CCC.C.", "CCCCCCCCC", ".C.CCC.C."},
            new String[]{".G.GKG.G.", "GMGMMMGMG", ".G.CCC.G."},
            new String[]{".G.CCC.G.", "GMGCGCGMG", ".G.CCC.G."},
            new String[]{".G.....G.", "GMG...GMG", ".G.....G."},
            new String[]{".C.....C.", "CCC...CCC", ".C.....C."});

    /**
     * Поздний магнит 7×9×7, второй тир: плита со срезанными углами, на ней постамент с ядром и четыре стойки
     * по углам, тонкий ствол со стеклом и хребтом механизмов, крестовина, голова 3×3 механизмов под стеклянным
     * куполом, корона из зубцов. Плита выступает перед ядром на два блока. 13 механизмов.
     * Форма — постройка юзера, снятая жезлом (2026-10-04), со стойками из варианта V3.
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
