package com.awsiaasproject.imagerecognization.webapp.repo;

import com.amazonaws.services.s3.model.PutObjectRequest;
import com.awsiaasproject.imagerecognization.webapp.config.BasicAWSConfigurations;
import com.awsiaasproject.imagerecognization.webapp.constant.ProjectConstant;

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class AWSS3RepoImpl implements AWSS3Repo {

    @Autowired
    private BasicAWSConfigurations awsConfigurations;

    @Override
    public void uploadFile(String fileName, File file) {
        try {
            if (!awsConfigurations.getS3().doesBucketExistV2(ProjectConstant.INPUT_BUCKET))
                awsConfigurations.getS3().createBucket(ProjectConstant.INPUT_BUCKET);
            awsConfigurations.getS3().putObject(new PutObjectRequest(ProjectConstant.INPUT_BUCKET, fileName, file));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
