package com.example.demo.service;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    // Import from CSV
    public void importFromCSV(MultipartFile file) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withHeader())) {

            List<User> usersToSave = new ArrayList<>();
            StringBuilder duplicateMessages = new StringBuilder();

            for (CSVRecord record : csvParser) {
                String userName = record.get("UserName");
                String emailId = record.get("EmailId");

                // Check if the user with the same userName or emailId already exists
                if (userRepository.existsByUserNameOrEmailId(userName, emailId)) {
                    duplicateMessages.append("User with username: ")
                            .append(userName)
                            .append(" or email: ")
                            .append(emailId)
                            .append(" already exists.\n");
                } else {
                    User user = new User();
                    user.setUserName(userName);
                    user.setEmailId(emailId);
                    usersToSave.add(user);
                }
            }

            // Save only the new users to the database
            if (!usersToSave.isEmpty()) {
                userRepository.saveAll(usersToSave);
            }

            // If there are duplicate entries, return a 207 Multi-Status response
            if (duplicateMessages.length() > 0) {
                throw new ResponseStatusException(HttpStatus.MULTI_STATUS, "Some users were not added:\n" + duplicateMessages.toString());
            }
        }
    }

    // Export to CSV
    public void exportToCSV(OutputStream os) throws Exception {
        List<User> users = userRepository.findAll();

        try (CSVPrinter csvPrinter = new CSVPrinter(new OutputStreamWriter(os), CSVFormat.DEFAULT.withHeader("UserName", "EmailId"))) {
            for (User user : users) {
                csvPrinter.printRecord(user.getUserName(), user.getEmailId());
            }
        }
    }
}
