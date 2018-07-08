// Receptor
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.CRC32;

import static java.util.Arrays.copyOfRange;

public class ReceptorUDP {

    /**
     * Método para recibir mensajes desde el cliente.
     * La ventana deslizante utiliza el protocolo Go-Back-N.
     * @param args
     */
    static int tamPaquete = 128; //Tamaño del paquete que se espera recibir.

    /**
     * Contructor para el receptor.
     * @param puertoSocket1 numero de puerto para el socket que recibe.
     * @param puertoSocket2 numero de puerto para el socket que envia ACKs.
     */
    public ReceptorUDP(int puertoSocket1, int puertoSocket2){
        DatagramSocket socket1, socket2;
        System.out.println("Receptor: puertoSocket1 = " + puertoSocket1 + ", " + "puertoSocket2 = " + puertoSocket2 + ".");

        int numSecuenciaPrevio = -1; //el último numero de secuencia que se recibió en orden.
        int numSecuenciaEsperado = 0; //el próximo numero de secuencia que se espera recibir.
        boolean transferenciaCompleta = false; // bandera para verificar si se completó la transferencia.
        String mensajeRe = "";
        int numPaquete = 0;

        //Se crean los sockets.
        try {
            socket1 = new DatagramSocket(puertoSocket1);
            socket2 = new DatagramSocket();
            System.out.println("Receptor: Escuchando...");
            try {
                byte[] datosEntrada = new byte[tamPaquete]; //Arreglo para guardar los bytes de los datos de entrada.
                DatagramPacket paqueteEntrada = new DatagramPacket (datosEntrada, datosEntrada.length); //Para guardar el paquete entrante.
                InetAddress direccionDest = InetAddress.getByName("127.0.0.1"); // Dirección local para que el programa emisor se pueda comunicar con el receptor.

                //Escuchar en el puertoSocket1
                while(!transferenciaCompleta) {
                    //recibir paquete
                    socket1.receive(paqueteEntrada);

                    byte[] checksumRecibido = copyOfRange(datosEntrada, 0, 8); //Copia los primeros 8 bytes del arreglo de datos de entrada, que corresponden al checksum recibido.
                    CRC32 checksum = new CRC32(); // Se usa para calcular el checksum de una secuencia de datos (lo usamos para calcular el nuevo checksum).
                    checksum.update(copyOfRange(datosEntrada, 8, paqueteEntrada.getLength())); //Actualiza la suma de control utilizando el segmento de datos de los datosEntrada recibidos.
                    byte[] checksumCalculado = ByteBuffer.allocate(8).putLong(checksum.getValue()).array(); // Guarda el nuevo checksum calculado.

                    //Si no se dectectaron errores en el paquete
                    if(Arrays.equals(checksumRecibido, checksumCalculado)){
                        int numeroSecuencia = ByteBuffer.wrap(copyOfRange(datosEntrada, 8 , 12)).getInt();
                        System.out.println("Receptor: Num de secuencia recibido: " + numeroSecuencia);

                        //Si el paquete se recibió en orden
                        if(numeroSecuencia == numSecuenciaEsperado) {
                            //si es el paquete final (sin datos), se envía el ack para terminar.
                            if(paqueteEntrada.getLength() == 12) {
                                byte[] paqueteACK = generarPaquete(numeroSecuencia); //ACK para el último paquete recibido.
                                socket2.send(new DatagramPacket(paqueteACK, paqueteACK.length, direccionDest, puertoSocket2)); //Envía ACK para el último paquete recibido.
                                System.out.println("Receptor: ACK Enviado " + numeroSecuencia);
                                paqueteACK = generarPaquete(-2); //Genera un ACK -2 para indicar que finalizó la transmisión.
                                //envía 20 acks en caso de que el último ack no haya sido recibido por el emisor (Se asegura de que termine).
                                for(int i = 0; i <= 20; i++){
                                    socket2.send(new DatagramPacket(paqueteACK, paqueteACK.length, direccionDest, puertoSocket2));
                                }
                                transferenciaCompleta = true; //Indica que se completó la transferencia.
                                System.out.println("Receptor: se han recibido todos los paquetes. Mensaje completado.");
                                continue;
                            } else { //Si no, envíe el ACK.
                                byte [] paqueteACK = generarPaquete(numeroSecuencia);
                                socket2.send(new DatagramPacket(paqueteACK, paqueteACK.length, direccionDest, puertoSocket2));
                                System.out.println("Receptor: ACK Enviado " + numeroSecuencia);
                            }

                            // Si es el primer paquete de la transferencia.
                            if(numeroSecuencia == 0 && numSecuenciaPrevio == -1){
                                mensajeRe = mensajeRe + "ip: " + paqueteEntrada.getAddress() + "\n";
                                mensajeRe = mensajeRe + "puerto: " + paqueteEntrada.getPort() + "\n";
                                mensajeRe = mensajeRe + "mensaje: " + new String(paqueteEntrada.getData(), 0 , paqueteEntrada.getLength());
                                System.out.println("Receptor: Paquete # " + numPaquete + " recibido.");
                                numPaquete++;
                            } else { //Si no es el primero, solo concatene al string del mensaje y actualice los numeros de secuencia.
                                mensajeRe = mensajeRe + new String(paqueteEntrada.getData(), 0, paqueteEntrada.getLength());
                                System.out.println("Receptor: Paquete # " + numPaquete + " recibido.");
                                numPaquete++;
                            }
                                numSecuenciaEsperado++;
                                numSecuenciaPrevio = numeroSecuencia;
                        } else{ //Si el paquete no se recibió en el orden esperado, envíe ACK duplicado.
                            byte[] paqueteACK = generarPaquete(numSecuenciaPrevio);
                            socket2.send(new DatagramPacket(paqueteACK, paqueteACK.length, direccionDest, puertoSocket2));
                            System.out.println("Receptor: ACK duplicado enviado: " + numSecuenciaPrevio);
                        }
                    } else { //El paquete tiene errores.
                        System.out.println("Receptor: Paquete dañado o perdido.");
                        byte[] paqueteACK = generarPaquete(numSecuenciaPrevio);
                        socket2.send(new DatagramPacket(paqueteACK, paqueteACK.length, direccionDest, puertoSocket2));
                        System.out.println("Receptor: ACK duplicado enviado: " + numSecuenciaPrevio);
                    }
                }
                System.out.println("Receptor: Transferencia completa " + "\n");
                System.out.println("Receptor: -Mensaje recibido- " + "\n" + mensajeRe + "\n");

            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            } finally {
                socket1.close();
                socket2.close();
                System.out.println("Receptor: socket de entrada cerrado.");
                System.out.println("Receptor: socket de salida cerrado.");
            }

        } catch (SocketException e1) {
            e1.printStackTrace();
        }

    }

    /**
     *  Método para generar un paquete ACK.
     * @param numACK numero del ACK generado.
     */

    public byte[] generarPaquete(int numACK){
        byte[] ackNumBytes = ByteBuffer.allocate(4).putInt(numACK).array(); //4 bytes para el número de ACK.
        CRC32 checksum = new CRC32(); // calcular el checksum
        checksum.update(ackNumBytes);

        ByteBuffer btBuff = ByteBuffer.allocate(12); // construye paquete ACK
        btBuff.put(ByteBuffer.allocate(8).putLong(checksum.getValue()).array()); //8 bytes para la suma de control.
        btBuff.put(ackNumBytes);
        return btBuff.array();
    }

    public static void main (String[] args){
        // se pasan los parametros
        if (args.length != 2) {
            System.err.println("Uso: java ReceptorUDP puertoSocket1, puertoSocket2");
            System.exit(-1);
        }
        else new ReceptorUDP(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
      /*
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
        */
    }
}