package org.sunflow.image.writers;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.zip.Deflater;

import org.sunflow.image.BitmapWriter;
import org.sunflow.image.Color;
import org.sunflow.system.ByteUtil;
import org.sunflow.system.UI;
import org.sunflow.system.UI.Module;

public class EXRBitmapWriter implements BitmapWriter {

    private static final byte HALF = 1;
    private static final byte FLOAT = 2;
    private static final int HALF_SIZE = 2;
    private static final int FLOAT_SIZE = 4;
    private final static int OE_MAGIC = 20000630;
    private final static int OE_EXR_VERSION = 2;
    private final static int OE_TILED_FLAG = 0x00000200;
    private static final int NO_COMPRESSION = 0;
    private static final int RLE_COMPRESSION = 1;
    private static final int ZIP_COMPRESSION = 3;
    private static final int RLE_MIN_RUN = 3;
    private static final int RLE_MAX_RUN = 127;
    private String filename;
    private RandomAccessFile file;
    private long[][] tileOffsets;
    private long tileOffsetsPosition;
    private int tilesX;
    private int tilesY;
    private int tileSize;
    private int compression;
    private byte channelType;
    private int channelSize;
    private byte[] tmpbuf;
    private byte[] comprbuf;
    final String COMPRESSION = "compression";

    public EXRBitmapWriter() {
        // default settings
        configure(COMPRESSION, "zip");
        configure("channeltype", "half");
    }

    @Override
    public final void configure(final String option, String value) {
        if (option.equals(COMPRESSION)) {
            if (value.equals("none")) {
                compression = NO_COMPRESSION;
            } else if (value.equals("rle")) {
                compression = RLE_COMPRESSION;
            } else if (value.equals("zip")) {
                compression = ZIP_COMPRESSION;
            } else {
                UI.printWarning(Module.IMG, "EXR - Compression type was not recognized - defaulting to zip");
                compression = ZIP_COMPRESSION;
            }
        } else if (option.equals("channeltype")) {
            if (value.equals("float")) {
                channelType = FLOAT;
                channelSize = FLOAT_SIZE;
            } else if (value.equals("half")) {
                channelType = HALF;
                channelSize = HALF_SIZE;
            } else {
                UI.printWarning(Module.DISP, "EXR - Channel type was not recognized - defaulting to float");
                channelType = FLOAT;
                channelSize = FLOAT_SIZE;
            }
        }
    }

    @Override
    public void openFile(String filename) throws IOException {
        this.filename = filename == null ? "output.exr" : filename;
    }

    @Override
    public void writeHeader(int width, int height, int tileSize) throws IOException, UnsupportedOperationException {
        file = new RandomAccessFile(filename, "rw");
        file.setLength(0);
        if (tileSize <= 0) {
            throw new UnsupportedOperationException("Can't use OpenEXR bitmap writer without buckets.");
        }
        writeRGBAHeader(width, height, tileSize);
    }

    @Override
    public void writeTile(int x, int y, int w, int h, Color[] color, float[] alpha) throws IOException {
        int tx = x / tileSize;
        int ty = y / tileSize;
        writeEXRTile(tx, ty, w, h, color, alpha);
    }

    @Override
    public void closeFile() throws IOException {
        writeTileOffsets();
        file.close();
    }

