package com.awsiaasproject.imagerecognization.webapp.controller;

import com.awsiaasproject.imagerecognization.webapp.constant.ProjectConstant;
import com.awsiaasproject.imagerecognization.webapp.service.AWSUtil;
import com.awsiaasproject.imagerecognization.webapp.service.BusinessLogic;

import java.util.HashMap;
import java.util.TreeMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/*
 * All the APIs are defined here that provide necessary services to run the application. 
 * This is backbone of the application.
 */
@Controller
public class WebAppAPIController {
	
    @Autowired
    private BusinessLogic callBackendService;

    @Autowired
    private AWSUtil awsService;
    
    /*
     * GetMapping : Get the request from the user.
     */
	@GetMapping("/home")
	public String greeting(@RequestParam(name = "name", required = false, defaultValue = "World") String name,
			Model model) {
		return "imagerecognization";
	}
	
	/*
	 * PostMapping: post the request to the user.
	 */
	@PostMapping("/imagerecognization")
	public String  getImageUrl(@RequestParam(name = "imageurl", required = true) MultipartFile[] files, Map<String, Object> model){
		awsService.createQueue(ProjectConstant.OUTPUT_QUEUE);
		Set<String> imageSet = new HashSet<>(); 
		Map<String,String> classificationResult = new TreeMap<>();
		for(MultipartFile a : files) {
			String uniqueFileName = callBackendService.generateFileName(a);
			awsService.uploadFileToS3(a,uniqueFileName);
			awsService.queueInputRequest(uniqueFileName, ProjectConstant.INPUT_QUEUE, 0);
			imageSet.add(uniqueFileName);
		}
		for(String imageFileName : imageSet) {
			String[] result = callBackendService.getImageClassificationResult(imageFileName);
			classificationResult.put(result[0],result[1]);
		}
		model.put("classificationResult",classificationResult);
		return "result";
	}
}
