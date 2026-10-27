package in.yusr.dawaguard.check;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Iterator;

/**
 * Phone photos are often 4000px+; shrink them before sending to the vision model to save tokens and time.
 * Dimensions are checked from the header before decoding, and decoding is subsampled, so a small file that
 * claims an enormous size (a "decompression bomb") can't exhaust memory.
 */
public final class Images {

    private static final int MAX_SIDE = 1600;
    static final int MAX_SOURCE_SIDE = 12_000;
    static final long MAX_SOURCE_PIXELS = 50_000_000L;

    private Images() {
    }

    /** Thrown when a photo is too large to process safely. */
    public static class ImageTooLargeException extends IllegalArgumentException {
        ImageTooLargeException(int width, int height) {
            super("Image is " + width + "x" + height + " pixels; the limit is " + MAX_SOURCE_SIDE
                    + " px per side and " + MAX_SOURCE_PIXELS / 1_000_000 + " MP");
        }
    }

    public static String toDataUrl(byte[] bytes, String contentType) throws IOException {
        BufferedImage src = decodeSafely(bytes);
        if (src == null) {
            // Format ImageIO can't decode (e.g. WebP): send as-is and let the model handle it.
            // The upload size limit (10 MB) still bounds what we forward.
            return "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(bytes);
        }
        double scale = Math.min(1.0, (double) MAX_SIDE / Math.max(src.getWidth(), src.getHeight()));
        int w = Math.max(1, (int) Math.round(src.getWidth() * scale));
        int h = Math.max(1, (int) Math.round(src.getHeight() * scale));
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();

        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try (MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(buf)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(0.9f);
            writer.write(null, new IIOImage(out, null, null), param);
        } finally {
            writer.dispose();
        }
        return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(buf.toByteArray());
    }

    /** Returns null if no ImageIO reader understands the format. */
    static BufferedImage decodeSafely(byte[] bytes) throws IOException {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (in == null) {
                return null;
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                return null;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(in, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > MAX_SOURCE_SIDE || height > MAX_SOURCE_SIDE
                        || (long) width * height > MAX_SOURCE_PIXELS) {
                    throw new ImageTooLargeException(width, height);
                }
                // Skip pixels while decoding so the full-resolution raster is never allocated.
                int step = Math.max(1, Math.max(width, height) / (MAX_SIDE * 2));
                ImageReadParam param = reader.getDefaultReadParam();
                param.setSourceSubsampling(step, step, 0, 0);
                return reader.read(0, param);
            } finally {
                reader.dispose();
            }
        }
    }
}
