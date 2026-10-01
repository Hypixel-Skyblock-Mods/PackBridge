package org.hypixelskyblockmods.packbridge;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

public final class ImageTransforms {
    private ImageTransforms() {}

    public static BufferedImage read(InputStream input) throws IOException {
        try (input; ImageInputStream stream = ImageIO.createImageInputStream(input)) {
            if (stream == null) throw new IOException("Cannot read texture");
            var readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw new IOException("Unsupported texture image");
            ImageReader reader = readers.next();
            try {
                reader.setInput(stream);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > 16384 || height > 16384
                        || (long) width * height > 16_777_216) {
                    throw new IOException("Texture dimensions exceed conversion limit");
                }
                return reader.read(0);
            } finally {
                reader.dispose();
            }
        }
    }

    public static byte[] png(BufferedImage image) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "PNG", output)) throw new IOException("PNG encoder unavailable");
        return output.toByteArray();
    }

    public static BufferedImage crop(BufferedImage sheet, SpriteSlices.Slice slice) throws IOException {
        int scale = sheet.getWidth() / slice.sheetWidth();
        if (scale < 1 || sheet.getWidth() != scale * slice.sheetWidth()
                || sheet.getHeight() != scale * slice.sheetHeight()) {
            throw new IOException("Unexpected sprite sheet dimensions");
        }
        return sheet.getSubimage(slice.x() * scale, slice.y() * scale,
                slice.width() * scale, slice.height() * scale);
    }

    /** Align the pre-1.9 crafting artwork with the current inventory's real slots. */
    public static BufferedImage inventoryCrafting(BufferedImage source) throws IOException {
        int scale = source.getWidth() / 256;
        if (scale < 1 || source.getWidth() != scale * 256 || source.getHeight() != scale * 256) {
            throw new IOException("Unexpected inventory texture dimensions");
        }
        int size = 256 * scale;
        BufferedImage result = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        result.setRGB(0, 0, size, size, source.getRGB(0, 0, size, size, null, 0, size), 0, size);

        // Old input slots start at (88, 26), the output at (144, 36).
        // Both moved by (+10, -8) in 1.9. Include their borders and crafting arrow.
        int x = 86 * scale, y = 24 * scale, width = 76 * scale, height = 38 * scale;
        // The one-pixel gutter immediately left of the artwork is plain background.
        // Extend it across the old position, retaining alpha and vertical shading.
        for (int row = y; row < y + height; row++) {
            for (int column = x; column < x + width; column++) {
                result.setRGB(column, row, source.getRGB(x + (column - x) % scale, row));
            }
        }
        // Read from the original so overlapping source/destination areas cannot smear.
        result.setRGB(x + 10 * scale, y - 8 * scale, width, height,
                source.getRGB(x, y, width, height, null, 0, width), 0, width);
        return result;
    }

    /** Repack the pre-1.15 cube faces into the current chest model's UV layout. */
    public static BufferedImage chest(BufferedImage source, String half) throws IOException {
        int scale = source.getHeight() / 64;
        boolean single = half.equals("single");
        if (scale < 1 || source.getHeight() != scale * 64
                || source.getWidth() != scale * (single ? 64 : 128)) {
            throw new IOException("Unexpected chest texture dimensions");
        }
        BufferedImage result = new BufferedImage(64 * scale, 64 * scale, BufferedImage.TYPE_INT_ARGB);
        // Each row describes a face: source x, destination x, width.
        int[][] tops = switch (half) {
            case "left" -> new int[][]{{29, 29, 15}, {59, 14, 15}};
            case "right" -> new int[][]{{14, 29, 15}, {44, 14, 15}};
            default -> new int[][]{{14, 28, 14}, {28, 14, 14}};
        };
        int[][] sides = switch (half) {
            case "left" -> new int[][]{{29, 43, 15}, {44, 29, 14}, {58, 14, 15}};
            case "right" -> new int[][]{{0, 0, 14}, {14, 43, 15}, {73, 14, 15}};
            default -> new int[][]{{0, 0, 14}, {14, 42, 14}, {28, 28, 14}, {42, 14, 14}};
        };
        for (int y : new int[]{0, 19}) {
            for (int[] face : tops) copy(source, result, face[0], y, face[1], y, face[2], 14, scale, false);
        }
        for (int[] part : new int[][]{{14, 5}, {33, 10}}) {
            for (int[] face : sides) copy(source, result, face[0], part[0], face[1], part[0], face[2], part[1], scale, true);
        }
        int[][] lockTops = switch (half) {
            case "left" -> new int[][]{{2, 2, 1}, {4, 1, 1}};
            case "right" -> new int[][]{{1, 2, 1}, {3, 1, 1}};
            default -> new int[][]{{1, 3, 2}, {3, 1, 2}};
        };
        int[][] lockSides = switch (half) {
            case "left" -> new int[][]{{2, 3, 1}, {3, 2, 1}, {4, 1, 1}};
            case "right" -> new int[][]{{0, 0, 1}, {1, 3, 1}, {5, 1, 1}};
            default -> new int[][]{{0, 0, 1}, {1, 4, 2}, {3, 3, 1}, {4, 1, 2}};
        };
        for (int[] face : lockTops) copy(source, result, face[0], 0, face[1], 0, face[2], 1, scale, false);
        for (int[] face : lockSides) copy(source, result, face[0], 1, face[1], 1, face[2], 4, scale, true);
        return result;
    }

    private static void copy(BufferedImage source, BufferedImage target, int sx, int sy, int dx, int dy,
                             int width, int height, int scale, boolean mirrorX) {
        int w = width * scale, h = height * scale;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                target.setRGB(dx * scale + x, dy * scale + y,
                        source.getRGB(sx * scale + (mirrorX ? w - 1 - x : x), sy * scale + h - 1 - y));
            }
        }
    }
}
