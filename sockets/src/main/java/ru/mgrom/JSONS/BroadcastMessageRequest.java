package ru.mgrom.JSONS;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = false)
public class BroadcastMessageRequest extends AbstractRequest {

  public static final String TYPE = "broadcastSendMessage";

  private String message;

  {
    setType(TYPE);
  }

}
