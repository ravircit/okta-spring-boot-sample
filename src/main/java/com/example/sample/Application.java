package com.example.sample;

import com.example.sample.s3.services.impl.S3ServicesImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import netscape.javascript.JSObject;
import org.apache.logging.log4j.CloseableThreadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    /**
     * This example controller has endpoints for displaying the user profile info on {code}/{code} and "you have been
     * logged out page" on {code}/post-logout{code}.
     */
    @RestController
    static class ExampleController {
        public static Map<String, String> cognitoLogin(OAuth2AuthenticationToken oauthToken) throws URISyntaxException {

            RestTemplate restTemplate = new RestTemplate();
            URI uri = new URI("https://dmllexl6n0.execute-api.us-east-1.amazonaws.com/login");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.valueOf(MediaType.APPLICATION_JSON_VALUE));
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            Map<String, String> details = new HashMap<>();
            details.put("username", oauthToken.getPrincipal().getAttributes().get("email").toString());
            details.put("password", "formkiq12");

            HttpEntity<?> entity = new HttpEntity<Object>(details, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(uri, entity, String.class);

            String responseBody = response.getBody();

            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, String> resultMap = new HashMap<>();
            try {
                resultMap = (Map<String, String>) objectMapper.readValue(responseBody, Map.class).get("AuthenticationResult");
                for (String mapId : oauthToken.getPrincipal().getAttributes().keySet()) {
                    if (oauthToken.getPrincipal().getAttributes().get(mapId) instanceof String && !mapId.equals("ExpiresIn")) {
                        resultMap.put(mapId, (String) oauthToken.getPrincipal().getAttributes().get(mapId).toString());
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            resultMap.remove("ExpiresIn");
            return resultMap;
        }

        @GetMapping("/profile")
        @PreAuthorize("hasAuthority('SCOPE_profile')")
        Map<String,String> userDetails(OAuth2AuthenticationToken authentication) {
            Map<String, String> idTokenMap = new HashMap<>();
            try {
                idTokenMap = cognitoLogin(authentication);

            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
            Collection<GrantedAuthority> authorities = authentication.getAuthorities();
            //return new ModelAndView("userProfile", Collections.singletonMap("details", authentication.getPrincipal().getAttributes()));
            return idTokenMap;
        }


        @Autowired
        private S3ServicesImpl s3Services;
        @GetMapping("/")
        String home() {
            return "home";
        }



        @GetMapping("/all")
        void get() {
            s3Services.getAll();
        }
        @GetMapping("/hello")
        String sayHello(@AuthenticationPrincipal Jwt jwt) {
            return String.format("Hello, %s!");
        }
    }

    @RestController
    static class ExampleRestController {

        @GetMapping("/hello1")
        String sayHello(@AuthenticationPrincipal Jwt jwt) {
            return String.format("Hello, %s!", jwt.getSubject());
        }
    }

    @RestController
    @RequestMapping("/uploadFile")
    static class UploadFileController {
        private Logger logger = LoggerFactory.getLogger(com.example.sample.s3.controller.UploadFileController.class);
        @Autowired
        private S3ServicesImpl s3Services;

        @RequestMapping(value = "", method = RequestMethod.POST)
        public ResponseEntity<?> uploadFile(@RequestPart(value = "files") MultipartFile[] srcFiles) throws Exception {
            MultipartFile file = null;
            Map<String, String> map = new HashMap<>();
            for (int i = 0; i < srcFiles.length; ++i) {
                file = srcFiles[i];
                if (!file.isEmpty()) {
                    s3Services.uploadFile(file);
                    map.put("result", "upload success!");
                } else {
                    map.put("result", "the file is empty!");
                }
            }
            return ResponseEntity.ok(map);
        }
    }
}
class User
{
    public String email;
    public String password;

    public User(String email, String password) {
        this.email = email;
        this.password = password;
    }
}