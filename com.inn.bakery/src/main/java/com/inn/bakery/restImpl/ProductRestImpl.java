package com.inn.bakery.restImpl;

import com.inn.bakery.constents.BakeryConstants;
import com.inn.bakery.dto.FileResponse;
import com.inn.bakery.rest.ProductRest;
import com.inn.bakery.service.ProductService;
import com.inn.bakery.utils.BakeryUtils;
import com.inn.bakery.wrapper.ProductWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class ProductRestImpl implements ProductRest {

    @Autowired
    ProductService productService;

    private static final String UPLOAD_DIR = "uploads/";

    @Override
    public ResponseEntity<String> addNewProduct(Map<String, String> requestMap) {
        try {
            return productService.addNewProduct(requestMap);
        } catch (Exception ex){
            ex.printStackTrace();
        }
        return BakeryUtils.getResponseEntity(BakeryConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<List<ProductWrapper>> getAllProduct() {
        try {
            return productService.getAllProduct();
        } catch (Exception ex){
            ex.printStackTrace();
        }
        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<String> updateProduct(Map<String, String> requestMap) {
        try {
            return productService.updateProduct(requestMap);
        } catch (Exception ex){
            ex.printStackTrace();
        }
        return BakeryUtils.getResponseEntity(BakeryConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<String> deleteProduct(Integer id) {
        try {
            return productService.deleteProduct(id);
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return BakeryUtils.getResponseEntity(BakeryConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<String> updateStatus(Map<String, String> requestMap) {
        try {
            return productService.updateStatus(requestMap);
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return BakeryUtils.getResponseEntity(BakeryConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<List<ProductWrapper>> getByCategory(Integer id) {
        try {
            return productService.getByCategory(id);
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<ProductWrapper> getProductById(Integer id) {
        try {
            return productService.getProductById(id);
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return new ResponseEntity<>(new ProductWrapper(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<FileResponse> uploadFile(MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(FileResponse.builder().build());
        }

        try {
            // Ensure upload directory exists
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate UUID and create new filename
            String filePrefix = UUID.randomUUID().toString();
            String originalFilename = file.getOriginalFilename();
            String newFilename = filePrefix + "_" + originalFilename;

            // Save file to project path
            Path filePath = uploadPath.resolve(newFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return ResponseEntity.ok(
                    FileResponse.builder()
                            .path("/product/images/" + newFilename) // API path to get image
                            .build()
            );

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(FileResponse.builder().build());
        }
    }

    @Override
    public ResponseEntity<byte[]> getImage(String filename) {
        try {
            Path imagePath = Paths.get(UPLOAD_DIR).resolve(filename);
            if (!Files.exists(imagePath)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            byte[] imageBytes = Files.readAllBytes(imagePath);
            HttpHeaders headers = new HttpHeaders();

            // Determine the content type based on the file extension
            String contentType = Files.probeContentType(imagePath);
            if (contentType == null) {
                contentType = "application/octet-stream"; // Default to binary stream if type is unknown
            }
            headers.setContentType(MediaType.parseMediaType(contentType));

            return ResponseEntity.ok().headers(headers).body(imageBytes);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


}
