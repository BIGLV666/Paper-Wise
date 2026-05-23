package org.example.paperwise.Until;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public  class PasswordEncoder {
    private static final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    public static String encode(CharSequence rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
    public static boolean matches(CharSequence rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}
