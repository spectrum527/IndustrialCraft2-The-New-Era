package com.industrialage.ia2.content;

import java.util.List;

/** Physical and visual properties of a source fluid and its flowing variant, measured in-game. */
public record IndustrialFluid(
        String path,
        String still,
        String flow,
        int argb,
        int density,
        int viscosity,
        int temperature,
        int light,
        int tickDelay) {
    /** All source fluids, in stable registry order. */
    public static final List<IndustrialFluid> ALL =
            IndustrialMaterials.rows("industrial-fluids.tsv").stream()
                    .map(IndustrialFluid::parse)
                    .toList();

    private static IndustrialFluid parse(String line) {
        var values = line.split("\t");
        return new IndustrialFluid(
                values[0],
                values[1],
                values[2],
                Integer.parseUnsignedInt(values[3], 16),
                Integer.parseInt(values[4]),
                Integer.parseInt(values[5]),
                Integer.parseInt(values[6]),
                Integer.parseInt(values[7]),
                Integer.parseInt(values[8]));
    }
}
