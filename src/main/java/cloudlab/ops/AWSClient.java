package cloudlab.ops;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import cloudlab.EC2OpsProto.DestroyReply;
import cloudlab.EC2OpsProto.DestroyRequest;
import cloudlab.EC2OpsProto.EC2OpsGrpc;
import cloudlab.EC2OpsProto.Reply;
import cloudlab.EC2OpsProto.Request;
import cloudlab.WordPressOpsProto.ConnectReply;
import cloudlab.WordPressOpsProto.ConnectRequest;
import cloudlab.WordPressOpsProto.DeployAppReply;
import cloudlab.WordPressOpsProto.DeployAppRequest;
import cloudlab.WordPressOpsProto.DeployDBReply;
import cloudlab.WordPressOpsProto.DeployDBRequest;
import cloudlab.WordPressOpsProto.WordPressOpsGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

/**
 * AWSClient: Reads property files and accesses the EC2Ops & WordPressOps
 * service by communicating with AWSServer.
 * Based on the property file this populates the request
 * message with required details to create or destroy a AWS EC2 instance or
 * deploy WordPress or deploy DB or connect the Wordpress & DB in the provided EC2 instance
 */

public class AWSClient {
	private static final Logger logger = Logger.getLogger(AWSClient.class.getName());

	private final ManagedChannel channel;
	private final EC2OpsGrpc.EC2OpsBlockingStub blockingStub;
	private final WordPressOpsGrpc.WordPressOpsBlockingStub wblockingStub;
	Scanner in = new Scanner(System.in);
	boolean quit, appdone, dbdone = quit = appdone = false;

	public AWSClient(String host, int port) {
		channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext(true).build();
		blockingStub = EC2OpsGrpc.newBlockingStub(channel);
		wblockingStub = WordPressOpsGrpc.newBlockingStub(channel);
	}

	public void shutdown() throws InterruptedException {
		channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
	}

	private void create(String region, String OS, String machineSize, String keyPair, String bucketName) {
		Request request = Request.newBuilder().setRegion(region).setOS(OS).setMachineSize(machineSize)
				.setKeyPair(keyPair).setBucketName(bucketName).build();
		Reply reply;
		try {
			reply = blockingStub.createVM(request);
		} catch (StatusRuntimeException e) {
			logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
			return;
		}
		logger.info("Instance ID: " + reply.getInstanceID());
		logger.info("Public IP: " + reply.getPublicIP());
	}

	private void destroy(String instanceID, String region) {
		DestroyRequest request = DestroyRequest.newBuilder().setInstanceID(instanceID).setRegion(region).build();
		DestroyReply reply;
		try {
			reply = blockingStub.destroyVM(request);
		} catch (StatusRuntimeException e) {
			logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
			return;
		}
		logger.info(instanceID + " status is: " + reply.getStatus());

	}

	private void deployApp(String credentials, String bucketName, String username, String publicIP) {
		DeployAppRequest request = DeployAppRequest.newBuilder().setCredentials(credentials).setBucketName(bucketName)
				.setUsername(username).setPublicIP(publicIP).build();
		DeployAppReply reply;
		try {
			reply = wblockingStub.deployApp(request);
		} catch (StatusRuntimeException e) {
			logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
			return;
		}
		logger.info("Output: " + reply.getOutput());
		appdone = true;
	}

	private void deployDB(String credentials, String bucketName, String username, String publicIP) {
		DeployDBRequest request = DeployDBRequest.newBuilder().setCredentials(credentials).setBucketName(bucketName)
				.setUsername(username).setPublicIP(publicIP).build();
		DeployDBReply reply;
		try {
			reply = wblockingStub.deployDB(request);
		} catch (StatusRuntimeException e) {
			logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
			return;
		}
		logger.info("Output: " + reply.getOutput());
		dbdone = true;
	}

	private void connect(String credentials, String bucketName, String username, String publicIP) {
		ConnectRequest request = ConnectRequest.newBuilder().setCredentials(credentials).setBucketName(bucketName)
				.setUsername(username).setPublicIP(publicIP).build();
		ConnectReply reply;
		try {
			reply = wblockingStub.connectAppToDB(request);
		} catch (StatusRuntimeException e) {
			logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
			return;
		}
		logger.info("Output: " + reply.getOutput());
	}

	public static void main(String[] args) throws Exception {
		AWSClient client = new AWSClient("localhost", 50051);
		String serviceCase = null;

		Properties properties = new Properties();
		InputStream propIn = new FileInputStream(new File("config.properties"));
		properties.load(propIn);

		serviceCase = properties.getProperty("serviceCase");

		try {
			System.out.println("MENU");

			if (serviceCase.equals("CreateVM")) {
				System.out.println("EC2 Ops");
				System.out.println("Create VM");
				System.out.println("Region of instance: " + properties.getProperty("region"));
				System.out.println("OS (machine ID): " + properties.getProperty("os"));
				System.out.println("Machine size: " + properties.getProperty("machineSize"));
				System.out.println("Key pair file name: " + properties.getProperty("keyPair"));
				System.out.println("S3 bucket name (holds the pem file): " + properties.getProperty("bucketName"));
				client.create(properties.getProperty("region"), properties.getProperty("os"),
						properties.getProperty("machineSize"), properties.getProperty("keyPair"),
						properties.getProperty("bucketName"));
			} else if (serviceCase.equals("DeleteVM")) {
				System.out.println("EC2 Ops");
				System.out.println("Delete VM");
				System.out.println("Instance ID: " + properties.getProperty("instanceID"));
				System.out.println("Region of instance: " + properties.getProperty("region"));
				client.destroy(properties.getProperty("instanceID"), properties.getProperty("region"));
			} else if (serviceCase.equals("DeployApp")) {
				System.out.println("WordPress Ops");
				System.out.println("Deploy App");
				System.out.println("Key pair file name: " + properties.getProperty("keyPair"));
				System.out.println("S3 bucket name (holds the pem file): " + properties.getProperty("bucketName"));
				System.out.println("Username: " + properties.getProperty("username"));
				System.out.println("Public IP of instance: " + properties.getProperty("publicIP"));
				client.deployApp(properties.getProperty("keyPair"), properties.getProperty("bucketName"),
						properties.getProperty("username"), properties.getProperty("publicIP"));
			} else if (serviceCase.equals("DeployDB")) {
				System.out.println("WordPress Ops");
				System.out.println("Deploy DB");
				System.out.println("Key pair file name: " + properties.getProperty("keyPair"));
				System.out.println("S3 bucket name (holds the pem file): " + properties.getProperty("bucketName"));
				System.out.println("Username: " + properties.getProperty("username"));
				System.out.println("Public IP of instance: " + properties.getProperty("publicIP"));
				client.deployDB(properties.getProperty("keyPair"), properties.getProperty("bucketName"),
						properties.getProperty("username"), properties.getProperty("publicIP"));
			} else if (serviceCase.equals("Connect")) {
				System.out.println("WordPress Ops");
				System.out.println("Connect App to DB");
				System.out.println("Key pair file name: " + properties.getProperty("keyPair"));
				System.out.println("S3 bucket name (holds the pem file): " + properties.getProperty("bucketName"));
				System.out.println("Username: " + properties.getProperty("username"));
				System.out.println("Public IP of instance: " + properties.getProperty("publicIP"));
				client.connect(properties.getProperty("keyPair"), properties.getProperty("bucketName"),
						properties.getProperty("username"), properties.getProperty("publicIP"));
			} else
				System.out.println("Wrong service option ");
		} finally {
			client.shutdown();
		}
	}
}
