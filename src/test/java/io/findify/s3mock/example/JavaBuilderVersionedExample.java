package io.findify.s3mock.example;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.IOUtils;
import io.findify.s3mock.S3Mock;
import org.apache.http.util.Asserts;

import java.io.IOException;

/**
 * Created by furkilic on 7/11/20.
 */
public class JavaBuilderVersionedExample {
    public static void main(String[] args) throws IOException {
        S3Mock api = new S3Mock.Builder().withPort(8002).withInMemoryVersionedBackend().build();
        api.start();
        AmazonS3 client = AmazonS3ClientBuilder
                .standard()
                .withPathStyleAccessEnabled(true)
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration("http://localhost:8002", "us-east-1"))
                .withCredentials(new AWSStaticCredentialsProvider(new AnonymousAWSCredentials()))
                .build();

        String firstContent = "firstContent";
        String secondContent = "secondContent";

        client.createBucket("testbucket");
        PutObjectResult firstPut = client.putObject("testbucket", "filename", firstContent);
        client.putObject("testbucket", "filename", secondContent);

        Asserts.check(getContent(client.getObject(new GetObjectRequest("testbucket", "filename", firstPut.getVersionId()))).equals(firstContent), "With Version");
        Asserts.check(getContent(client.getObject("testbucket", "filename")).equals(secondContent), "Without Version");

        client.deleteObjects(new DeleteObjectsRequest("testbucket").withKeys("filename"));

    }

    private static String getContent(S3Object s3Object) throws IOException {
        return IOUtils.toString(s3Object.getObjectContent().getDelegateStream());
    }
}
