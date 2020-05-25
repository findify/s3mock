package io.findify.s3mock.example;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Builder;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import io.findify.s3mock.S3Mock;

/**
 * Created by shutty on 5/23/17.
 */
public class JavaBuilderExample {
    public static void main(String[] args) {
        S3Mock api = new S3Mock.Builder().withPort(8001).withInMemoryBackend().build();
        api.start();
        AmazonS3 client = AmazonS3ClientBuilder
                .standard()
                .withPathStyleAccessEnabled(true)
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration("http://localhost:8001", "us-east-1"))
                .withCredentials(new AWSStaticCredentialsProvider(new AnonymousAWSCredentials()))
                .build();
        client.createBucket("testbucket");
        client.putObject("testbucket", "file^name", "contents");
        client.deleteObjects(new DeleteObjectsRequest("testbucket").withKeys("file^name"));
    }

}
