/**
 * 
 */
package com.force.formula.sql.google;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.testcontainers.containers.SpannerEmulatorContainer;
import org.testcontainers.utility.DockerImageName;

import com.force.formula.DisplayField;
import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.spanner.Instance;
import com.google.cloud.spanner.InstanceAdminClient;
import com.google.cloud.spanner.InstanceConfigId;
import com.google.cloud.spanner.InstanceId;
import com.google.cloud.spanner.InstanceInfo;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;
import com.google.spanner.admin.instance.v1.CreateInstanceMetadata;

/**
 * Spanner tester that uses a container with the latest emulator 
 * @author stamm
 * @since 0.3
 */
public class SpannerContainerTester extends GoogleContainerTester<SpannerEmulatorContainer> {

	/**
	 * @throws IOException
	 */
	public SpannerContainerTester() throws IOException {
	}

	@Override
	public String getDbTypeName() {
		return "spanner";
	}

	@Override
	protected SpannerEmulatorContainer constructDb() throws IOException {
		return new SpannerEmulatorContainer(DockerImageName.parse("gcr.io/cloud-spanner-emulator/emulator"));
    }

    private static final String PROJECT_ID = "test-project";
    private static final String INSTANCE_ID = "test-instance";
    private static final String DATABASE_ID = "test-database";

    @Override
    protected Connection constructConnection(SpannerEmulatorContainer db) throws SQLException, IOException {
        String host = db.getEmulatorGrpcEndpoint();

        SpannerOptions options = SpannerOptions.newBuilder()
                .setEmulatorHost(host)
                .setProjectId(PROJECT_ID)
                .build();
        Spanner spanner = options.getService();
        
        try {

            // Create a Spanner instance
            InstanceAdminClient instanceAdminClient = spanner.getInstanceAdminClient();
            InstanceId instanceId = InstanceId.of(PROJECT_ID, INSTANCE_ID);

            OperationFuture<Instance, CreateInstanceMetadata> instanceFuture =
                instanceAdminClient.createInstance(
                    InstanceInfo.newBuilder(instanceId)
                        // make sure to use the special `emulator-config`
                        .setInstanceConfigId(InstanceConfigId.of(PROJECT_ID, "emulator-config"))
                        .build());
            instanceFuture.get();
    
            // create the database and the table so that we have an explicit database
            spanner.getDatabaseAdminClient()
                    .createDatabase(INSTANCE_ID, DATABASE_ID, List.of(
                            "CREATE TABLE Test ( TestId INT64 NOT NULL ) PRIMARY KEY (TestId)"))
                    .get(1, TimeUnit.SECONDS);
        } catch (TimeoutException | ExecutionException | InterruptedException ex) {
            throw new IOException(ex);
        }

        return DriverManager.getConnection("jdbc:cloudspanner://"+host+"/projects/"+PROJECT_ID+"/instances/"+INSTANCE_ID+"/databases/"+DATABASE_ID+";usePlainText=true");
    }

    @Override
    protected void bindBigDecimal(PreparedStatement pstmt, DisplayField df, BigDecimal bd, int position)
            throws SQLException {
        // Spanner doesn't support scale > 9.  Sigh.
        if (bd.scale() > 9) {
            bd = bd.setScale(9, RoundingMode.HALF_EVEN);
        }
        super.bindBigDecimal(pstmt, df, bd, position);
    }
    
    
}
