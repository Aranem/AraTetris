package com.aratetris.engine

/**
 * The seven tetrominoes. [id] is the non-zero value stored in board cells (and used by the renderer
 * to pick a color). [rgb] is the canonical Guideline color as 0xRRGGBB (the engine stays free of any
 * graphics dependency; the renderer converts this to its own color type). [boxSize] is the side of
 * the square bounding box used for SRS rotation (4 for I, 2 for O, 3 for the rest).
 */
enum class PieceType(val id: Int, val rgb: Int, val boxSize: Int) {
    I(1, 0x2FBFBF, 4),
    O(2, 0xE0C040, 2),
    T(3, 0xA040C0, 3),
    S(4, 0x40C040, 3),
    Z(5, 0xE05050, 3),
    J(6, 0x4060E0, 3),
    L(7, 0xE0902F, 3);

    companion object {
        fun fromId(id: Int): PieceType? = entries.firstOrNull { it.id == id }
    }
}
