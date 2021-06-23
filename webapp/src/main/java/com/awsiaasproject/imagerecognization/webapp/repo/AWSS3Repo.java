package com.awsiaasproject.imagerecognization.webapp.repo;

import java.io.File;

public interface AWSS3Repo {

    public void uploadFile(String fileName, File file);

}
