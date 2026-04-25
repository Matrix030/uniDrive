package edu.nyu.unidrive.client.net;

import edu.nyu.unidrive.common.dto.LoginResponse;
import java.io.IOException;

public interface AuthApiClient {
    LoginResponse login(String userId, String role) throws IOException;
}
