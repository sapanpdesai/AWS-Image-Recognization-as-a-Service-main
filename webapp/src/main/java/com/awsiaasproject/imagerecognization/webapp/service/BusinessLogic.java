package com.awsiaasproject.imagerecognization.webapp.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/*
 * This interface is project specific might or might not be implemented by other class, it is based on requirements.
 */
public interface BusinessLogic {
	public Map<String, String> outputMap = new HashMap<>();
	
    public String[] getImageClassificationResult(String imageURL);

    public File convertMultiPartToFile(MultipartFile file);

    public String generateFileName(MultipartFile multiPart);
    
    public void putOutputFromResponseQueueToHashMap();
    
    public String formatImageUrl(String imageUrl);
}
