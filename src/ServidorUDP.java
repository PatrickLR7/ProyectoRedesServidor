import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class ServidorUDP {
    public static void main (String[] args){
        try  {
            DatagramSocket udpSocket = new DatagramSocket(9107);
            byte[] buffer = new byte [1024];
            while (true){
                DatagramPacket dPacket = new DatagramPacket(buffer, buffer.length);
                    udpSocket.receive(dPacket);
                    System.out.print("ip: " + dPacket.getAddress());
                    System.out.print("puerto: " + dPacket.getPort());
                    System.out.print("mensaje: " + new String(dPacket.getData(), 0 , dPacket.getLength()));

            }
        } catch (SocketException e){
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
