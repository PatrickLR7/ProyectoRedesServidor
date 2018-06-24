import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class ServidorUDP {

    /**
     * Método para recibir mensajes desde el cliente
     * @param args
     */
    public static void main (String[] args){
        try  {
            DatagramSocket udpSocket = new DatagramSocket(9107); // socket para recibir mensajes del cliente.
            byte[] buffer = new byte [1024]; //buffer o memoria que utiliza el servidor para guardar mensajes recibidos.
            while (true){
                DatagramPacket dPacket = new DatagramPacket(buffer, buffer.length); //guarda los paquetes del cliente.
                    udpSocket.receive(dPacket); //socket recibe paquete enviado por el cliente y lo guarda en el datagramPacket.
                    System.out.print("ip: " + dPacket.getAddress() + "\n");
                    System.out.print("puerto: " + dPacket.getPort() + "\n");
                    System.out.print("mensaje: " + new String(dPacket.getData(), 0 , dPacket.getLength()) + "\n");

            }
        } catch (SocketException e){ //maneja excepción del datagram socket.
            e.printStackTrace();
        } catch (IOException e) { // maneja excepción del datagram packet
            e.printStackTrace();
        }
    }
}
