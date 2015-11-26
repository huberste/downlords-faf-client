package com.faforever.client.legacy.relay;

public class JoinGameMessage extends GpgServerMessage {

  private static final int PEER_ADDRESS_INDEX = 0;
  private static final int USERNAME_INDEX = 1;
  private static final int PEER_UID_INDEX = 2;

  public JoinGameMessage() {
    super(GpgServerCommandServerCommand.JOIN_GAME);
  }

  public String getPeerAddress() {
    return getString(PEER_ADDRESS_INDEX);
  }

  public void setPeerAddress(String peerAddress) {
    setValue(PEER_ADDRESS_INDEX, peerAddress);
  }

  public int getPeerUid() {
    return getInt(PEER_UID_INDEX);
  }

  public void setPeerUid(int peerUid) {
    setValue(PEER_UID_INDEX, peerUid);
  }

  public void setUsername(String username) {
    setValue(USERNAME_INDEX, username);
  }
}
