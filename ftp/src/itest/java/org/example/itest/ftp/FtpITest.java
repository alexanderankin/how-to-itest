package org.example.itest.ftp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

class FtpITest extends BaseITest {

    @Autowired
    Ftp.Service ftpService;

    @Test
    void test() {
        System.out.println("context loaded!");
    }

}
