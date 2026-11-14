package com.allbutyou.room.service;

import com.allbutyou.room.domain.Room;
import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class RoomCodeGenerator {

    private static final char[] ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        char[] code = new char[Room.CODE_LENGTH];
        for (int i = 0; i < code.length; i++) {
            code[i] = ALPHABET[random.nextInt(ALPHABET.length)];
        }
        return new String(code);
    }
}
