package utilities;

import controllers.Controller;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import yahoofinance.Stock;

import javax.swing.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import static utilities.GlobalStatics.highlightErrorsV;

public class Scan {
	
	@FXML
	TextField conditionalTF, nameTF, discriminatorTF, epsilonTF;
	@FXML
	ChoiceBox<String> propertyCB;
	
	private static Controller staticController;
	private final Evaluator evaluator = new Evaluator();
	private Operator operator = Operator.doesNotEqual;
	private Controller controller;
	private String fullCondition = "";
	private String discriminator = "0";
	private String conditional = "0";
	private String filePrepend = "src/log/";
	private String fileAppend = ".txt";
	
	public void initialize() {
		controller = staticController;
		propertyCB.getItems().addAll(">", "≥", "<", "≤", "=", "≠");
	}
	
	@FXML
	private void createScan() {
		//Error Checking
		if (nameTF.getText().isEmpty()) {
			highlightErrorsV("Scan ID must not be empty", nameTF, discriminatorTF, conditionalTF, epsilonTF);
			return;
		} else if (conditionalTF.getText().isEmpty()) {
			highlightErrorsV("Conditional must not be empty", nameTF, discriminatorTF, conditionalTF, epsilonTF);
			return;
		} else if (discriminatorTF.getText().isEmpty()) {
			highlightErrorsV("Discriminator must not be empty", nameTF, discriminatorTF, conditionalTF, epsilonTF);
			return;
		} else if(propertyCB.getValue() == null){
			highlightErrorsV("Property must have a value", nameTF, discriminatorTF, conditionalTF, epsilonTF);
			return;
		}

		//Get the operand that the user wants to apply
		switch (propertyCB.getValue()) {
			case ">":
				operator = Operator.lessThan;
				break;
			case "≥":
				operator = Operator.lessThanOrEqual;
				break;
			case "<":
				operator = Operator.greaterThan;
				break;
			case "≤":
				operator = Operator.greaterThanOrEqual;
				break;
			case "=":
				operator = Operator.equals;
				break;
			case "≠":
				operator = Operator.doesNotEqual;
				break;
			default:
				operator = Operator.equals;
		}

		//Set the evaluator variables
		String name = nameTF.getText();

		//Check to see if the conditional or the discriminator have a constant value, i.e., price(Goog) will be
		// constant for the scan.
		conditional = conditionalTF.getText();
		discriminator = evaluator.reduce(discriminatorTF.getText());
		fullCondition = conditional + propertyCB.getValue() + discriminator;
				Function<Stock, Boolean> expression = stock -> {
			evaluator.setActiveStock(stock);
			return operator.compare(evaluator.evaluate(conditional),
					evaluator.evaluate(discriminator));
		};

		Runnable log = (() -> {
			try {
				boolean doScan = checkFileConstraints(name);

				if (doScan) {
					Scanner scanner = new Scanner(expression, controller);
					logScan( filePrepend + name + fileAppend, scanner.getFutureSuccesses());
				} else{
					controller.showProgressBar(false, "");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		});

		ExecutorService executorService = Executors.newFixedThreadPool(1);
		executorService.submit(log);
	}

	private boolean checkFileConstraints(String nameOfScan) throws IOException {
		String fileName = filePrepend + nameOfScan + fileAppend;
		File file = new File(fileName);
		if (!file.exists()) {
			//create file
			return file.createNewFile();
		}
		else {
			int i = JOptionPane.showConfirmDialog(null, "Past scan already exists with same name. Would you like to overwrite it?",
					"Duplicate past scan", JOptionPane.OK_CANCEL_OPTION);
			if (i == 0){
				PrintWriter pw = new PrintWriter(fileName);
				pw.close();
				return true;
			} return false;
		}
	}

	/**
	 * Get the controller for the UI thread so that we can update the progressBar while we are doing scans
	 * */
	public static void getController(Controller controller) {
		staticController = controller;
	}

	private void logScan(String fileName, Set<String> scanResults) throws IOException {
		//Error checking
		if (scanResults == null) {
			return;
		}

		RandomAccessFile stream = new RandomAccessFile(fileName, "rw");
		byte[] delimiter = ", ".getBytes();
		FileChannel channel = stream.getChannel();

		byte[] start = ("All ticker symbols that passed the criteria: "+fullCondition).getBytes();
		ByteBuffer buffer = ByteBuffer.allocate(start.length);
		buffer.put(start);
		buffer.flip();
		channel.write(buffer);
		buffer.clear();

		for (String value : scanResults) {
			byte[] strBytes = value.getBytes();
			buffer = ByteBuffer.allocate(strBytes.length+delimiter.length);
			buffer.put(strBytes);
			buffer.put(delimiter);
			buffer.flip();
			channel.write(buffer);
			buffer.clear();
		}
		stream.close();
		channel.close();
	}
}
