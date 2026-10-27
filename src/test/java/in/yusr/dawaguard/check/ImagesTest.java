package in.yusr.dawaguard.check;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.zip.CRC32;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImagesTest {

    @Test
    void rejectsImagesThatClaimHugeDimensionsWithoutDecodingThem() throws IOException {
        // A PNG header that claims 60000 x 60000 pixels: tiny file, ~14 GB if decoded.
        byte[] bomb = pngHeader(60_000, 60_000);
        assertThatThrownBy(() -> Images.toDataUrl(bomb, "image/png"))
                .isInstanceOf(Images.ImageTooLargeException.class);
    }

    @Test
    void shrinksLargePhotosToTheModelSize() throws IOException {
        byte[] photo = jpeg(4000, 3000);
        String dataUrl = Images.toDataUrl(photo, "image/jpeg");

        assertThat(dataUrl).startsWith("data:image/jpeg;base64,");
        BufferedImage out = ImageIO.read(new ByteArrayInputStream(
                Base64.getDecoder().decode(dataUrl.substring(dataUrl.indexOf(',') + 1))));
        assertThat(Math.max(out.getWidth(), out.getHeight())).isEqualTo(1600);
    }

    @Test
    void passesThroughFormatsItCannotDecode() throws IOException {
        byte[] unknown = "not an image ImageIO knows".getBytes();
        assertThat(Images.toDataUrl(unknown, "image/webp")).startsWith("data:image/webp;base64,");
    }

    private static byte[] jpeg(int w, int h) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB), "jpg", out);
        return out.toByteArray();
    }

    /** PNG signature plus a valid IHDR chunk; enough for a reader to report dimensions. */
    private static byte[] pngHeader(int width, int height) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.write(new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'});
        ByteArrayOutputStream chunk = new ByteArrayOutputStream();
        DataOutputStream c = new DataOutputStream(chunk);
        c.writeBytes("IHDR");
        c.writeInt(width);
        c.writeInt(height);
        c.write(new byte[]{8, 2, 0, 0, 0});
        byte[] typeAndData = chunk.toByteArray();
        CRC32 crc = new CRC32();
        crc.update(typeAndData);
        out.writeInt(typeAndData.length - 4);
        out.write(typeAndData);
        out.writeInt((int) crc.getValue());
        return bytes.toByteArray();
    }
}
