package com.inn.bakery.serviceImpl;

import com.google.common.base.Strings;
import com.inn.bakery.JWT.CustomerUsersDetailsService;
import com.inn.bakery.JWT.JwtFilter;
import com.inn.bakery.JWT.JwtUtil;
import com.inn.bakery.POJO.User;
import com.inn.bakery.constents.BakeryConstants;
import com.inn.bakery.dao.UserDao;
import com.inn.bakery.service.UserService;
import com.inn.bakery.utils.BakeryUtils;
import com.inn.bakery.utils.EmailUtils;
import com.inn.bakery.wrapper.UserWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    UserDao userDao;

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    CustomerUsersDetailsService customerUsersDetailsService;

    @Autowired
    JwtUtil jwtUtil;

    @Autowired
    JwtFilter jwtFilter;

    @Autowired
    EmailUtils emailUtils;

    @Override
    public ResponseEntity<String> signUp(Map<String, String> requestMap) {
        log.info("Inside signup {}", requestMap);
        try {
            if (validateSignUp(requestMap)) {
                User user = userDao.findByEmailId(requestMap.get("email"));
                if (Objects.isNull(user)) {
                    userDao.save(getUserFromMap(requestMap));
                    return BakeryUtils.getResponseEntity("Successfully registered", HttpStatus.OK);
                } else {
                    return BakeryUtils.getResponseEntity("Email already exist.", HttpStatus.BAD_REQUEST);
                }
            } else {
                return BakeryUtils.getResponseEntity(BakeryConstants.INVALID_DATA, HttpStatus.BAD_REQUEST);
            }
        }catch (Exception ex) {
            ex.printStackTrace();
        }
        return BakeryUtils.getResponseEntity(BakeryConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private boolean validateSignUp(Map<String, String> requestMap) {

        if(requestMap.containsKey("name") && requestMap.containsKey("contactNumber")
                && requestMap.containsKey("email") && requestMap.containsKey("password")) {
            return true;
        }
        return false;
    }


    private User getUserFromMap(Map<String, String> requestMap) {
        User user = new User();
        user.setName(requestMap.get("name"));
        user.setContactNumber(requestMap.get("contactNumber"));
        user.setEmail(requestMap.get("email"));
        user.setPassword(requestMap.get("password"));
        user.setStatus("false");
        user.setRole("user");
        return user;

    }

    @Override
    public ResponseEntity<String> login(Map<String, String> requestMap) {
        log.info("Inside login");
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(requestMap.get("email"), requestMap.get("password"))
            );
            if (auth.isAuthenticated()) {
                var userDetails = customerUsersDetailsService.getUserDetail();
                if ("true".equalsIgnoreCase(userDetails.getStatus())) {
                    String token = jwtUtil.generateToken(userDetails.getEmail(), userDetails.getRole());
                    String responseJson = String.format("{\"token\":\"%s\", \"email\":\"%s\", \"role\":\"%s\"}",
                            token, userDetails.getEmail(), userDetails.getRole());
                    return new ResponseEntity<>(responseJson, HttpStatus.OK);
                } else {
                    return new ResponseEntity<>("{\"message\":\"Wait for admin approval.\"}", HttpStatus.BAD_REQUEST);
                }
            }
        } catch (Exception ex) {
            log.error("{}", ex);
        }
        return new ResponseEntity<>("{\"message\":\"Bad Credentials.\"}", HttpStatus.BAD_REQUEST);
    }


    @Override
    public ResponseEntity<List<UserWrapper>> getAllUser() {
        try {
            if (jwtFilter.isAdmin()){
                return new ResponseEntity<>(userDao.getAllUser(), HttpStatus.OK);
            }else{
                return new ResponseEntity<>(new ArrayList<>(), HttpStatus.UNAUTHORIZED);
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<String> update(Map<String, String> requestMap) {
        try {
            if (jwtFilter.isAdmin()){
                Optional<User> optional = userDao.findById(Integer.parseInt(requestMap.get("id")));
                if(!optional.isEmpty()) {
                    userDao.updateStatus(requestMap.get("status"), Integer.parseInt(requestMap.get("id")));
                    sendMailToAllAdmin(requestMap.get("status"), optional.get().getEmail(), userDao.getAllAdmin());
                    return BakeryUtils.getResponseEntity("User Status Updated Successfully.", HttpStatus.OK);
                } else {
                    return BakeryUtils.getResponseEntity("User id does not exist.", HttpStatus.OK);
                }
            } else {
                return BakeryUtils.getResponseEntity(BakeryConstants.UNAUTHORIZED_ACCESS, HttpStatus.UNAUTHORIZED);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return BakeryUtils.getResponseEntity(BakeryConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
    }


    private void sendMailToAllAdmin(String status, String user, List<String> allAdmin) {
        allAdmin.remove(jwtFilter.getCurrentUser());
        if (status  != null && status.equalsIgnoreCase("true")) {
            emailUtils.sendSimpleMailMessage(jwtFilter.getCurrentUser(), "Account Approved", "USER:- " + user + "\n is approved by \nADMIN:- " + jwtFilter.getCurrentUser(), allAdmin);
        } else {
            emailUtils.sendSimpleMailMessage(jwtFilter.getCurrentUser(), "Account Disabled", "USER:- " + user + "\n is disabled by \nADMIN:- " + jwtFilter.getCurrentUser(), allAdmin);
        }
    }

    @Override
    public ResponseEntity<String> checkToken() {
        return BakeryUtils.getResponseEntity("true", HttpStatus.OK);
    }

    @Override
    public ResponseEntity<String> changePassword(Map<String, String> requestMap) {
        try {
            User userObj = userDao.findByEmail(jwtFilter.getCurrentUser());
            if (!userObj.equals(null)){
                if(userObj.getPassword().equals(requestMap.get("oldPassword"))){
                    userObj.setPassword(requestMap.get("newPassword"));
                    userDao.save(userObj);
                    return BakeryUtils.getResponseEntity("Password Updated Successfully.", HttpStatus.OK);
                }
                return BakeryUtils.getResponseEntity("Incorrect Old Password", HttpStatus.BAD_REQUEST);
            }
            return BakeryUtils.getResponseEntity(BakeryConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception ex){
            ex.printStackTrace();
        }
        return BakeryUtils.getResponseEntity(BakeryConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<String> forgotPassword(Map<String, String> requestMap) {
        try {
            User user = userDao.findByEmail(requestMap.get("email"));
            if (!Objects.isNull(user) && !Strings.isNullOrEmpty(user.getEmail())){
                emailUtils.forgotMail(user.getEmail(), "Credentials by Bakery Management System", user.getPassword());
                return BakeryUtils.getResponseEntity("Check your mail for credentials.", HttpStatus.OK);
            }
        } catch (Exception ex){
            ex.printStackTrace();
        }
        return BakeryUtils.getResponseEntity(BakeryConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
    }


}
