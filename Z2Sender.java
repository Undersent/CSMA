import java.net.*;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.io.IOException;

class DatagramInfo
{
	int id;
	long time;
	Z2Packet packet;

	public DatagramInfo(int id,long time, Z2Packet packet)
	{
		this.id = id;
		this.time = time;
		this.packet = packet;
	}
}

class Z2Sender
{
	static final int datagramSize = 50;
	static final int sleepTime = 500;
	static final int maxPacket = 50;
	static final int confirmTime = 7000;
	static int sectorSize = 15;
	static int numberOfSendedMessages = 0;
	boolean isConfrimation = true;

	InetAddress localHost;
	int destinationPort;
	DatagramSocket socket;
	SenderThread sender;
	ReceiverThread receiver;
	ConfirmGuard guard;

	ConcurrentLinkedQueue <DatagramInfo> waitForConfirmFifo;

	public synchronized void sendPacket(DatagramPacket packet) throws IOException
	{
		socket.send(packet);
	}

	public Z2Sender(int myPort, int destPort) throws Exception
	{
		localHost = InetAddress.getByName("127.0.0.1");
		destinationPort = destPort;
		socket = new DatagramSocket(myPort);
		sender = new SenderThread();
		receiver = new ReceiverThread();
		guard = new ConfirmGuard();
		waitForConfirmFifo = new ConcurrentLinkedQueue<DatagramInfo>();
	}

	class SenderThread extends Thread
	{
		public void run()
		{
			int i, x;
			try
			{
				if(isConfrimation){
				for (i = 0; (x = System.in.read()) > 0; i++)
				{
					Z2Packet p = new Z2Packet(5); 	//1 + 4 ==  id + dane
					p.setIntAt(i, 0);				//ustawiamy wartosc id na bajcie 0
					p.data[4] = (byte) x;

					DatagramPacket packet = new DatagramPacket(p.data, p.data.length, localHost, destinationPort);
					sendPacket(packet);

					numberOfSendedMessages++;
					if(numberOfSendedMessages==15){
						isConfrimation=false;
						System.out.println("^^^^^PAUZA^^^^^");
					}
					System.out.println("S: [" + p.getIntAt(0) + "] " + (char)p.data[4]);

					DatagramInfo info = new DatagramInfo(i,System.currentTimeMillis(),p);

					// dodaj informacje o wyslanym pakiecie
					waitForConfirmFifo.add(info);

					sleep(sleepTime);
				}
			}
				while( ! waitForConfirmFifo.isEmpty() )
					sleep(sleepTime);

				System.out.println("Wiadomosc zostala poprawnie wyslana");
				System.out.println("Wyslane wiadomosci "+ numberOfSendedMessages);
			}
			catch (Exception e)
			{
				System.out.println("Z2Sender.SenderThread.run: " + e);
			}
		}
	}

	class ReceiverThread extends Thread
	{
		int expectedId = 0;
		PriorityQueue<Z2Packet> packetsQueue = new PriorityQueue<Z2Packet>();

		public void run()
		{
			try
			{
				while (true)
				{
					byte[] data = new byte[datagramSize];
					DatagramPacket packet = new DatagramPacket(data, datagramSize);

					//czekaj na odebranie pakietu
					socket.receive(packet);

					Z2Packet p = new Z2Packet(packet.getData());
					System.out.println("Sender otrzymal id "+ p.getIntAt(0) );
					isConfrimation=true;
					for( DatagramInfo info : waitForConfirmFifo)
						if( info.packet.compareTo(p) < 0 ||  info.packet.compareTo(p) == 0)
						{
							System.out.println("Usuwam z oczekiwania "+ info.id);
							waitForConfirmFifo.remove(info);
						}



				/*	while (! packetsQueue.isEmpty() && packetsQueue.peek().getIntAt(0) == expectedId)
					{

						Z2Packet expectedPacket = packetsQueue.poll();

						//usun powtorzenia tego pakietu
						while(! packetsQueue.isEmpty() && packetsQueue.peek().getIntAt(0) == expectedId)
							packetsQueue.poll();

						// czekamy na kolejny pakiet
						++expectedId;
					}*/

				}

			}
			catch (Exception e)
			{
				System.out.println("Z2Sender.ReceiverThread.run: " + e);
			}
		}
	}


	class ConfirmGuard extends Thread
	{
		public void run()
		{
			try
			{
				while (true)
				{
						if ( ! waitForConfirmFifo.isEmpty() && System.currentTimeMillis() - waitForConfirmFifo.peek().time >= confirmTime)
						{
							// sciagamy nasz element z kolejki
							DatagramInfo curInfo = waitForConfirmFifo.poll();

							// wykonujemy retransmisje
							System.out.println("Retransmisja [" + curInfo.id + "] " + (char)curInfo.packet.data[4]);
							numberOfSendedMessages++;

							// tworzymy nowy DatagramPackiet o danych z naszego pakietu ( wzietego z DatagramInfo)
							Z2Packet p = new Z2Packet(4 + 1);
							p.setIntAt(curInfo.id, 0);
							p.data = curInfo.packet.data;
							DatagramPacket packet = new DatagramPacket(p.data, p.data.length, localHost, destinationPort);

							// wysylamy do odbiorcy
							sendPacket(packet);

							// akuatlizujemy czas
							curInfo.time = System.currentTimeMillis();

							// dodajemy do kolejki
							waitForConfirmFifo.add(curInfo);
						}

					sleep(200);
				}
			}
			catch (Exception e)
			{
				System.out.println("ConfirmGurad Exception: " + e);
			}
		}
	}

	public static void main(String[] args) throws Exception
	{
		Z2Sender sender = new Z2Sender(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
		sender.sender.start();
		sender.receiver.start();
		sender.guard.start();
	}
}
