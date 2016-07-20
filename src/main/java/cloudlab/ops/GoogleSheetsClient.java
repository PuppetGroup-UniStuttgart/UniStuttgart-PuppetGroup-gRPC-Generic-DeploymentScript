package cloudlab.ops;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import cloudlab.GoogleSheetsProto.CreateWorksheetReply;
import cloudlab.GoogleSheetsProto.CreateWorksheetRequest;
import cloudlab.GoogleSheetsProto.DeleteRowReply;
import cloudlab.GoogleSheetsProto.DeleteRowRequest;
import cloudlab.GoogleSheetsProto.DeleteWorksheetReply;
import cloudlab.GoogleSheetsProto.DeleteWorksheetRequest;
import cloudlab.GoogleSheetsProto.Empty;
import cloudlab.GoogleSheetsProto.FetchRowColReply;
import cloudlab.GoogleSheetsProto.FetchRowColRequest;
import cloudlab.GoogleSheetsProto.GetWorksheetContentsReply;
import cloudlab.GoogleSheetsProto.GetWorksheetContentsRequest;
import cloudlab.GoogleSheetsProto.SpreadsheetListReply;
import cloudlab.GoogleSheetsProto.UpdateWorksheetReply;
import cloudlab.GoogleSheetsProto.UpdateWorksheetRequest;
import cloudlab.GoogleSheetsProto.WorksheetListReply;
import cloudlab.GoogleSheetsProto.WorksheetListRequest;
import cloudlab.GoogleSheetsProto.GoogleSheetsGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

/**
 * GoogleSheetClient: Reads property files and accesses the Google Sheets
 * service Based on the property file this populates the request message with
 * required details to call different Google Sheets functions
 */

public class GoogleSheetsClient {
	private static final Logger logger = Logger.getLogger(GoogleSheetsClient.class.getName());

	private final ManagedChannel channel;
	private final GoogleSheetsGrpc.GoogleSheetsBlockingStub blockingStub;

	public GoogleSheetsClient(String host, int port) {
		channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext(true).build();
		blockingStub = GoogleSheetsGrpc.newBlockingStub(channel);
	}

	public void shutdown() throws InterruptedException {
		channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
	}

