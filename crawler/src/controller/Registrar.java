package controller;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import message.AcknowledgementMessage;
import message.RegisterMessage;

import common.Constants;

// Registers workers with the controller
public class Registrar implements Runnable {
	private Controller controller;
	private ServerSocket serverSocket;
	private ExecutorService threadPool;
	
	public Registrar(Controller controller, ServerSocket serverSocket) {
		this.controller = controller;
		this.serverSocket = serverSocket;
		this.threadPool = Executors.newSingleThreadExecutor();
	}
	
	public void run() {
		while (true) {
			try {
				this.threadPool.execute(new Registration(this.controller, this.serverSocket.accept()));
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
	}
}

class Registration implements Runnable {
	private Controller controller;
	private Socket socket;
	private ObjectInputStream in;
	private ObjectOutputStream out;
	
	public Registration(Controller controller, Socket socket) throws IOException {
		this.controller = controller;
		this.socket = socket;
		this.socket.setSoTimeout(Constants.TIMEOUT);
	}
	
	public void run() {
		try {
			this.out = new ObjectOutputStream(this.socket.getOutputStream());
			this.in = new ObjectInputStream(this.socket.getInputStream());
			
			Object o = this.in.readObject();
			if (!(o instanceof RegisterMessage)) throw new RuntimeException("Not a register message.");
			else if (!this.controller.validate((RegisterMessage)o)) throw new RuntimeException("Not a valid message.");
			else {
				this.out.writeObject(new AcknowledgementMessage(Constants.SECRET));
				this.out.flush();
				RegisterMessage m = (RegisterMessage)o;
				WorkerRemote worker = new WorkerRemote(m.getWorkerName(), m.getUserName(), this.controller, this.in, this.out);
				this.controller.addWorker(worker);
			}
		} catch (Exception e) {
			System.out.println(e);
			try {
				this.socket.close();
			} catch (IOException f) {}
		}
	}
}
