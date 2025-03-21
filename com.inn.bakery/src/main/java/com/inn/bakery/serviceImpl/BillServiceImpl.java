package com.inn.bakery.serviceImpl;

import com.inn.bakery.JWT.JwtFilter;
import com.inn.bakery.POJO.Bill;
import com.inn.bakery.constents.BakeryConstants;
import com.inn.bakery.dao.BillDao;
import com.inn.bakery.service.BillService;
import com.inn.bakery.utils.BakeryUtils;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.io.IOUtils;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
@Service
public class BillServiceImpl implements BillService {

    @Autowired
    JwtFilter jwtFilter;

    @Autowired
    BillDao billDao;

    @Override
    public ResponseEntity<String> generateReport(Map<String, Object> requestMap) {
        log.info("Inside generateReport");
        try {
            String fileName;
            if (validateRequestMap(requestMap)){
                if (requestMap.containsKey("isGenerate") && !(Boolean) requestMap.get("isGenerate")) {
                    fileName = (String) requestMap.get("uuid");
                } else {
                    fileName = BakeryUtils.getUUID();
                    requestMap.put("uuid", fileName);
                    insertBill(requestMap);
                }

                String data = "Name: " + requestMap.get("name") + "\n" + "Contact Number: " + requestMap.get("contactNumber") +
                        "\n" + "Email: " + requestMap.get("email") + "\n" + "Payment Method: " + requestMap.get("paymentMethod") + "\n";

                Document document = new Document();
                PdfWriter.getInstance(document, new FileOutputStream(BakeryConstants.STORE_LOCATION + "\\" + fileName + ".pdf"));

                document.open();
                setRectangleInPdf(document);

                Paragraph chunk = new Paragraph("Bakery Management System", getFont("Header"));
                chunk.setAlignment(Element.ALIGN_CENTER);
                document.add(chunk);

                Paragraph paragraph = new Paragraph(data+ "\n \n", getFont("Data"));
                document.add(paragraph);

                PdfPTable table = new PdfPTable(5);
                table.setWidthPercentage(100);
                addTableHeader(table);

                JSONArray jsonArray = BakeryUtils.getJSONArrayFromString((String) requestMap.get("productDetails"));
                for (int i = 0; i < jsonArray.length(); i++) {
                    addRows(table, BakeryUtils.getMapFromJson(jsonArray.getString(i)));
                }
                document.add(table);

                Paragraph footer = new Paragraph("Total : " + requestMap.get("totalAmount") + "\n"
                        + "Thank you for visiting. Please visit again!", getFont("Data"));
                document.add(footer);
                document.close();
                return new ResponseEntity<>("{\"uuid\":\"" + fileName + "\"}", HttpStatus.OK);

            }
            return BakeryUtils.getResponseEntity("Required data not found", HttpStatus.BAD_REQUEST);
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return BakeryUtils.getResponseEntity(BakeryConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
    }


    private void addRows(PdfPTable table, Map<String, Object> data) {
        log.info("Inside addRows");
        table.addCell((String) data.get("name"));
        table.addCell((String) data.get("category"));

        // Check the type of quantity and cast accordingly
        Object quantityObj = data.get("quantity");
        if (quantityObj instanceof String) {
            table.addCell((String) quantityObj);
        } else if (quantityObj instanceof Double) {
            // Convert Double to Integer and then to String
            int quantityInt = ((Double) quantityObj).intValue();
            table.addCell(Integer.toString(quantityInt));
        } else {
            table.addCell(""); // Handle the case where quantity is neither String nor Double
        }

        table.addCell(Double.toString((Double) data.get("price")));
        table.addCell(Double.toString((Double) data.get("total")));
    }

    private void addTableHeader(PdfPTable table) {
        log.info("Inside addTableHeader");
        Stream.of("Name", "Category", "Quantity", "Price", "Sub Total")
                .forEach(columnTitle -> {
                    PdfPCell header = new PdfPCell();
                    header.setBackgroundColor(BaseColor.LIGHT_GRAY);
                    header.setBorderWidth(2);
                    header.setPhrase(new Phrase(columnTitle));
                    header.setBackgroundColor(BaseColor.CYAN);
                    header.setHorizontalAlignment(Element.ALIGN_CENTER);
                    header.setVerticalAlignment(Element.ALIGN_CENTER);
                    table.addCell(header);
                });
    }

    private Font getFont(String type){
        log.info("Inside getFont");
        switch (type){
            case "Header":
                Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLDOBLIQUE, 18, BaseColor.BLACK);
                headerFont.setStyle(Font.BOLD);
                return headerFont;
            case "Data":
                Font dataFont = FontFactory.getFont(FontFactory.TIMES_ROMAN, 11, BaseColor.BLACK);
                dataFont.setStyle(Font.BOLD);
                return dataFont;
            default:
                return new Font();
        }
    }

    private void setRectangleInPdf(Document document) throws DocumentException {
        log.info("Inside setRectangleInPdf");
        Rectangle rect = new Rectangle(577, 825, 18,15);
        rect.enableBorderSide(1);
        rect.enableBorderSide(2);
        rect.enableBorderSide(4);
        rect.enableBorderSide(8);
        rect.setBorderColor(BaseColor.BLACK);
        rect.setBorderWidth(1);
        document.add(rect);
    }

    private void insertBill(Map<String, Object> requestMap) {
        try {
            Bill bill = new Bill();
            bill.setUuid((String) requestMap.get("uuid"));
            bill.setName((String)requestMap.get("name"));
            bill.setEmail((String)requestMap.get("email"));
            bill.setContactNumber((String)requestMap.get("contactNumber"));
            bill.setPaymentMethod((String)requestMap.get("paymentMethod"));
            bill.setTotal(Integer.parseInt((String) requestMap.get("totalAmount")));
            bill.setProductDetail((String)requestMap.get("productDetails"));
            bill.setCreatedBy(jwtFilter.getCurrentUser());
            billDao.save(bill);
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    private boolean validateRequestMap(Map<String, Object> requestMap) {
        return requestMap.containsKey("name") &&
                requestMap.containsKey("contactNumber")&&
                requestMap.containsKey("email") &&
                requestMap.containsKey("paymentMethod") &&
                requestMap.containsKey("productDetails") &&
                requestMap.containsKey("totalAmount");
    }

    @Override
    public ResponseEntity<List<Bill>> getBills() {
        List<Bill> list = new ArrayList<>();
        if (jwtFilter.isAdmin()){
            list = billDao.getAllBills();
        } else {
            list = billDao.getBillByUserName(jwtFilter.getCurrentUser());
        }
        return new ResponseEntity<>(list, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<byte[]> getPdf(Map<String, Object> requestMap) {
        log.info("Inside getPdf : requestMap {} " , requestMap);
        try {
            byte[] byteArray = new byte[0];
            if(!requestMap.containsKey("uuid") && validateRequestMap(requestMap))
                return new ResponseEntity<>(byteArray, HttpStatus.BAD_REQUEST);
            String filePath = BakeryConstants.STORE_LOCATION+ "\\" + (String)requestMap.get("uuid") + ".pdf";
            if (BakeryUtils.isFileExist(filePath)) {
                byteArray = getByteArray(filePath);
                return new ResponseEntity<>(byteArray, HttpStatus.OK);
            } else {
                requestMap.put("isGenerate", false);
                generateReport(requestMap);
                byteArray = getByteArray(filePath);
                return new ResponseEntity<>(byteArray, HttpStatus.OK);
            }


        }catch (Exception ex){
            ex.printStackTrace();
        }
        return null;
    }


    private byte[] getByteArray(String filePath) throws IOException {
        File initialFile = new File(filePath);
        InputStream targetStream = new FileInputStream(initialFile);
        byte[] byteArray = IOUtils.toByteArray(targetStream);
        targetStream.close();
        return byteArray;
    }

    @Override
    public ResponseEntity<String> deleteBill(Integer id) {
        try {
            Optional optional = billDao.findById(id);
            if (!optional.isEmpty()){
                billDao.deleteById(id);
                return BakeryUtils.getResponseEntity("Bill Deleted Successfully", HttpStatus.OK);
            }
            return BakeryUtils.getResponseEntity("Bill id does not exist",HttpStatus.OK);
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return BakeryUtils.getResponseEntity(BakeryConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
    }


}
