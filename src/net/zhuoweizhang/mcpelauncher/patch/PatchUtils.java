package net.zhuoweizhang.mcpelauncher.patch;

import java.io.*;
import java.nio.*;

import com.mojang.minecraftpe.MainActivity;

import net.zhuoweizhang.mcpelauncher.MaraudersMap;
import net.zhuoweizhang.mcpelauncher.MinecraftVersion;

public final class PatchUtils {

    public static MinecraftVersion minecraftVersion = null;

    private PatchUtils() {}

    private static ByteBuffer positionBuf(ByteBuffer buf, int addr, int length) {
        if (buf == null) throw new IllegalStateException("Minecraft native buffer is unavailable.");
        ByteBuffer target = buf;
        if (buf == MainActivity.minecraftLibBuffer && addr >= 0 &&
                addr < MaraudersMap.minecraftTextBuffer.capacity()) {
            target = MaraudersMap.minecraftTextBuffer;
        }
        if (addr < 0 || length < 0 || addr > target.capacity() - length) {
            throw new IndexOutOfBoundsException(
                "Patch range is outside the mapped buffer: offset=" + addr +
                ", length=" + length + ", capacity=" + target.capacity());
        }
        target.position(addr);
        return target;
    }

    private static MinecraftVersion requireVersionProfile() {
        if (minecraftVersion == null) {
            throw new IllegalStateException("No exact Minecraft version profile is selected; refusing native patch.");
        }
        if (!MinecraftVersion.hasExactVersionCode(minecraftVersion.versionCode)) {
            throw new IllegalStateException("Unknown Minecraft version profile; refusing native patch.");
        }
        return minecraftVersion;
    }

    public static void patch(ByteBuffer buf, com.joshuahuelsman.patchtool.PTPatch patch) {
        MinecraftVersion profile = requireVersionProfile();
        MinecraftVersion.PatchTranslator translator = profile.translator;
        for (patch.count = 0; patch.count < patch.getNumPatches(); patch.count++) {
            int addr = patch.getNextAddr();
            if (translator != null) addr = translator.get(addr);
            byte[] data = patch.getNextData();
            positionBuf(buf, addr, data.length).put(data);
        }
    }

    public static void unpatch(ByteBuffer buf, byte[] original,
                               com.joshuahuelsman.patchtool.PTPatch patch) {
        MinecraftVersion profile = requireVersionProfile();
        if (original == null) throw new IllegalArgumentException("Original native image is missing.");
        MinecraftVersion.PatchTranslator translator = profile.translator;
        ByteBuffer originalBuf = ByteBuffer.wrap(original);
        for (patch.count = 0; patch.count < patch.getNumPatches(); patch.count++) {
            int addr = patch.getNextAddr();
            if (translator != null) addr = translator.get(addr);
            int length = patch.getDataLength();
            if (addr < 0 || length < 0 || addr > originalBuf.capacity() - length) {
                throw new IndexOutOfBoundsException(
                    "Original image does not contain patch range: offset=" + addr + ", length=" + length);
            }
            ByteBuffer target = positionBuf(buf, addr, length);
            originalBuf.position(addr);
            byte[] nextData = new byte[length];
            originalBuf.get(nextData);
            target.put(nextData);
        }
    }

    public static void copy(File from, File to) throws IOException {
        if (from == null || to == null) throw new IllegalArgumentException("Source and destination are required.");
        try (InputStream input = new BufferedInputStream(new FileInputStream(from));
             OutputStream output = new BufferedOutputStream(new FileOutputStream(to))) {
            byte[] buffer = new byte[32 * 1024];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
        }
    }

    public static boolean canLivePatch(File file) throws IOException {
        if (file == null || !file.isFile() || !file.canRead()) return false;
        if (minecraftVersion == null ||
                !MinecraftVersion.hasExactVersionCode(minecraftVersion.versionCode)) return false;
        // The old live-patch analyzer is incomplete; do not claim safety based only on file readability.
        return false;
    }

    public static byte[] createMovwInstr(int rd, int imm) {
        long instr = 0xf2400000L;
        instr |= (rd << 8);
        instr |= (imm & 0xff);
        instr |= ((imm >> 8) & 0x7) << 12;
        instr |= ((imm >> 11) & 0x1) << 26;
        instr |= ((imm >> 12) & 0xf) << 16;
        return intToLEByteArray(instr);
    }

    public static final byte[] intToLEByteArray(long value) {
        return new byte[] {
            (byte) (value >>> 16),
            (byte) (value >>> 24),
            (byte) value,
            (byte) (value >>> 8)
        };
    }
}
