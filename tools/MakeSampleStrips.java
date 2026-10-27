import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Renders synthetic medicine-strip photos for demos and tests. Every image is watermarked as synthetic.
 * Usage: java tools/MakeSampleStrips.java src/main/resources/static/samples
 */
public class MakeSampleStrips {

    record Strip(String file, String brand, String generic, String batch, String mfg, String exp, String maker) {
    }

    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : "samples");
        out.mkdirs();
        Strip[] strips = {
                // Real batch from CDSCO's January 2026 NSQ alert, used to demonstrate a match.
                new Strip("flagged-pantoprazole.jpg", "Pansid-40 Tablets", "Pantoprazole Gastro-resistant Tablets IP 40 mg",
                        "TG25-1814A", "OCT.2025", "SEP.2027", "Athens Life Sciences, Kala Amb (H.P.)"),
                // Fictional product, not on any list.
                new Strip("safe-paracetamol.jpg", "Demopar 500", "Paracetamol Tablets IP 500 mg",
                        "DG26B047", "JUN.2026", "MAY.2028", "Example Pharma Ltd. (fictional)"),
                // Fictional product, past its expiry date.
                new Strip("expired-cetirizine.jpg", "Democet 10", "Cetirizine Hydrochloride Tablets IP 10 mg",
                        "DC23K201", "NOV.2023", "OCT.2025", "Example Pharma Ltd. (fictional)"),
        };
        for (Strip s : strips) {
            ImageIO.write(render(s), "jpg", new File(out, s.file()));
            System.out.println("wrote " + new File(out, s.file()));
        }
    }

    static BufferedImage render(Strip s) {
        int w = 1400, h = 820;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Table surface.
        g.setPaint(new GradientPaint(0, 0, new Color(122, 98, 78), w, h, new Color(92, 72, 58)));
        g.fillRect(0, 0, w, h);

        // Slightly rotated foil strip, like a hand-held photo.
        AffineTransform base = g.getTransform();
        g.rotate(Math.toRadians(-3.5), w / 2.0, h / 2.0);
        int sx = 110, sy = 120, sw = 1180, sh = 580;
        g.setPaint(new GradientPaint(sx, sy, new Color(214, 218, 224), sx + sw, sy + sh, new Color(168, 174, 184)));
        g.fillRoundRect(sx, sy, sw, sh, 36, 36);
        g.setColor(new Color(140, 146, 156));
        g.setStroke(new BasicStroke(3));
        g.drawRoundRect(sx, sy, sw, sh, 36, 36);

        // Blister pockets on the left.
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 2; col++) {
                int cx = sx + 70 + col * 170, cy = sy + 90 + row * 230;
                g.setPaint(new GradientPaint(cx, cy, new Color(236, 238, 242), cx + 140, cy + 140, new Color(150, 156, 166)));
                g.fillOval(cx, cy, 140, 170);
                g.setColor(new Color(120, 126, 136));
                g.drawOval(cx, cy, 140, 170);
            }
        }

        // Printed text on the foil.
        int tx = sx + 430;
        g.setColor(new Color(30, 60, 140));
        g.setFont(new Font("SansSerif", Font.BOLD, 54));
        g.drawString(s.brand(), tx, sy + 100);
        g.setColor(new Color(40, 40, 48));
        g.setFont(new Font("SansSerif", Font.PLAIN, 27));
        drawWrapped(g, s.generic(), tx, sy + 150, 700, 34);
        g.setFont(new Font("Monospaced", Font.BOLD, 34));
        g.drawString("B.No. : " + s.batch(), tx, sy + 290);
        g.drawString("Mfg.  : " + s.mfg(), tx, sy + 340);
        g.drawString("Exp.  : " + s.exp(), tx, sy + 390);
        g.setFont(new Font("SansSerif", Font.PLAIN, 22));
        g.drawString("Mfd. by: " + s.maker(), tx, sy + 450);
        g.drawString("Store below 30°C. Keep out of reach of children.", tx, sy + 490);
        g.setTransform(base);

        // Watermark.
        g.rotate(Math.toRadians(-18), w / 2.0, h / 2.0);
        g.setColor(new Color(200, 30, 30, 70));
        g.setFont(new Font("SansSerif", Font.BOLD, 64));
        g.drawString("SYNTHETIC TEST IMAGE", 300, h / 2 + 20);
        g.setTransform(base);
        g.setColor(new Color(255, 255, 255, 200));
        g.setFont(new Font("SansSerif", Font.PLAIN, 20));
        g.drawString("DawaGuard demo sample - not a real product photo", 24, h - 24);
        g.dispose();
        return img;
    }

    static void drawWrapped(Graphics2D g, String text, int x, int y, int maxWidth, int lineHeight) {
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (g.getFontMetrics().stringWidth(candidate) > maxWidth) {
                g.drawString(line.toString(), x, y);
                y += lineHeight;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        g.drawString(line.toString(), x, y);
    }
}