    private void writeRGBAHeader(int w, int h, int tileSize) throws IOException {
        byte[] chanOut = {0, channelType, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1,
            0, 0, 0};

        file.write(ByteUtil.get4Bytes(OE_MAGIC));

        file.write(ByteUtil.get4Bytes(OE_EXR_VERSION | OE_TILED_FLAG));

        file.write("channels".getBytes());
        file.write(0);
        file.write("chlist".getBytes());
        file.write(0);
        file.write(ByteUtil.get4Bytes(73));
        file.write("R".getBytes());
        file.write(chanOut);
        file.write("G".getBytes());
        file.write(chanOut);
        file.write("B".getBytes());
        file.write(chanOut);
        file.write("A".getBytes());
        file.write(chanOut);
        file.write(0);

        // compression
        file.write(COMPRESSION.getBytes());
        file.write(0);
        file.write(COMPRESSION.getBytes());
        file.write(0);
        file.write(1);
        file.write(ByteUtil.get4BytesInv(compression));

        // datawindow =~ image size
        file.write("dataWindow".getBytes());
        file.write(0);
        file.write("box2i".getBytes());
        file.write(0);
        file.write(ByteUtil.get4Bytes(0x10));
        file.write(ByteUtil.get4Bytes(0));
        file.write(ByteUtil.get4Bytes(0));
        file.write(ByteUtil.get4Bytes(w - 1));
        file.write(ByteUtil.get4Bytes(h - 1));

        // dispwindow -> look at openexr.com for more info
        file.write("displayWindow".getBytes());
        file.write(0);
        file.write("box2i".getBytes());
        file.write(0);
        file.write(ByteUtil.get4Bytes(0x10));
        file.write(ByteUtil.get4Bytes(0));
        file.write(ByteUtil.get4Bytes(0));
        file.write(ByteUtil.get4Bytes(w - 1));
        file.write(ByteUtil.get4Bytes(h - 1));

        /*
         * lines in increasing y order = 0 decreasing would be 1
         */
        file.write("lineOrder".getBytes());
        file.write(0);
        file.write("lineOrder".getBytes());
        file.write(0);
        file.write(1);
        file.write(ByteUtil.get4BytesInv(2));

        file.write("pixelAspectRatio".getBytes());
        file.write(0);
        file.write("float".getBytes());
        file.write(0);
        file.write(ByteUtil.get4Bytes(4));
        file.write(ByteUtil.get4Bytes(Float.floatToIntBits(1)));

        // meaningless to a flat (2D) image
        file.write("screenWindowCenter".getBytes());
        file.write(0);
        file.write("v2f".getBytes());
        file.write(0);
        file.write(ByteUtil.get4Bytes(8));
        file.write(ByteUtil.get4Bytes(Float.floatToIntBits(0)));
        file.write(ByteUtil.get4Bytes(Float.floatToIntBits(0)));

        // meaningless to a flat (2D) image
        file.write("screenWindowWidth".getBytes());
        file.write(0);
        file.write("float".getBytes());
        file.write(0);
        file.write(ByteUtil.get4Bytes(4));
        file.write(ByteUtil.get4Bytes(Float.floatToIntBits(1)));

        this.tileSize = tileSize;

        tilesX = ((w + tileSize - 1) / tileSize);
        tilesY = ((h + tileSize - 1) / tileSize);

        /*
         * twice the space for the compressing buffer, as for ex. the compressor
         * can actually increase the size of the data :) If that happens though,
         * it is not saved into the file, but discarded
         */
        tmpbuf = new byte[tileSize * tileSize * channelSize * 4];
        comprbuf = new byte[tileSize * tileSize * channelSize * 4 * 2];

        tileOffsets = new long[tilesX][tilesY];

        file.write("tiles".getBytes());
        file.write(0);
        file.write("tiledesc".getBytes());
        file.write(0);
        file.write(ByteUtil.get4Bytes(9));

        file.write(ByteUtil.get4Bytes(tileSize));
        file.write(ByteUtil.get4Bytes(tileSize));

        // ONE_LEVEL tiles, ROUNDING_MODE = not important
        file.write(0);

        // an attribute with a name of 0 to end the list
        file.write(0);

        // save a pointer to where the tileOffsets are stored and write dummy
        // fillers for now
        tileOffsetsPosition = file.getFilePointer();
        writeTileOffsets();
    }

    private void writeTileOffsets() throws IOException {
        file.seek(tileOffsetsPosition);
        for (int ty = 0; ty < tilesY; ty++) {
            for (int tx = 0; tx < tilesX; tx++) {
                file.write(ByteUtil.get8Bytes(tileOffsets[tx][ty]));
            }
        }
    }

