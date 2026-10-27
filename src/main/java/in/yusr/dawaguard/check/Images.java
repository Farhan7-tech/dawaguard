package in.yusr.dawaguard.check;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/** Phone photos are often 4000px+; shrink them before sending to the vision model to save tokens and time. */
public final class Images {

    private static final int MAX_SIDE = 1600;

    private Images() {
    }

    public static String toDataUrl(byte[] bytes, String contentType) throws IOException {
        BufferedImage src = ImageIO.read(new ByteArrayInputStream(bytes));
        if (src == null) {
            // Format ImageIO can't decode (e.g. WebP): send as-is and let the model handle it.
            return "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(bytes);
        }
        double scale = Math.min(1.0, (double) MAX_SIDE / Math.max(src.getWidth(), src.getHeight()));
        int w = (int) Math.round(src.getWidth() * scale);
        int h = (int) Math.round(src.getHeight() * scale);
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
}
