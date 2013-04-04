package ru.hh.anton.server;

public class ServerStarter {

	public static void main(String[] args) {

		int port;
		if (args.length > 0) {
			port = Integer.parseInt(args[0]);
		} else {
			port = 8080;
		}

		new Server(port).run();

	}

}
