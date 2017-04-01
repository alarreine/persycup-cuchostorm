package org.r2d2.util;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

public class CameraClient {

	private final int port;

	// Create a socket to listen on the port.
	private DatagramSocket dsocket;

	private byte[] buffer;
	private DatagramPacket packet;

	public CameraClient(int p) {
		port = p;

		buffer = new byte[2048];
		packet = new DatagramPacket(buffer, buffer.length);

		try {
			dsocket = new DatagramSocket(port);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public List<ObjectPosition> getObjectPosition() {
		List<ObjectPosition> retorno = null;

		try {
			packet.setLength(buffer.length);

			dsocket.receive(packet);
			retorno = new ArrayList<>();

			String msg = new String(buffer, 0, packet.getLength());
			String[] palets = msg.split("\n");
			for (int i = 0; i < palets.length; i++) {
				String[] coord = palets[i].split(";");
				retorno.add(new ObjectPosition(Integer.parseInt(coord[1]), Integer.parseInt(coord[2])));

			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return retorno;
	}

	public ObjectPosition getObjectPosition(int i) {
		ObjectPosition retorno = null;

		try {
			packet.setLength(buffer.length);

			dsocket.receive(packet);

			String msg = new String(buffer, 0, packet.getLength());
			String[] palets = msg.split("\n");

			String[] coord = palets[i].split(";");
			retorno = new ObjectPosition(Integer.parseInt(coord[1]), Integer.parseInt(coord[2]));

		} catch (

		IOException e) {

			e.printStackTrace();
		}

		return retorno;
	}

}
