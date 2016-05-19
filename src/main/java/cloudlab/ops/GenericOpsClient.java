package cloudlab.ops;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import cloudlab.GenericOpsProto.GenericOpsGrpc;
import cloudlab.GenericOpsProto.GenericReply;
import cloudlab.GenericOpsProto.GenericRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

/**
 * AWSClient: Reads property files and accesses the GenericOps service by communicating
 * with GenericOpsServer. Populates the request message with required details to run a 
 * generic puppet module in the VM given by the user
 */

public class GenericOpsClient {
	private static final Logger logger = Logger.getLogger(GenericOpsClient.class.getName());

	private final ManagedChannel channel;
	private final GenericOpsGrpc.GenericOpsBlockingStub blockingStub;

	public GenericOpsClient(String host, int port) {
		channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext(true).build();
		blockingStub = GenericOpsGrpc.newBlockingStub(channel);
	}

	public void shutdown() throws InterruptedException {
		channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
	}

	private void create(String credentials, String bucketName, String username, String publicIP, String moduleName, String installFile) {
		GenericRequest request = GenericRequest.newBuilder().setCredentials(credentials).setBucketName(bucketName).setUsername(username).setPublicIP(publicIP).setModuleName(moduleName).setInstallFile(installFile).build();
		GenericReply reply;
		try {
			reply = blockingStub.create(request);
		} catch (StatusRuntimeException e) {
			logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
			return;
		}
		logger.info("Output: " + reply.getOutput());		
	}

	public static void main(String[] args) throws Exception {
		GenericOpsClient client = new GenericOpsClient("genericlocalhost", 50052);
		String serviceCase = null;
		
		Properties properties = new Properties();
		InputStream propIn = new FileInputStream(new File("config.properties"));
		properties.load(propIn);

		serviceCase = properties.getProperty("serviceCase");

		try {
			System.out.println("Generic Service");

			if (serviceCase.equals("Generic")) {
				System.out.println("WordPress Ops");
				System.out.println("Deploy App");
				System.out.println("Key pair file name: " + properties.getProperty("keyPair"));
				System.out.println("S3 bucket name (holds the pem file): " + properties.getProperty("bucketName"));
				System.out.println("Username: " + properties.getProperty("username"));
				System.out.println("Public IP of instance: " + properties.getProperty("publicIP"));
				System.out.println("Puppet module name: " + properties.getProperty("moduleName"));
				System.out.println("Installation file git url: " + properties.getProperty("installFile"));
				client.create(properties.getProperty("keyPair"), properties.getProperty("bucketName"),
						properties.getProperty("username"), properties.getProperty("publicIP"), properties.getProperty("moduleName"), properties.getProperty("installFile"));
			} else
				System.out.println("Wrong service option ");
		} finally {
			client.shutdown();
		}
	}
}
