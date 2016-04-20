package com.nao20010128nao.Wisecraft.misc.pinger;

import java.io.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class Utils {

	public static byte PACKET_HANDSHAKE = 0x00, PACKET_STATUSREQUEST = 0x00,
	PACKET_PING = 0x01;
	public static int PROTOCOL_VERSION = 4;
	public static int STATUS_HANDSHAKE = 1;

	public static void validate(final Object o, final String m) {
		if (o == null)
			throw new RuntimeException(m);
	}

	public static void io(final boolean b, final String m) throws IOException {
		if (b)
			throw new IOException(m);
	}

	public static int readVarInt(DataInputStream in) throws IOException {
		int i = 0;
		int j = 0;
		while (true) {
			int k = in.readByte();

			i |= (k & 0x7F) << j++ * 7;

			if (j > 5)
				throw new RuntimeException("VarInt too big");

			if ((k & 0x80) != 128)
				break;
		}

		return i;
	}

	public static void writeVarInt(DataOutputStream out, int paramInt)
	throws IOException {
		while (true) {
			if ((paramInt & 0xFFFFFF80) == 0) {
				out.writeByte(paramInt);
				return;
			}

			out.writeByte(paramInt & 0x7F | 0x80);
			paramInt >>>= 7;
		}
	}


	public static byte[] subarray(byte[] in, int a, int b) {
		if (b - a > in.length)
			return in;
		byte[] out = new byte[(b - a) + 1];
		for (int i = a; i <= b; i++)
			out[i - a] = in[i];
		return out;
	}

	public static byte[] trim(byte[] arr) {
		if (arr[0] != 0 && arr[arr.length - 1] != 0)
			return arr;

		int begin = 0, end = arr.length;
		for (int i = 0; i < arr.length; i++) {
			if (arr[i] != 0) {
				begin = i;
				break;
			}
		}
		for (int i = arr.length - 1; i >= 0; i--) {
			if (arr[i] != 0) {
				end = i;
				break;
			}
		}

		return subarray(arr, begin, end);
	}

	public static byte[][] split(byte[] input) {
		ArrayList<byte[]> temp = new ArrayList<byte[]>();

		byte[][] output;
		output = new byte[input.length][input.length];

		int index_cache = 0;
		for (int i = 0; i < input.length; i++) {
			if (input[i] == 0x00) {
				byte[] b = subarray(input, index_cache, i - 1);
				temp.add(b);
				index_cache = i + 1;
			}
		}

		if (index_cache != 0) {
			byte[] b = subarray(input, index_cache, input.length - 1);
			temp.add(b);
		}

		output = new byte[temp.size()][input.length];
		for (int i = 0; i < temp.size(); i++) {
			output[i] = temp.get(i);
		}

		return output;
	}

	public static byte[] padArrayEnd(byte[] arr, int amount) {
		byte[] arr2 = new byte[arr.length + amount];
		for (int i = 0; i < arr.length; i++)
			arr2[i] = arr[i];
		for (int i = arr.length; i < arr2.length; i++)
			arr2[i] = 0;
		return arr2;
	}

	public static short bytesToShort(byte[] b) {
		ByteBuffer buf = ByteBuffer.wrap(b, 0, 2);
		buf.order(ByteOrder.LITTLE_ENDIAN);
		return buf.getShort();
	}

	public static byte[] intToBytes(int in) {
		byte[] b;
		b = new byte[] { (byte) (in >>> 24 & 0xFF), (byte) (in >>> 16 & 0xFF),
			(byte) (in >>> 8 & 0xFF), (byte) (in >>> 0 & 0xFF) };
		return b;
	}

	public static int bytesToInt(byte[] in) {
		return ByteBuffer.wrap(in).getInt();
	}
}