	public static void main(String[] args) throws Exception {
		GoogleSheetsClient client = new GoogleSheetsClient("localhost", 50054);
		String serviceCase = null;

		Properties properties = new Properties();
		InputStream propIn = new FileInputStream(new File("config.properties"));
		properties.load(propIn);

		serviceCase = properties.getProperty("serviceCase");

		try {
			System.out.println("Google Sheets gRPC");

			if (serviceCase.equals("spreadsheetList")) {
				System.out.println("spreadsheetList");
				client.spreadsheetList();
			} else if (serviceCase.equals("worksheetList")) {
				System.out.println("worksheetList");
				System.out.println("Spreadsheet Id: " + properties.getProperty("spreadsheetId"));
				client.worksheetList(properties.getProperty("spreadsheetId"));
			} else if (serviceCase.equals("createWorksheet")) {
				System.out.println("createWorksheet");
				System.out.println("Spreadsheet Name: " + properties.getProperty("spreadsheetName"));
				System.out.println("Worksheet Name: " + properties.getProperty("worksheetName"));
				System.out.println("Worksheet Row: " + properties.getProperty("row"));
				System.out.println("Worksheet Col: " + properties.getProperty("col"));
				client.createWorksheet(properties.getProperty("spreadsheetName"),
						properties.getProperty("worksheetName"), properties.getProperty("row"),
						properties.getProperty("col"));
			} else if (serviceCase.equals("updateWorksheet")) {
				System.out.println("updateWorksheet");
				System.out.println("Spreadsheet Id: " + properties.getProperty("spreadsheetId"));
				System.out.println("Old Worksheet Name: " + properties.getProperty("oldWorksheetName"));
				System.out.println("New Worksheet Name: " + properties.getProperty("newWorksheetName"));
				System.out.println("New Worksheet Row: " + properties.getProperty("newWorksheetRow"));
				System.out.println("New Worksheet Col: " + properties.getProperty("newWorksheetCol"));
				client.updateWorksheet(properties.getProperty("spreadsheetId"),
						properties.getProperty("oldWorksheetName"), properties.getProperty("newWorksheetName"),
						properties.getProperty("newWorksheetRow"), properties.getProperty("newWorksheetCol"));
			} else if (serviceCase.equals("deleteWorksheet")) {
				System.out.println("deleteWorksheet");
				System.out.println("Spreadsheet Id: " + properties.getProperty("spreadsheetId"));
				System.out.println("Worksheet Name: " + properties.getProperty("worksheetName"));
				client.deleteWorksheet(properties.getProperty("spreadsheetId"),
						properties.getProperty("worksheetName"));
			} else if (serviceCase.equals("getWorksheetContents")) {
				System.out.println("getWorksheetContents");
				System.out.println("Spreadsheet Id: " + properties.getProperty("spreadsheetId"));
				System.out.println("Worksheet Name: " + properties.getProperty("worksheetName"));
				client.getWorksheetContents(properties.getProperty("spreadsheetId"),
						properties.getProperty("worksheetName"));
			} else if (serviceCase.equals("fetchRowCol")) {
				System.out.println("fetchRowCol");
				System.out.println("Spreadsheet Id: " + properties.getProperty("spreadsheetId"));
				System.out.println("Worksheet Name: " + properties.getProperty("worksheetName"));
				System.out.println("Row: " + properties.getProperty("row"));
				System.out.println("Col: " + properties.getProperty("col"));
				client.fetchRowCol(properties.getProperty("spreadsheetId"), properties.getProperty("worksheetName"),
						properties.getProperty("row"), properties.getProperty("col"));
			} else if (serviceCase.equals("deleteRow")) {
				System.out.println("deleteRow");
				System.out.println("Spreadsheet Id: " + properties.getProperty("spreadsheetId"));
				System.out.println("Worksheet Name: " + properties.getProperty("worksheetName"));
				System.out.println("Row: " + properties.getProperty("row"));
				client.deleteRow(properties.getProperty("spreadsheetId"), properties.getProperty("worksheetName"),
						properties.getProperty("row"));
			} else
				System.out.println("Wrong service option ");
		} finally {
			client.shutdown();
		}

	}

	private void spreadsheetList() {
		Empty request = Empty.newBuilder().build();
		SpreadsheetListReply reply;
		try {
			reply = blockingStub.spreadsheetList(request);
			logger.info("RESPONSE: ");
			logger.info("Spreadsheets are: ");
			for (String name : reply.getSpreadsheetNameList())
				logger.info(name);
		} catch (StatusRuntimeException e) {
			logger.log(Level.WARNING, "RPC failed: {0}", e.getStackTrace());
		}
	}

	private void worksheetList(String spreadsheetId) {
		WorksheetListRequest request = WorksheetListRequest.newBuilder().setSpreadsheetId(spreadsheetId).build();
		WorksheetListReply reply;
		try {
			reply = blockingStub.worksheetList(request);
			logger.info("RESPONSE: ");
			logger.info("Worksheets details are: ");
			for (String name : reply.getWorksheetDetailsList())
				logger.info(name);
		} catch (StatusRuntimeException e) {
			logger.log(Level.WARNING, "RPC failed: {0}", e.getStackTrace());
		}
	}

