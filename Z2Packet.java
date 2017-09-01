import java.util.Comparator;

public class Z2Packet implements Comparable<Z2Packet>
{
	// PAKIET PRZESYLANY W DATAGRAMIE
	byte[] data;
	public int id;

	// TWORZY PUSTY PAKIET
	public Z2Packet(int size)
	{
		data = new byte[size];
	}

	// TWORZY PAKIET ZAWIERAJACY CIAG BAJTOW b
	public Z2Packet(byte[] b)
	{
		data = b;
	}
	
	// ZAPISUJE LICZBE CALKOWITA value JAKO 4 BAJTY OD POZYCJI idx
	public void setIntAt(int value, int idx)
	{
		
		data[idx] = (byte) ((value >> 24) & 0xFF);
		data[idx + 1] = (byte) ((value >> 16) & 0xFF);
		data[idx + 2] = (byte) ((value >> 8) & 0xFF);
		data[idx + 3] = (byte) ((value) & 0xFF);
		
	}
	
	// ODCZYTUJE LICZBE CALKOWITA NA 4 BAJTACH OD POZYCJI idx
	public int getIntAt(int idx)
	{
		int x;
		x = (((int) data[idx]) & 0xFF) << 24;
		x |= (((int) data[idx + 1]) & 0xFF) << 16;
		x |= (((int) data[idx + 2]) & 0xFF) << 8;
		x |= (((int) data[idx + 3]) & 0xFF);
		return x;
	}

	@Override
	public int compareTo(Z2Packet packet)
	{	
		if(getIntAt(0) < packet.getIntAt(0))
			return -1;
		
		if(getIntAt(0) > packet.getIntAt(0))
			return 1;

		return 0;
	}

}
