package io.findify.s3mock.example;

import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import io.findify.s3mock.S3Mock;

/**
 * Created by shutty on 8/12/16.
 */
public class JavaExample {
    public static void main(String[] args) {
        S3Mock api = S3Mock.create(8001, "/tmp/s3");
        api.start();

        AmazonS3Client client = new AmazonS3Client(new AnonymousAWSCredentials());
        client.setEndpoint("http://127.0.0.1:8001");
        client.createBucket("testbucket");
        client.putObject("testbucket", "file/name", "contents");
    }
}