	private void createWorksheet(String spreadsheetName, String worksheetName, String row, String col) {
		int intRow = Integer.parseInt(row);
		int intCol = Integer.parseInt(col);
		CreateWorksheetRequest request = CreateWorksheetRequest.newBuilder().setSpreadsheetName(spreadsheetName)
				.setWorksheetName(worksheetName).setRow(intRow).setCol(intCol).build();
		CreateWorksheetReply reply;
		try {
			reply = blockingStub.createWorksheet(request);
			logger.info("RESPONSE: ");
			logger.info(reply.getSuccess());
		} catch (StatusRuntimeException e) {
			logger.log(Level.WARNING, "RPC failed: {0}", e.getStackTrace());
		}
	}

	private void updateWorksheet(String spreadsheetId, String oldWorksheetName, String newWorksheetName,
			String newWorksheetRow, String newWorksheetCol) {
		int intRow = Integer.parseInt(newWorksheetRow);
		int intCol = Integer.parseInt(newWorksheetCol);
		UpdateWorksheetRequest request = UpdateWorksheetRequest.newBuilder().setSpreadsheetId(spreadsheetId)
				.setOldWorksheetName(oldWorksheetName).setNewWorksheetName(newWorksheetName).setNewWorksheetRow(intRow)
				.setNewWorksheetCol(intCol).build();
		UpdateWorksheetReply reply;
		try {
			reply = blockingStub.updateWorksheet(request);
			logger.info("RESPONSE: ");
			logger.info(reply.getSuccess());
		} catch (StatusRuntimeException e) {
			logger.log(Level.WARNING, "RPC failed: {0}", e.getStackTrace());
		}
	}

	private void deleteWorksheet(String spreadsheetId, String worksheetName) {
		DeleteWorksheetRequest request = DeleteWorksheetRequest.newBuilder().setSpreadsheetId(spreadsheetId)
				.setWorksheetName(worksheetName).build();
		DeleteWorksheetReply reply;
		try {
			reply = blockingStub.deleteWorksheet(request);
			logger.info("RESPONSE: ");
			logger.info(reply.getSuccess());
		} catch (StatusRuntimeException e) {
			logger.log(Level.WARNING, "RPC failed: {0}", e.getStackTrace());
		}
	}

	private void getWorksheetContents(String spreadsheetId, String worksheetName) {
		GetWorksheetContentsRequest request = GetWorksheetContentsRequest.newBuilder().setSpreadsheetId(spreadsheetId)
				.setWorksheetName(worksheetName).build();
		GetWorksheetContentsReply reply;
		try {
			reply = blockingStub.getWorksheetContents(request);
			logger.info("RESPONSE: ");
			for (String name : reply.getContentsList())
				logger.info(name);
		} catch (StatusRuntimeException e) {
			logger.log(Level.WARNING, "RPC failed: {0}", e.getStackTrace());
		}
	}

	private void fetchRowCol(String spreadsheetId, String worksheetName, String row, String col) {
		int intRow = Integer.parseInt(row);
		int intCol = Integer.parseInt(col);

		FetchRowColRequest request = FetchRowColRequest.newBuilder().setSpreadsheetId(spreadsheetId)
				.setWorksheetName(worksheetName).setRow(intRow).setCol(intCol).build();
		FetchRowColReply reply;
		try {
			reply = blockingStub.fetchRowCol(request);
			logger.info("RESPONSE: ");
			for (String name : reply.getContentsList())
				logger.info(name);
		} catch (StatusRuntimeException e) {
			logger.log(Level.WARNING, "RPC failed: {0}", e.getStackTrace());
		}
	}

	private void deleteRow(String spreadsheetId, String worksheetName, String row) {
		int intRow = Integer.parseInt(row);

		DeleteRowRequest request = DeleteRowRequest.newBuilder().setSpreadsheetId(spreadsheetId)
				.setWorksheetName(worksheetName).setRow(intRow).build();
		DeleteRowReply reply;
		try {
			reply = blockingStub.deleteRow(request);
			logger.info("RESPONSE: ");
			logger.info(reply.getSuccess());
		} catch (StatusRuntimeException e) {
			logger.log(Level.WARNING, "RPC failed: {0}", e.getStackTrace());
		}
	}
}
