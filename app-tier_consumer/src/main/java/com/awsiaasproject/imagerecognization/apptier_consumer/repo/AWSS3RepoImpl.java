package com.awsiaasproject.imagerecognization.apptier_consumer.repo;

import java.io.ByteArrayInputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.awsiaasproject.imagerecognization.apptier_consumer.config.BasicAWSConfigurations;
import com.awsiaasproject.imagerecognization.apptier_consumer.constant.ProjectConstant;

@Repository
public class AWSS3RepoImpl implements AWSS3Repo {

	@Autowired
	private BasicAWSConfigurations awsConfigurations;

	@Override
	public void uploadFile(String key, String value) {
		try {
			if (!awsConfigurations.getS3().doesBucketExistV2(ProjectConstant.OUTPUT_BUCKET))
				awsConfigurations.getS3().createBucket(ProjectConstant.OUTPUT_BUCKET);

			byte[] contentAsBytes = null;
			try {
				contentAsBytes = value.getBytes("UTF-8");
			} catch (Exception e) {
				e.printStackTrace();
			}
			ByteArrayInputStream contentsAsStream = new ByteArrayInputStream(contentAsBytes);
			ObjectMetadata omd = new ObjectMetadata();
			omd.setContentLength(contentAsBytes.length);
			awsConfigurations.getS3()
					.putObject(new PutObjectRequest(ProjectConstant.OUTPUT_BUCKET, key, contentsAsStream, omd));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
