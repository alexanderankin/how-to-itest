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

    @Test
    void test_readFiles() {
        String contents = "abc def 12345";
        String file = "some-example-file.txt";
        ftpService.write(file, contents);

        String read = ftpService.contents(file);
        assertEquals(contents, read);
    }

}
