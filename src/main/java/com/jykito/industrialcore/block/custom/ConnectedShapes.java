package com.jykito.industrialcore.block.custom;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

/**
 * Формы соединяющихся блоков (кабель, теплотруба, хаб): куб по центру плюс отросток к каждой
 * соединённой грани.
 * <p>
 * getShape зовут коллизии, луч прицела, пасфайндинг и обновления соседей, то есть очень часто,
 * а собирать форму через Shapes.or каждый раз дорого. Комбинаций всего 64, поэтому все они
 * считаются один раз при загрузке класса блока.
 * <p>
 * Размеры в пикселях блока (0..16). Отростки симметричны: сечение {@code armMin..armMax},
 * длина от грани блока внутрь {@code armLen}.
 */
public final class ConnectedShapes {

    private final VoxelShape[] shapes = new VoxelShape[64];

    public ConnectedShapes(double coreMin, double coreMax, double armMin, double armMax, double armLen) {
        this(coreMin, coreMax, armMin, armMax, armLen, coreMin, coreMax);
    }

    /**
     * С отдельным узлом: если соединения образуют прямой участок (ровно две противоположные стороны),
     * центр — {@code coreMin..coreMax}, иначе (развилка, угол, конец, одиночный) — узел {@code hubMin..hubMax}.
     */
    public ConnectedShapes(double coreMin, double coreMax, double armMin, double armMax, double armLen, double hubMin, double hubMax) {
        VoxelShape core = Block.box(coreMin, coreMin, coreMin, coreMax, coreMax, coreMax);
        VoxelShape hub = Block.box(hubMin, hubMin, hubMin, hubMax, hubMax, hubMax);
        VoxelShape[] arms = new VoxelShape[6]; // индекс = Direction.get3DDataValue()
        arms[Direction.DOWN.get3DDataValue()]  = Block.box(armMin, 0, armMin, armMax, armLen, armMax);
        arms[Direction.UP.get3DDataValue()]    = Block.box(armMin, 16 - armLen, armMin, armMax, 16, armMax);
        arms[Direction.NORTH.get3DDataValue()] = Block.box(armMin, armMin, 0, armMax, armMax, armLen);
        arms[Direction.SOUTH.get3DDataValue()] = Block.box(armMin, armMin, 16 - armLen, armMax, armMax, 16);
        arms[Direction.WEST.get3DDataValue()]  = Block.box(0, armMin, armMin, armLen, armMax, armMax);
        arms[Direction.EAST.get3DDataValue()]  = Block.box(16 - armLen, armMin, armMin, 16, armMax, armMax);

        for (int mask = 0; mask < shapes.length; mask++) {
            VoxelShape shape = isStraight(mask) ? core : hub;
            for (int i = 0; i < 6; i++)
                if ((mask & (1 << i)) != 0) shape = Shapes.or(shape, arms[i]);
            shapes[mask] = shape.optimize();   // разово упрощаем, все последующие проверки дешевле
        }
    }

    /** Ровно две противоположные стороны: низ+верх, север+юг или запад+восток (биты по get3DDataValue). */
    private static boolean isStraight(int mask) {
        return mask == 0b000011 || mask == 0b001100 || mask == 0b110000;
    }

    /** Форма по соединениям в blockstate. Прочие свойства (нагрев, вода) в маску не входят. */
    public VoxelShape get(BlockState state, Map<Direction, BooleanProperty> connections) {
        int mask = 0;
        for (Map.Entry<Direction, BooleanProperty> e : connections.entrySet())
            if (state.getValue(e.getValue())) mask |= 1 << e.getKey().get3DDataValue();
        return shapes[mask];
    }
}
