import java.net.*;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;

public class Z2Receiver
{
	static final int datagramSize = 50;
	static final int confirmTime = 2000;
	static int sectorSize = 15;
	static long currentTime = 0;
	InetAddress localHost;
	int destinationPort;
	DatagramSocket socket;
	static int numberOfSendedMessages = 0;

	ReceiverThread receiver;

	public Z2Receiver(int myPort, int destPort) throws Exception
	{
		localHost = InetAddress.getByName("127.0.0.1");
		destinationPort = destPort;
		socket = new DatagramSocket(myPort);
		receiver = new ReceiverThread();
	}

	class ReceiverThread extends Thread
	{
		int expectedId = 0;
		ArrayList<Z2Packet> packetsQueue = new ArrayList<Z2Packet>();
		int numberOfReceivedMessages = 0;
		public void run()
		{
			try
			{
				while (true)
				{

					byte[] data = new byte[datagramSize];
					DatagramPacket packet = new DatagramPacket(data, datagramSize);

					// czekaj az przyjdzie wiadomosc
					socket.receive(packet);
					numberOfReceivedMessages++;
					Z2Packet p = new Z2Packet(packet.getData());
					System.out.println("Dostalem id "+ p.getIntAt(0));
					// dodaj tylko pakiety na ktore czekamy ( mniejsze Id juz na pewno otrzymalismy i potwierdzilismy )
					if( p.getIntAt(0) == expectedId){
						expectedId++;
						packetsQueue.add(p);
						//System.out.println(packetsQueue.toString());
						currentTime = System.currentTimeMillis();
					}else{
						if(System.currentTimeMillis() - currentTime > confirmTime && !packetsQueue.isEmpty()){
							Z2Packet lastElement = packetsQueue.get(packetsQueue.size()-1);
							System.out.println("Wysylam id dla potwierdzenia "+lastElement.getIntAt(0) );
							DatagramPacket confirmPacket = new DatagramPacket(lastElement.data, lastElement.data.length, localHost, destinationPort);
							socket.send(confirmPacket);
							numberOfSendedMessages++;
							currentTime = System.currentTimeMillis();
						}
					}

					//dostaliśmy to na co czekaliśmy wysyłamy potwierdzenie
					if(packetsQueue.size()== sectorSize){
						System.out.println("Wysylam id dla potwierdzenia bo mam caly sektor "+p.getIntAt(0) );
						DatagramPacket confirmPacket = new DatagramPacket(p.data, p.data.length, localHost, destinationPort);
						socket.send(confirmPacket);
						numberOfSendedMessages++;
						numberOfReceivedMessages = 0;
					}



					/*while ( ! packetsQueue.isEmpty() && packetsQueue.size() == sectorSize)
					{
							Z2Packet expectedPacket = packetsQueue.poll();

							System.out.println("R: [" + expectedPacket.getIntAt(0) + "] " + (char)expectedPacket.data[4]);

							// ściagaj dopóki mamy powtórzenia
							while( ! packetsQueue.isEmpty() && packetsQueue.peek().getIntAt(0) == expectedId)
								packetsQueue.poll();

							// czekamy na kolejny fragment wiadomosci
							++expectedId;
					}*/
					if(packetsQueue.size()== sectorSize){
						System.out.println("####Dostalem caly sektor#####");
					for(Z2Packet pac : packetsQueue){

						System.out.println("Receiver: [" + pac.getIntAt(0) + "] " + (char)pac.data[4]);

					}
					System.out.println("Receiver wyslal potwierdzen: "+	numberOfSendedMessages);
					packetsQueue.clear();
				}

				}
			}
			catch (Exception e)
			{
				System.out.println("Z2Receiver.ReceiverThread.run: " + e);
			}
		}
	}

	public static void main(String[] args) throws Exception
	{
		Z2Receiver receiver = new Z2Receiver(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
		receiver.receiver.start();
	}
}
