
package org.mitre.fhir.authorization.token;

import java.util.HashMap;
import java.util.Map;

public class TokenManager {

  private static TokenManager instance;

  private final Map<String, Token> tokenMap = new HashMap<>();
  private final Map<String, String> tokenToCorrespondingRefreshToken = new HashMap<>();
  private final Map<String, Token> refreshTokenMap = new HashMap<>();

  private Token serverToken;

  private TokenManager() {

  }

  /**
   * Gets instance of the TokenManager singleton.
   * 
   * @return 
   */
  public static TokenManager getInstance() {
    if (instance == null) {
      instance = new TokenManager();
    }

    return instance;
  }

  /**
   * Creates a token.
   * 
   * @return the created token
   */
  public Token createToken() {
    Token token = new Token();
    tokenMap.put(token.getTokenValue(), token);

    Token refreshToken = new Token();
    tokenToCorrespondingRefreshToken.put(token.getTokenValue(), refreshToken.getTokenValue());
    refreshTokenMap.put(refreshToken.getTokenValue(), refreshToken);

    return token;
  }

  /**
   * Get the refresh token corresponding to a token.
   * 
   * @param tokenValue the relevant token value
   * 
   * @return refresh token corresponding to the provided token.
   * @throws TokenNotFoundException if the provided token value is not found.
   */
  public Token getCorrespondingRefreshToken(String tokenValue) throws TokenNotFoundException {
    // confirm we were passed a valid token value
    if (!tokenMap.containsKey(tokenValue)) {
      throw new TokenNotFoundException(tokenValue);
    }

    String refreshTokenValue = tokenToCorrespondingRefreshToken.get(tokenValue);

    Token refreshToken = refreshTokenMap.get(refreshTokenValue);

    return refreshToken;

  }

  /**
   * revokes a token.
   * 
   * @param tokenValue the token to be revoked.
   * @throws TokenNotFoundException if the provided token value is not found.
   * @throws InactiveTokenException the token has already been revoked.
   */
  public void revokeToken(String tokenValue) throws TokenNotFoundException, InactiveTokenException {
    
    String truncatedToken = tokenValue.split("\\.")[0]; //remove any scopes
    
    Token token = tokenMap.get(truncatedToken);

    if (token != null) {
      if (token.isActive()) {
        token.revokeToken();
        // revoke the refresh token
        Token refreshToken = getCorrespondingRefreshToken(truncatedToken);
        refreshToken.revokeToken();
      } else {
        throw new InactiveTokenException(token);
      }
    } else {
      throw new TokenNotFoundException(tokenValue);
    }
  }

  /**
   * authenticates if the token is active.
   * 
   * @param tokenValue the token to authenticate
   * @return it token is active
   * @throws TokenNotFoundException if the supplied token is not found.
   */
  public boolean authenticateToken(String tokenValue) throws TokenNotFoundException {
    Token token = tokenMap.get(tokenValue);

    if (token != null) {
      return token.isActive();
    }

    throw new TokenNotFoundException(tokenValue);
  }

  /**
   * Authenticates a refresh token.
   * 
   * @param refreshTokenValue the refresh token to be authenticated
   * @return boolean indicating whether the refresh token is active
   * @throws TokenNotFoundException if the provided token is not found
   */
  public boolean authenticateRefreshToken(String refreshTokenValue) throws TokenNotFoundException {
    Token refreshToken = refreshTokenMap.get(refreshTokenValue);

    if (refreshToken != null) {
      return refreshToken.isActive();
    }

    throw new TokenNotFoundException(refreshTokenValue);
  }

  public void clearAllTokens() {
    tokenMap.clear();
    serverToken = null;
  }

  /**
   * Gets a preadded token for calls in the java code. 
   * 
   * @return a Token
   */
  public Token getServerToken() {

    if (serverToken == null) {
      serverToken = createToken();
    }

    return serverToken;
  }

}