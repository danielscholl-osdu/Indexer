package org.opengroup.osdu.util;

import lombok.ToString;
import lombok.extern.java.Log;

import java.io.IOException;

@Log
@ToString
public class GCPHTTPClient extends HTTPClient {

    private static String token = null;

    @Override
    public synchronized String getAccessToken() {
        if(token == null) {
            try {
                token = "Bearer " + JwtTokenUtil.getAccessToken();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return token;
    }
}