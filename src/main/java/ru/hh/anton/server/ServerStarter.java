package ru.hh.anton.server;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class ServerStarter {

	private static FileHandler fileHandler = null;

	public static void main(String[] args) throws IOException {

		int port;
		if (args.length > 0) {
			port = Integer.parseInt(args[0]);
		} else {
			port = 8080;
		}

		initLogger();

		new Server(port).run();

	}

	private static void initLogger() throws IOException {

		fileHandler = new FileHandler("log.log", false);

		final Logger l = Logger.getLogger("");

		fileHandler.setFormatter(new SimpleFormatter());

		l.addHandler(fileHandler);
		l.setLevel(Level.INFO);

	}

}
