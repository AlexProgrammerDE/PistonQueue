package net.pistonmaster.pistonqueue.shared.loadbalance;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal 1.7+ Server List Ping (SLP) client to get players.online.
 * Not fully spec-compliant; intended for quick stats with short timeouts.
 */
public final class MinecraftStatusPinger {
  private MinecraftStatusPinger() {}

  private static final Pattern ONLINE_PATTERN = Pattern.compile("\\\"players\\\"\\s*:\\s*\\{[^}]*\\\"online\\\"\\s*:\\s*(\\\\d+)");

  public static Integer getOnlinePlayers(String host, int port, int timeoutMs) {
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress(host, port), timeoutMs);
      socket.setSoTimeout(timeoutMs);

      ByteArrayOutputStream handshake = new ByteArrayOutputStream();
      DataOutputStream out = new DataOutputStream(handshake);
      // Packet ID 0x00 (handshake)
      writeVarInt(out, 0x00);
      // Protocol version (use 47 for compatibility)
      writeVarInt(out, 47);
      writeString(out, host);
      out.writeShort((short) port);
      // Next state: status (1)
      writeVarInt(out, 1);

      // Write handshake packet with length prefix
      DataOutputStream socketOut = new DataOutputStream(socket.getOutputStream());
      writeVarInt(socketOut, handshake.size());
      socketOut.write(handshake.toByteArray());

      // Send status request (packet 0x00)
      ByteArrayOutputStream request = new ByteArrayOutputStream();
      DataOutputStream reqOut = new DataOutputStream(request);
      writeVarInt(reqOut, 0x00);
      writeVarInt(socketOut, request.size());
      socketOut.write(request.toByteArray());

      // Read response
      DataInputStream in = new DataInputStream(socket.getInputStream());
      int size = readVarInt(in);
      if (size < 10) return null;
      int packetId = readVarInt(in);
      if (packetId != 0x00) return null;
      int strLen = readVarInt(in);
      byte[] data = in.readNBytes(strLen);
      String json = new String(data, StandardCharsets.UTF_8);

      Matcher m = ONLINE_PATTERN.matcher(json);
      if (m.find()) {
        return Integer.parseInt(m.group(1));
      }
      return null;
    } catch (IOException e) {
      return null;
    }
  }

  private static void writeString(DataOutputStream out, String s) throws IOException {
    byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
    writeVarInt(out, bytes.length);
    out.write(bytes);
  }

  private static void writeVarInt(DataOutputStream out, int paramInt) throws IOException {
    while (true) {
      if ((paramInt & 0xFFFFFF80) == 0) {
        out.writeByte(paramInt);
        return;
      }
      out.writeByte(paramInt & 0x7F | 0x80);
      paramInt >>>= 7;
    }
  }

  private static int readVarInt(DataInputStream in) throws IOException {
    int i = 0;
    int j = 0;
    while (true) {
      int k = in.readByte();
      i |= (k & 0x7F) << j++;
      if (j > 5) throw new RuntimeException("VarInt too big");
      if ((k & 0x80) != 128) break;
    }
    return i;
  }
}