    private synchronized void writeEXRTile(int tileX, int tileY, int w, int h, Color[] tile, float[] alpha) throws IOException {
        byte[] rgb;

        // setting comprSize to max integer so without compression things
        // don't go awry
        int pixptr = 0, writeSize = 0, comprSize = Integer.MAX_VALUE;
        int tileRangeX = (tileSize < w) ? tileSize : w;
        int tileRangeY = (tileSize < h) ? tileSize : h;
        int channelBase = tileRangeX * channelSize;

        // lets see if the alignment matches, you can comment this out if
        // need be
        if ((tileSize != tileRangeX) && (tileX == 0)) {
            System.out.print(" bad X alignment ");
        }
        if ((tileSize != tileRangeY) && (tileY == 0)) {
            System.out.print(" bad Y alignment ");
        }

        tileOffsets[tileX][tileY] = file.getFilePointer();

        // the tile header: tile's x&y coordinate, levels x&y coordinate and
        // tilesize
        file.write(ByteUtil.get4Bytes(tileX));
        file.write(ByteUtil.get4Bytes(tileY));
        file.write(ByteUtil.get4Bytes(0));
        file.write(ByteUtil.get4Bytes(0));

        // just in case
        Arrays.fill(tmpbuf, (byte) 0);

        for (int ty = 0; ty < tileRangeY; ty++) {
            for (int tx = 0; tx < tileRangeX; tx++) {
                float[] rgbf = tile[tx + ty * tileRangeX].getRGB();
                if (channelType == FLOAT) {
                    rgb = ByteUtil.get4Bytes(Float.floatToRawIntBits(alpha[tx + ty * tileRangeX]));
                    tmpbuf[pixptr + 0] = rgb[0];
                    tmpbuf[pixptr + 1] = rgb[1];
                    tmpbuf[pixptr + 2] = rgb[2];
                    tmpbuf[pixptr + 3] = rgb[3];
                } else if (channelType == HALF) {
                    rgb = ByteUtil.get2Bytes(ByteUtil.floatToHalf(alpha[tx + ty * tileRangeX]));
                    tmpbuf[pixptr + 0] = rgb[0];
                    tmpbuf[pixptr + 1] = rgb[1];
                }
                for (int component = 1; component <= 3; component++) {
                    if (channelType == FLOAT) {
                        rgb = ByteUtil.get4Bytes(Float.floatToRawIntBits(rgbf[3 - component]));
                        tmpbuf[(channelBase * component) + pixptr + 0] = rgb[0];
                        tmpbuf[(channelBase * component) + pixptr + 1] = rgb[1];
                        tmpbuf[(channelBase * component) + pixptr + 2] = rgb[2];
                        tmpbuf[(channelBase * component) + pixptr + 3] = rgb[3];
                    } else if (channelType == HALF) {
                        rgb = ByteUtil.get2Bytes(ByteUtil.floatToHalf(rgbf[3 - component]));
                        tmpbuf[(channelBase * component) + pixptr + 0] = rgb[0];
                        tmpbuf[(channelBase * component) + pixptr + 1] = rgb[1];
                    }
                }
                pixptr += channelSize;
            }
            pixptr += (tileRangeX * channelSize * 3);
        }

        writeSize = tileRangeX * tileRangeY * channelSize * 4;

        if (compression != NO_COMPRESSION) {
            comprSize = compress(compression, tmpbuf, writeSize, comprbuf);
        }

        // lastly, write the size of the tile and the tile itself
        // (compressed or not)
        if (comprSize < writeSize) {
            file.write(ByteUtil.get4Bytes(comprSize));
            file.write(comprbuf, 0, comprSize);
        } else {
            file.write(ByteUtil.get4Bytes(writeSize));
            file.write(tmpbuf, 0, writeSize);
        }
    }

    private static int compress(int tp, byte[] in, int inSize, byte[] out) {
        if (inSize == 0) {
            return 0;
        }

        int t1 = 0, t2 = (inSize + 1) / 2;
        int inPtr = 0, ret;
        byte[] tmp = new byte[inSize];

        // zip and rle treat the data first, in the same way so I'm not
        // repeating the code
        if ((tp == ZIP_COMPRESSION) || (tp == RLE_COMPRESSION)) {
            // reorder the pixel data ~ straight from ImfZipCompressor.cpp :)
            while (true) {
                if (inPtr < inSize) {
                    tmp[t1++] = in[inPtr++];
                } else {
                    break;
                }

                if (inPtr < inSize) {
                    tmp[t2++] = in[inPtr++];
                } else {
                    break;
                }
            }

            // Predictor ~ straight from ImfZipCompressor.cpp :)
            t1 = 1;
            int p = tmp[t1 - 1];
            while (t1 < inSize) {
                int d = tmp[t1] - p + (128 + 256);
                p = tmp[t1];
                tmp[t1] = (byte) d;
                t1++;
            }
        }

        // We'll just jump from here to the wanted compress/decompress stuff if
        // need be
        switch (tp) {
            case ZIP_COMPRESSION:
                Deflater def = new Deflater(Deflater.DEFAULT_COMPRESSION, false);
                def.setInput(tmp, 0, inSize);
                def.finish();
                ret = def.deflate(out);
                return ret;
            case RLE_COMPRESSION:
                return rleCompress(tmp, inSize, out);
            default:
                return -1;
        }
    }

    private static int rleCompress(byte[] in, int inLen, byte[] out) {
        int runStart = 0, runEnd = 1, outWrite = 0;
        while (runStart < inLen) {
            while (runEnd < inLen && in[runStart] == in[runEnd] && (runEnd - runStart - 1) < RLE_MAX_RUN) {
                runEnd++;
            }
            if (runEnd - runStart >= RLE_MIN_RUN) {
                // Compressable run
                out[outWrite++] = (byte) ((runEnd - runStart) - 1);
                out[outWrite++] = in[runStart];
                runStart = runEnd;
            } else {
                // Uncompressable run
                while (runEnd < inLen && (((runEnd + 1) >= inLen || in[runEnd] != in[runEnd + 1]) || ((runEnd + 2) >= inLen || in[runEnd + 1] != in[runEnd + 2])) && (runEnd - runStart) < RLE_MAX_RUN) {
                    runEnd++;
                }
                out[outWrite++] = (byte) (runStart - runEnd);
                while (runStart < runEnd) {
                    out[outWrite++] = in[runStart++];
                }
            }
            runEnd++;
        }
        return outWrite;
    }
}